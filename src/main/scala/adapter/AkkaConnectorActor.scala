package adapter

import java.time.{LocalDate, LocalDateTime, ZoneId, ZoneOffset}
import java.util.{Date, UUID}

import adapter.obpApiModel.{CounterpartyAccountReference, CustomerAccountReference, HistoricalTransactionJson, ObpApi}
import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import com.openbankproject.commons.dto._
import com.openbankproject.commons.model._
import com.openbankproject.commons.model.enums.TransactionRequestStatus
import io.circe.generic.auto._
import io.circe.syntax._
import model.enums.sepaReasonCodes.PaymentRecallNegativeAnswerReasonCode.{apply => _, values => _, withName => _, _}
import model.enums.sepaReasonCodes.PaymentRecallReasonCode._
import model.enums.sepaReasonCodes.PaymentReturnReasonCode.PaymentReturnReasonCode
import model.enums.sepaReasonCodes.{PaymentRecallNegativeAnswerReasonCode, PaymentReturnReasonCode}
import model.enums.{SepaCreditTransferTransactionStatus, SepaFileType, SepaMessageStatus, SepaMessageType}
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaFile, SepaMessage}
import sepa.{PaymentRecallMessage, PaymentRecallNegativeAnswerMessage, PaymentReturnMessage, SepaUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

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
            settlementDate = Some(LocalDate.now().plusDays(1)),
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
            posted = creditTransferTransaction.creationDateTime.atZone(ZoneId.of("Europe/Paris")).withZoneSameInstant(ZoneOffset.UTC).format(HistoricalTransactionJson.jsonDateTimeFormatter),
            completed = creditTransferTransaction.creationDateTime.atZone(ZoneId.of("Europe/Paris")).withZoneSameInstant(ZoneOffset.UTC).format(HistoricalTransactionJson.jsonDateTimeFormatter),
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
                obpTransactionRequestId = Some(transactionRequestId),
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

          val createdObpTransactionId = for {
            originalTransaction <- SepaCreditTransferTransaction.getByObpTransactionId(originalObpTransactionId)
            transactionRequestValid = (for {
              fromAccountIban <- fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address))
              toAccountIban <- toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address))
              originalTransactionDebtorIban <- originalTransaction.debtorAccount
              originalTransactionCreditorIban <- originalTransaction.creditorAccount
            } yield fromAccountIban == originalTransactionCreditorIban && toAccountIban == originalTransactionDebtorIban).getOrElse(false)
            createdObpTransactionId <- if (transactionRequestValid) {
              // TODO : We can use the sepa file direction to detect which account to request to the API
              val fromAccountIban = fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)).getOrElse(Iban(""))
              val toAccountIban = toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)).getOrElse(Iban(""))
              val accountFrom = ObpApi.getAccountIdByIban(Adapter.BANK_ID, Adapter.VIEW_ID, fromAccountIban)
              val accountTo = ObpApi.getAccountIdByIban(Adapter.BANK_ID, Adapter.VIEW_ID, toAccountIban)

              accountFrom.flatMap(_ => { // Case where the account refund the counterparty
                val reasonCodeString = description.split(" - ").last.split(":").last.trim
                val refundReasonCode: PaymentReturnReasonCode = reasonCodeString match {
                  case reasonCode if values.map(_.toString).contains(reasonCode) => PaymentReturnReasonCode.FOLLOWING_CANCELLATION_REQUEST
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
                  posted = originalTransaction.creationDateTime.atZone(ZoneId.of("Europe/Paris")).withZoneSameInstant(ZoneOffset.UTC).format(HistoricalTransactionJson.jsonDateTimeFormatter),
                  completed = originalTransaction.creationDateTime.atZone(ZoneId.of("Europe/Paris")).withZoneSameInstant(ZoneOffset.UTC).format(HistoricalTransactionJson.jsonDateTimeFormatter),
                  `type` = transactionRequestType.value,
                  charge_policy = chargePolicy
                )
                for {
                  createdObpTransactionId <- ObpApi.saveHistoricalTransaction(historicalTransactionJson)
                  _ <- PaymentReturnMessage.returnTransaction(
                    originalTransaction, originalTransaction.creditorName.orElse(originalTransaction.creditorAgent.map(_.bic)).getOrElse(""),
                    refundReasonCode, Some(transactionRequestId), Some(createdObpTransactionId))
                } yield createdObpTransactionId
              }).fallbackTo(accountTo.flatMap(_ => { // Case where the counterparty refund the account
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
                  posted = originalTransaction.creationDateTime.atZone(ZoneId.of("Europe/Paris")).withZoneSameInstant(ZoneOffset.UTC).format(HistoricalTransactionJson.jsonDateTimeFormatter),
                  completed = originalTransaction.creationDateTime.atZone(ZoneId.of("Europe/Paris")).withZoneSameInstant(ZoneOffset.UTC).format(HistoricalTransactionJson.jsonDateTimeFormatter),
                  `type` = transactionRequestType.value,
                  charge_policy = chargePolicy
                )
                for {
                  createdObpTransactionId <- ObpApi.saveHistoricalTransaction(historicalTransactionJson)
                } yield createdObpTransactionId
              }))
            } else Future.failed(new Exception(s"Transaction request invalid, counterparty_iban don't match with the original SEPA transaction ${originalTransaction.id} (OBP original transaction : ${originalObpTransactionId.value}). Maybe the errror is coming from the to/from field name"))

          } yield createdObpTransactionId

          createdObpTransactionId.map(obpTransactionId => {
            val result = InBoundMakePaymentv210(
              inboundAdapterCallContext = InboundAdapterCallContext(
                callContext.correlationId,
                callContext.sessionId,
                callContext.generalContext
              ),
              status = successInBoundStatus,
              data = TransactionId(obpTransactionId.toString)
            )
            obpAkkaConnector ! result
          }).recover {
            case exception: Exception => log.error(exception.getMessage)
          }
      }

    case OutBoundNotifyTransactionRequest(callContext, fromAccount, toAccount, transactionRequest) =>
      println(s"Notify transaction request received : ${transactionRequest.id}")
      println(transactionRequest)

      val obpAkkaConnector = sender

      // We are doing something here only when we need to send a message that don't create a transaction
      // Example : Recall request, Recall rejection
      // A recall is when an App account want to recover sended funds
      val newTransactionRequestStatus = (transactionRequest.`type`, TransactionRequestStatus.withName(transactionRequest.status)) match {
        case ("REFUND", TransactionRequestStatus.INITIATED) =>
          val maybePaymentRecallReasonCode = Try(withName(transactionRequest.body.description.split(" - ").last.split(":").last.trim)).toOption
          maybePaymentRecallReasonCode match {
            case Some(paymentRecallReasonCode) =>
              val originalObpTransactionId = TransactionId(transactionRequest.body.description.split(" - ").reverse(1).split(Array('(', ')'))(1))
              for {
                originalTransaction <- SepaCreditTransferTransaction.getByObpTransactionId(originalObpTransactionId)
                transactionRequestValid = (for {
                  fromAccountIban <- fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address))
                  toAccountIban <- toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address))
                  originalTransactionDebtorIban <- originalTransaction.debtorAccount
                  originalTransactionCreditorIban <- originalTransaction.creditorAccount
                } yield fromAccountIban == originalTransactionCreditorIban && toAccountIban == originalTransactionDebtorIban).getOrElse(false)
                newTransactionRequestStatus <- if (transactionRequestValid) {
                  // TODO : We can use the sepa file direction to detect which account to request to the API
                  val fromAccountIban = fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)).getOrElse(Iban(""))
                  val toAccountIban = toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)).getOrElse(Iban(""))
                  val accountFrom = ObpApi.getAccountIdByIban(Adapter.BANK_ID, Adapter.VIEW_ID, fromAccountIban)
                  val accountTo = ObpApi.getAccountIdByIban(Adapter.BANK_ID, Adapter.VIEW_ID, toAccountIban)

                  accountFrom.map(_ => { // Case where the counterparty want to be refund by the account (RECALL Request Receipt)
                    // In this case, the transactionRequest is coming from the SEPA Adapter, so no need to save anything
                    // We just set the TransactionRequestStatus to NEXT_CHALLENGE_PENDING to say the app that it need to complete
                    // (Accept or Reject) the created challenge.
                    TransactionRequestStatus.NEXT_CHALLENGE_PENDING
                  }).fallbackTo(accountTo.flatMap(_ => { // Case where the account want to be refund by the counterparty (RECALL Request Sending)
                    // In this case, the transactionRequest is coming from the OBP API (APP), so no need to send the recall message
                    // We then set the TransactionRequestStatus to FORWARDED to indicate that we're waiting for a response from the counterparty bank
                    val originator = paymentRecallReasonCode match {
                      case FRAUDULENT_ORIGIN | TECHNICAL_PROBLEM | DUPLICATE_PAYMENT =>
                        originalTransaction.debtorAgent.map(_.bic).getOrElse(Adapter.BANK_BIC.bic)
                      case REQUESTED_BY_CUSTOMER | WRONG_AMOUNT | INVALID_CREDITOR_ACCOUNT_NUMBER =>
                        originalTransaction.debtorName.getOrElse(Adapter.BANK_BIC.bic)
                    }
                    val additionalInformation = transactionRequest.body.description.split(" - ").reverse.drop(2).reverse.mkString(" - ")
                    for {
                      _ <- PaymentRecallMessage.recallTransaction(originalTransaction, originator, paymentRecallReasonCode, Some(additionalInformation),
                        Some(transactionRequest.id))
                    } yield TransactionRequestStatus.FORWARDED
                  }
                  ))
                } else {
                  log.error(s"Transaction request invalid, counterparty_iban don't match with the original SEPA transaction ${originalTransaction.id} (OBP original transaction : ${originalObpTransactionId.value}). Maybe the errror is coming from the to/from field name")
                  Future.successful(TransactionRequestStatus.FAILED)
                }
              } yield newTransactionRequestStatus
            case _ => Future.successful(TransactionRequestStatus.INITIATED)
          }

        case ("REFUND", TransactionRequestStatus.REJECTED) =>
          val paymentRecallNegativeAnswerReasonCode: PaymentRecallNegativeAnswerReasonCode =
            PaymentRecallNegativeAnswerReasonCode.withName(transactionRequest.body.description
              .split(" - Refund reject reason code : ").last.split(" ").head)
          val paymentRecallNegativeAnswerAditionalInformation = transactionRequest.body.description
            .split(" - Refund reject additional information : ").lastOption
          (for {
            originalCreditTransferTransaction <- SepaCreditTransferTransaction.getByObpTransactionRequestId(transactionRequest.id)
            originalRecallMessage <- SepaMessage.getBySepaCreditTransferTransactionId(originalCreditTransferTransaction.id)
              .map(_.find(_.messageType == SepaMessageType.B2B_PAYMENT_RECALL))
            originalRecallMessageFile <- originalRecallMessage match {
              case Some(recallMessage) =>
                recallMessage.sepaFileId match {
                  case Some(fileId) => SepaFile.getById(fileId)
                  case None => Future.failed(new Exception(s"originalRecallMessage ${recallMessage.id} is not linked to a SepaFileId"))
                }
              case None => Future.failed(new Exception(s"originalRecallMessage linked with with SepaCreditTransferTransactionId(${originalCreditTransferTransaction.id}) not found"))
            }
            newTransactionRequestStatus <- originalRecallMessageFile match {
              case SepaFileType.SCT_IN =>
                for {
                  _ <- PaymentRecallNegativeAnswerMessage.sendRecallNegativeAnswer(
                    originalCreditTransferTransaction,
                    Seq(PaymentRecallNegativeAnswerMessage.ReasonInformation(
                      originator = paymentRecallNegativeAnswerReasonCode match {
                        case CUSTOMER_DECISION => originalCreditTransferTransaction.creditorName
                          .orElse(originalCreditTransferTransaction.creditorAgent.map(_.bic))
                          .getOrElse(Adapter.BANK_BIC.bic)
                        case _ => originalCreditTransferTransaction.creditorAgent.map(_.bic).getOrElse(Adapter.BANK_BIC.bic)
                      },
                      reasonCode = paymentRecallNegativeAnswerReasonCode,
                      additionalInformation = paymentRecallNegativeAnswerAditionalInformation
                    )),
                    Some(transactionRequest.id)
                  )
                } yield TransactionRequestStatus.REJECTED

              case SepaFileType.SCT_OUT => Future.successful(TransactionRequestStatus.REJECTED)
            }
          } yield newTransactionRequestStatus)

        case _ => Future.successful(TransactionRequestStatus.withName(transactionRequest.status))
      }
      newTransactionRequestStatus.map(status => {
        val result = InBoundNotifyTransactionRequest(InboundAdapterCallContext(
          callContext.correlationId,
          callContext.sessionId,
          callContext.generalContext
        ), successInBoundStatus, status)
        obpAkkaConnector ! result
      }).recover {
        case exception: Exception => log.error(exception.getMessage)
      }

    case _ => println(s"Message received but not implemented")
  }

  def successInBoundStatus: Status = Status("", Nil)

}