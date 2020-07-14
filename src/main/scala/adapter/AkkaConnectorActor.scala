package adapter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.{Date, UUID}

import adapter.obpApiModel.{CounterpartyAccountReference, CustomerAccountReference, HistoricalTransactionJson, ObpApi}
import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import com.openbankproject.commons.dto._
import com.openbankproject.commons.model._
import com.openbankproject.commons.model.enums.TransactionRequestStatus
import io.circe.generic.auto._
import io.circe.syntax._
import model.enums.sepaReasonCodes.PaymentReturnReasonCode.PaymentReturnReasonCode
import model.enums.sepaReasonCodes.{PaymentRecallReasonCode, PaymentReturnReasonCode}
import model.enums.{SepaCreditTransferTransactionStatus, SepaMessageStatus, SepaMessageType}
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import sepa.{PaymentReturnMessage, SepaUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class AkkaConnectorActor extends Actor with ActorLogging {

  val cluster: Cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.join(self.path.address)

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive: Receive = {
    case OutBoundGetAdapterInfo(callContext) =>
      val result = InBoundGetAdapterInfo(
        inboundAdapterCallContext = InboundAdapterCallContext(
          callContext.correlationId,
          callContext.sessionId,
          callContext.generalContext
        ),
        status = successInBoundStatus,
        data = InboundAdapterInfoInternal("", Nil, "SEPA-Adapter", "Jun2020", APIUtil.gitCommit, new Date().toString)
      )
      sender ! result

    case OutBoundMakePaymentv210(callContext, fromAccount, toAccount, transactionRequestId, transactionRequestCommonBody, amount, description, transactionRequestType, chargePolicy) =>
      println("Make payment message received")

      val obpAkkaConnector = sender

      val jsonDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

      transactionRequestType.value match {
        case "SEPA" =>
          val creditTransferTransactionId = UUID.randomUUID()

          val creditTransferTransaction = SepaCreditTransferTransaction(
            id = creditTransferTransactionId,
            amount = amount,
            debtorName = Some(fromAccount.accountHolder),
            debtorAccount = fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)),
            debtorAgent = Some(Bic(fromAccount.bankId.value)),
            creditorName = Some(toAccount.accountHolder),
            creditorAccount = toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)),
            creditorAgent = Some(Bic(toAccount.bankId.value)),
            purposeCode = None,
            description = Some(description),
            creationDateTime = LocalDateTime.now(),
            transactionIdInSepaFile = SepaUtil.removeDashesToUUID(creditTransferTransactionId),
            instructionId = None,
            endToEndId = SepaUtil.removeDashesToUUID(creditTransferTransactionId),
            status = SepaCreditTransferTransactionStatus.UNPROCESSED,
            customFields = None
          )

          val historicalTransactionJson = HistoricalTransactionJson(
            from = CustomerAccountReference(
              account_iban = creditTransferTransaction.debtorAccount.map(_.iban).getOrElse(""),
              bank_bic = creditTransferTransaction.debtorAgent.map(_.bic)
            ).asJson,
            to = CounterpartyAccountReference(
              counterparty_iban = creditTransferTransaction.creditorAccount.map(_.iban).getOrElse(""),
              bank_bic = creditTransferTransaction.creditorAgent.map(_.bic),
              counterparty_name = creditTransferTransaction.creditorName
            ).asJson,
            value = AmountOfMoney(
              currency = transactionRequestCommonBody.value.currency,
              amount = creditTransferTransaction.amount.toString
            ).asJson,
            description = creditTransferTransaction.description.getOrElse(""),
            posted = creditTransferTransaction.creationDateTime.format(jsonDateTimeFormatter),
            completed = creditTransferTransaction.creationDateTime.format(jsonDateTimeFormatter),
            `type` = transactionRequestType.value,
            charge_policy = chargePolicy
          )

          ObpApi.saveHistoricalTransaction(historicalTransactionJson).map { createdObpTransactionId =>
            creditTransferTransaction.insert()
            val unprocessedCreditTransferMessage = SepaMessage.getUnprocessedByType(SepaMessageType.B2B_CREDIT_TRANSFER)
              .map(_.headOption.getOrElse {
                val sepaMessageId = UUID.randomUUID()
                val message = SepaMessage(
                  sepaMessageId, LocalDateTime.now(), SepaMessageType.B2B_CREDIT_TRANSFER,
                  SepaMessageStatus.UNPROCESSED, sepaFileId = None, SepaUtil.removeDashesToUUID(sepaMessageId),
                  numberOfTransactions = 0, totalAmount = 0, None, None, None, None
                )
                message.insert()
                message
              })
            unprocessedCreditTransferMessage.onComplete {
              case Success(message) => creditTransferTransaction.linkMessage(
                sepaMessageId = message.id,
                transactionStatusIdInSepaFile = creditTransferTransaction.transactionIdInSepaFile,
                obpTransactionRequestId = Some(UUID.fromString(transactionRequestId.value)),
                obpTransactionId = Some(createdObpTransactionId)
              )
                message.copy(
                  numberOfTransactions = message.numberOfTransactions + 1,
                  totalAmount = message.totalAmount + creditTransferTransaction.amount
                ).update()
              case Failure(exception) => log.error(exception.getMessage)
            }

            val result = InBoundMakePaymentv210(
              inboundAdapterCallContext = InboundAdapterCallContext(
                callContext.correlationId,
                callContext.sessionId,
                callContext.generalContext
              ),
              status = successInBoundStatus,
              data = TransactionId(createdObpTransactionId.toString)
            )
            obpAkkaConnector ! result
          }

        case "REFUND" =>
          val originalObpTransactionId = TransactionId(description.split(" - ").reverse(1).split(Array('(', ')'))(1))

          val originalSepaCreditTransferTransaction = SepaCreditTransferTransaction.getByObpTransactionId(originalObpTransactionId)

          originalSepaCreditTransferTransaction.map {
            case Some(originalTransaction) =>

              val transactionRequestInvalid = (for {
                fromAccountIban <- fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address))
                toAccountIban <- toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address))
                originalTransactionDebtorIban <- originalTransaction.debtorAccount
                originalTransactionCreditorIban <- originalTransaction.creditorAccount
              } yield fromAccountIban == originalTransactionCreditorIban && toAccountIban == originalTransactionDebtorIban).getOrElse(false)

              if (transactionRequestInvalid) {
                val fromAccountIban = fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)).getOrElse(Iban(""))
                val toAccountIban = toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)).getOrElse(Iban(""))
                val accountFrom = ObpApi.getAccountIdByIban(Adapter.BANK_ID, Adapter.VIEW_ID, fromAccountIban)
                val accountTo = ObpApi.getAccountIdByIban(Adapter.BANK_ID, Adapter.VIEW_ID, toAccountIban)

                accountFrom.map( _ => {
                  val reasonCodeString = description.split(" - ").last.split(":").last.trim
                  val refundReasonCode: PaymentReturnReasonCode = reasonCodeString match {
                    case reasonCode if PaymentRecallReasonCode.values.map(_.toString).contains(reasonCode) => PaymentReturnReasonCode.FOLLOWING_CANCELLATION_REQUEST
                    case _ => PaymentReturnReasonCode.withName(reasonCodeString)
                  }
                  val historicalTransactionJson = HistoricalTransactionJson(
                    from = CustomerAccountReference(
                      account_iban = originalTransaction.creditorAccount.map(_.iban).getOrElse(""),
                      bank_bic = originalTransaction.creditorAgent.map(_.bic)
                    ).asJson,
                    to = CounterpartyAccountReference(
                      counterparty_iban = originalTransaction.debtorAccount.map(_.iban).getOrElse(""),
                      bank_bic = originalTransaction.debtorAgent.map(_.bic),
                      counterparty_name = originalTransaction.debtorName
                    ).asJson,
                    value = AmountOfMoney(
                      currency = "EUR",
                      amount = originalTransaction.amount.toString
                    ).asJson,
                    description = description,
                    posted = originalTransaction.creationDateTime.format(jsonDateTimeFormatter),
                    completed = originalTransaction.creationDateTime.format(jsonDateTimeFormatter),
                    `type` = transactionRequestType.value,
                    charge_policy = chargePolicy
                  )
                  for {
                    createdObpTransactionId <- ObpApi.saveHistoricalTransaction(historicalTransactionJson)
                    _ <- PaymentReturnMessage.returnTransaction(
                      originalTransaction, originalTransaction.creditorName.orElse(originalTransaction.creditorAgent.map(_.bic)).getOrElse(""),
                      refundReasonCode, Some(UUID.fromString(transactionRequestId.value)), Some(createdObpTransactionId))
                  } yield {
                    val result = InBoundMakePaymentv210(
                      inboundAdapterCallContext = InboundAdapterCallContext(
                        callContext.correlationId,
                        callContext.sessionId,
                        callContext.generalContext
                      ),
                      status = successInBoundStatus,
                      data = TransactionId(createdObpTransactionId.toString)
                    )
                    obpAkkaConnector ! result
                  }
                }).fallbackTo(accountTo.map( _ => {
                  val historicalTransactionJson = HistoricalTransactionJson(
                    from = CounterpartyAccountReference(
                      counterparty_iban = originalTransaction.creditorAccount.map(_.iban).getOrElse(""),
                      bank_bic = originalTransaction.creditorAgent.map(_.bic),
                      counterparty_name = originalTransaction.creditorName
                    ).asJson,
                    to = CustomerAccountReference(
                      account_iban = originalTransaction.debtorAccount.map(_.iban).getOrElse(""),
                      bank_bic = originalTransaction.debtorAgent.map(_.bic)
                    ).asJson,
                    value = AmountOfMoney(
                      currency = "EUR",
                      amount = originalTransaction.amount.toString
                    ).asJson,
                    description = description,
                    posted = originalTransaction.creationDateTime.format(jsonDateTimeFormatter),
                    completed = originalTransaction.creationDateTime.format(jsonDateTimeFormatter),
                    `type` = transactionRequestType.value,
                    charge_policy = chargePolicy
                  )
                  for {
                    createdObpTransactionId <- ObpApi.saveHistoricalTransaction(historicalTransactionJson)
                    // TODO : Link the obpTransactionRequestId and the obpCreatedTransactionId with the paymentReturn message
                  } yield {
                    val result = InBoundMakePaymentv210(
                      inboundAdapterCallContext = InboundAdapterCallContext(
                        callContext.correlationId,
                        callContext.sessionId,
                        callContext.generalContext
                      ),
                      status = successInBoundStatus,
                      data = TransactionId(createdObpTransactionId.toString)
                    )
                    obpAkkaConnector ! result
                  }
                }))
              } else log.error(s"Transaction request invalid, counterparty_iban don't match with the original SEPA transaction ${originalTransaction.id} (OBP original transaction : ${originalObpTransactionId.value}). Maybe the errror is coming from the to/from field name")

            case None => log.error(s"Original credit transfer transaction with obpTransactionId (${originalObpTransactionId.value}) not found in SEPA Adapter")
          }
      }

    case OutBoundNotifyTransactionRequest(callContext, fromAccount, toAccount, transactionRequest) =>
      println("Notify transaction request received")
      println(transactionRequest)

      val obpAkkaConnector = sender

      transactionRequest.`type` match {
        case "REFUND" =>
          val originalObpTransactionId = TransactionId(transactionRequest.body.description.split(" - ").reverse(1).split(Array('(', ')'))(1))
          val reasonCodeString = transactionRequest.body.description.split(" - ").last.split(":").last.trim
          val refundReasonCode: PaymentReturnReasonCode = reasonCodeString match {
            case reasonCode if PaymentRecallReasonCode.values.map(_.toString).contains(reasonCode) => PaymentReturnReasonCode.FOLLOWING_CANCELLATION_REQUEST
            case _ => PaymentReturnReasonCode.withName(reasonCodeString)
          }


      }

      val result = InBoundNotifyTransactionRequest(
        inboundAdapterCallContext = InboundAdapterCallContext(
          callContext.correlationId,
          callContext.sessionId,
          callContext.generalContext
        ),
        status = successInBoundStatus,
        data = TransactionRequestStatus.FORWARDED
      )
      obpAkkaConnector ! result


    case _ => println(s"Message received but not implemented")
  }

  def successInBoundStatus: Status = Status("", Nil)

}