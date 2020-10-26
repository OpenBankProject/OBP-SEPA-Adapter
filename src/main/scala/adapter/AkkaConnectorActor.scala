package adapter

import java.time.{LocalDate, LocalDateTime, ZoneId, ZoneOffset}
import java.util.{Date, UUID}

import adapter.obpApiModel.{HistoricalTransactionAccountJsonV310, ObpApi}
import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.cluster.Cluster
import com.openbankproject.commons.dto._
import com.openbankproject.commons.model._
import com.openbankproject.commons.model.enums.TransactionRequestStatus
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Json, JsonObject}
import model.enums._
import model.enums.sepaReasonCodes.PaymentRecallNegativeAnswerReasonCode.{apply => _, values => _, withName => _, _}
import model.enums.sepaReasonCodes.PaymentRecallReasonCode._
import model.enums.sepaReasonCodes.PaymentReturnReasonCode.PaymentReturnReasonCode
import model.enums.sepaReasonCodes.{PaymentRecallNegativeAnswerReasonCode, PaymentReturnReasonCode}
import model.jsonClasses._
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaFile, SepaMessage}
import sepa.SepaUtil
import sepa.sct.message._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class AkkaConnectorActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system

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

    // Case we receive a makePaymentv210 message
    case OutBoundMakePaymentv210(callContext, fromAccount, toAccount, transactionRequestId, transactionRequestCommonBody, amount, description, transactionRequestType, chargePolicy) =>
      println("Make payment v210 message received")

      val obpAkkaConnector = sender

      // In the case the transaction request type is SEPA
      transactionRequestType.value match {
        case "SEPA" =>
          val creditTransferTransactionId = UUID.randomUUID()

          (for {
            fromAccountBic <- ObpApi.getBicByBankId(fromAccount.bankId)
            toAccountBic <- toAccount.attributes.flatMap(_.find(attribute =>
              attribute.name == "BANK_ROUTING_SCHEME" && attribute.value == "BIC")).flatMap(_ =>
              toAccount.attributes.flatMap(_.find(attribute =>
                attribute.name == "BANK_ROUTING_ADDRESS" && attribute.value.nonEmpty)).map(_.value).map(Bic).map(Future.successful)
            ).getOrElse(Future.failed(new Exception("The beneficiary bank BIC is mandatory to process a payment")))

            // We create a new transaction on the Adapter side (only in memory at the moment)
            creditTransferTransaction = SepaCreditTransferTransaction(
              id = creditTransferTransactionId,
              amount = amount,
              debtor = Some(Party(Some(fromAccount.accountHolder))),
              debtorAccount = fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)),
              debtorAgent = Some(fromAccountBic),
              ultimateDebtor = None,
              creditor = Some(Party(Some(toAccount.accountHolder))),
              creditorAccount = toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)),
              creditorAgent = Some(toAccountBic),
              ultimateCreditor = None,
              purposeCode = None,
              description = Some(description),
              creationDateTime = LocalDateTime.now(),
              settlementDate = Some(LocalDate.now().plusDays(1)),
              transactionIdInSepaFile = SepaUtil.removeDashesToUUID(creditTransferTransactionId),
              instructionId = None,
              endToEndId = SepaUtil.removeDashesToUUID(creditTransferTransactionId),
              settlementInformation = Some(SettlementInformation(settlementMethod = SettlementMethod.CLEARING_SYSTEM)),
              paymentTypeInformation = Some(PaymentTypeInformation(serviceLevelCode = Some(ServiceLevelCode.SEPA))),
              status = SepaCreditTransferTransactionStatus.UNPROCESSED,
              customFields = None
            )

            debtor = HistoricalTransactionAccountJsonV310(
              bank_id = Some(fromAccount.bankId.value),
              account_id = Some(fromAccount.accountId.value),
              counterparty_id = None
            )

            creditor <- ObpApi.getOrCreateCounterparty(
              bankId = fromAccount.bankId,
              accountId = fromAccount.accountId,
              name = toAccount.accountHolder,
              iban = Iban(creditTransferTransaction.creditorAccount.map(_.iban).getOrElse("")),
              bic = toAccountBic
            ).map(counterparty =>
              HistoricalTransactionAccountJsonV310(
                bank_id = None,
                account_id = None,
                counterparty_id = Some(counterparty.counterparty_id)
              )
            )

            // We send the request to save the transaction in OBP-API
            createdObpTransaction <- ObpApi.saveHistoricalTransaction(
              debtor = debtor,
              creditor = creditor,
              amount = creditTransferTransaction.amount,
              description = creditTransferTransaction.description.getOrElse("")
            )

            createdObpTransactionId = TransactionId(createdObpTransaction.transaction_id)

            // If it succeed, we save the transaction in the SEPA Adapter
            _ <- creditTransferTransaction.insert()
            // We now look for the unprocessed credit transfer message
            unprocessedCreditTransferMessage <- SepaMessage.getUnprocessedByType(SepaMessageType.B2B_CREDIT_TRANSFER)
              .map(_.headOption.getOrElse {
                // if we don't find one, we create a new message and save it into the Adapter database
                val sepaMessageId = UUID.randomUUID()
                val message = SepaMessage(
                  sepaMessageId, LocalDateTime.now(), SepaMessageType.B2B_CREDIT_TRANSFER,
                  SepaMessageStatus.UNPROCESSED, sepaFileId = None, SepaUtil.removeDashesToUUID(sepaMessageId),
                  numberOfTransactions = 0, totalAmount = 0, None, None, None,
                  customFields = Some(Json.fromJsonObject(JsonObject.empty
                    .add(SepaMessageCustomField.CREDIT_TRANFER_SETTLEMENT_INFORMATION.toString,
                      SettlementInformation(settlementMethod = SettlementMethod.CLEARING_SYSTEM).toJson)
                    .add(SepaMessageCustomField.CREDIT_TRANFER_PAYMENT_TYPE_INFORMATION.toString,
                      PaymentTypeInformation(serviceLevelCode = Some(ServiceLevelCode.SEPA)).toJson)
                  ))
                )
                message.insert()
                message
              })

            // We can now link the transaction with the credit transfer message
            _ <- creditTransferTransaction.linkMessage(
              sepaMessageId = unprocessedCreditTransferMessage.id,
              transactionStatusIdInSepaFile = creditTransferTransaction.transactionIdInSepaFile,
              obpTransactionRequestId = Some(transactionRequestId),
              obpTransactionId = Some(createdObpTransactionId)
            )
            // We also update the message total amount and number of transactions
            _ <- unprocessedCreditTransferMessage.copy(
              numberOfTransactions = unprocessedCreditTransferMessage.numberOfTransactions + 1,
              totalAmount = unprocessedCreditTransferMessage.totalAmount + creditTransferTransaction.amount
            ).update()

            // We send the inBound message to the OBP-API connector with the newly created transactionId in OBP
            result = InBoundMakePaymentv210(
              inboundAdapterCallContext = InboundAdapterCallContext(
                callContext.correlationId,
                callContext.sessionId,
                callContext.generalContext
              ),
              status = successInBoundStatus,
              data = TransactionId(createdObpTransactionId.toString)
            )
          } yield {
            obpAkkaConnector ! result
          })
            .recover {
              case exception: Exception =>
                log.error(exception.getMessage)

                val result = InBoundMakePaymentv210(
                  inboundAdapterCallContext = InboundAdapterCallContext(
                    callContext.correlationId,
                    callContext.sessionId,
                    callContext.generalContext
                  ),
                  status = Status("Payment error", List(InboundStatusMessage(
                    "", "Error in MakePayment", "", exception.getMessage
                  ))),
                  data = null
                )
                obpAkkaConnector ! result
            }


        // In the case the transaction request type is "REFUND"
        case "REFUND" =>
          // We parse the original OBP transaction Id from the description (Should be put in Transaction request attributes in the future)
          val originalObpTransactionId = TransactionId(description.split(" - ").reverse(1).split(Array('(', ')'))(1))

          val createdObpTransactionId = for {
            // We retrieve the Adapter transaction from the obp original transaction Id
            originalTransaction <- SepaCreditTransferTransaction.getByObpTransactionId(originalObpTransactionId)
            // We get the IBANs from the TransactionRequest and the original transaction to see if all is ok
            transactionRequestValid = (for {
              fromAccountIban <- fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address))
              toAccountIban <- toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address))
              originalTransactionDebtorIban <- originalTransaction.debtorAccount
              originalTransactionCreditorIban <- originalTransaction.creditorAccount
              // If all is ok, the following condition should be true, otherwise, one IBAN is not corresponding to the original transaction
            } yield fromAccountIban == originalTransactionCreditorIban && toAccountIban == originalTransactionDebtorIban).getOrElse(false)
            createdObpTransactionId <- if (transactionRequestValid) {
              // Here, we want to detect who of the debtor/creditor is the owner of an OBP Account
              // TODO : We can use the sepa file direction to detect which account to request to the API
              val fromAccountIban = fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)).getOrElse(Iban(""))
              val toAccountIban = toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)).getOrElse(Iban(""))
              // So one of the two following OBP request will fail and one will success (WARNING : this is not a good method to do this)
              val accountFrom = ObpApi.getAccountByIban(Some(Adapter.BANK_ID), fromAccountIban)
              val accountTo = ObpApi.getAccountByIban(Some(Adapter.BANK_ID), toAccountIban)

              // In case the fromAccount is the OBP account owner and he want to refund the counterparty (SEPA Return message)
              accountFrom.flatMap(obpAccountFrom => {
                // We parse the return reason code from the description
                val reasonCodeString = description.split(" - ").last.split(":").last.trim
                val refundReasonCode: PaymentReturnReasonCode = reasonCodeString match {
                  // In case the transaction request refund reason code was a recall reason code, the return reason code is FOLLOWING_CANCELLATION_REQUEST
                  case reasonCode if values.map(_.toString).contains(reasonCode) => PaymentReturnReasonCode.FOLLOWING_CANCELLATION_REQUEST
                  // Else, we just parse the return reason code
                  case _ => PaymentReturnReasonCode.withName(reasonCodeString)
                }

                val debtor = HistoricalTransactionAccountJsonV310(
                  bank_id = Some(Adapter.BANK_ID.value),
                  account_id = Some(obpAccountFrom.id),
                  counterparty_id = None
                )

                for {
                  creditor <- ObpApi.getOrCreateCounterparty(
                    bankId = Adapter.BANK_ID,
                    accountId = debtor.account_id.map(AccountId(_)).get,
                    name = originalTransaction.debtor.flatMap(_.name).getOrElse(""),
                    iban = Iban(originalTransaction.debtorAccount.map(_.iban).getOrElse("")),
                    bic = Bic(originalTransaction.debtorAgent.map(_.bic).getOrElse(""))
                  ).map(counterparty =>
                    HistoricalTransactionAccountJsonV310(
                      bank_id = None,
                      account_id = None,
                      counterparty_id = Some(counterparty.counterparty_id)
                    )
                  )

                  // We send the request to save the transaction in OBP-API
                  createdObpTransaction <- ObpApi.saveHistoricalTransaction(
                    debtor = debtor,
                    creditor = creditor,
                    amount = originalTransaction.amount,
                    description = description
                  )

                  createdObpTransactionId = TransactionId(createdObpTransaction.transaction_id)

                  // We can now create and save the return message and link it with the Adapter original transaction
                  _ <- PaymentReturnMessage.returnTransaction(
                    originalTransaction, originalTransaction.creditor.flatMap(_.name).orElse(originalTransaction.creditorAgent.map(_.bic)).getOrElse(""),
                    refundReasonCode, Some(transactionRequestId), Some(createdObpTransactionId))
                } yield createdObpTransactionId

                // In case the toAccount is the OBP account owner and the counterparty want to refund the account (SEPA Return message)
              }).fallbackTo(accountTo.flatMap(obpAccountTo => { // Case where the counterparty refund the account

                val creditor = HistoricalTransactionAccountJsonV310(
                  bank_id = Some(Adapter.BANK_ID.value),
                  account_id = Some(obpAccountTo.id),
                  counterparty_id = None
                )

                for {
                  debtor <- ObpApi.getOrCreateCounterparty(
                    bankId = Adapter.BANK_ID,
                    accountId = creditor.account_id.map(AccountId(_)).get,
                    name = originalTransaction.creditor.flatMap(_.name).getOrElse(""),
                    iban = Iban(originalTransaction.creditorAccount.map(_.iban).getOrElse("")),
                    bic = Bic(originalTransaction.creditorAgent.map(_.bic).getOrElse(""))
                  ).map(counterparty =>
                    HistoricalTransactionAccountJsonV310(
                      bank_id = None,
                      account_id = None,
                      counterparty_id = Some(counterparty.counterparty_id)
                    )
                  )

                  // We send the request to save the transaction in OBP-API
                  createdObpTransaction <- ObpApi.saveHistoricalTransaction(
                    debtor = debtor,
                    creditor = creditor,
                    amount = originalTransaction.amount,
                    description = description
                  )

                  createdObpTransactionId = TransactionId(createdObpTransaction.transaction_id)
                } yield createdObpTransactionId
              }))
            } else Future.failed(new Exception(s"Transaction request invalid, counterparty_iban don't match with the original SEPA transaction ${originalTransaction.id} (OBP original transaction : ${originalObpTransactionId.value}). Maybe the errror is coming from the to/from field name"))

          } yield createdObpTransactionId

          // We then send the inBound message response to the OBP Connector with the created OBP TransactionId
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

    // Case we receive a makePaymentv210 message
    case OutBoundMakePaymentV400(callContext, transactionRequest, reasons) =>
      println("Make payment v400 message received")

      val obpAkkaConnector = sender

      // In the case the transaction request type is SEPA
      transactionRequest.`type` match {
        case "SEPA" =>
          val creditTransferTransactionId = UUID.randomUUID()

          (for {
            fromAccount <- ObpApi.getAccountByAccountId(BankId(transactionRequest.from.bank_id),
              AccountId(transactionRequest.from.account_id))
            fromAccountBic <- ObpApi.getBicByBankId(BankId(fromAccount.bank_id))
            toAccountBic = Bic(transactionRequest.other_bank_routing_address)

            // We create a new transaction on the Adapter side (only in memory at the moment)
            creditTransferTransaction = SepaCreditTransferTransaction(
              id = creditTransferTransactionId,
              amount = BigDecimal(transactionRequest.body.value.amount),
              debtor = Some(Party(fromAccount.owners.headOption.map(_.display_name))),
              debtorAccount = fromAccount.account_routings.find(_.scheme == "IBAN").map(a => Iban(a.address)),
              debtorAgent = Some(fromAccountBic),
              ultimateDebtor = None,
              creditor = Some(Party(Some(transactionRequest.name))),
              creditorAccount = Some(Iban(transactionRequest.other_account_routing_address)),
              creditorAgent = Some(toAccountBic),
              ultimateCreditor = None,
              purposeCode = None,
              description = Some(transactionRequest.body.description),
              creationDateTime = LocalDateTime.now(),
              settlementDate = Some(LocalDate.now().plusDays(1)),
              transactionIdInSepaFile = SepaUtil.removeDashesToUUID(creditTransferTransactionId),
              instructionId = None,
              endToEndId = SepaUtil.removeDashesToUUID(creditTransferTransactionId),
              settlementInformation = Some(SettlementInformation(settlementMethod = SettlementMethod.CLEARING_SYSTEM)),
              paymentTypeInformation = Some(PaymentTypeInformation(serviceLevelCode = Some(ServiceLevelCode.SEPA))),
              status = SepaCreditTransferTransactionStatus.UNPROCESSED,
              customFields = None
            )

            debtor = HistoricalTransactionAccountJsonV310(
              bank_id = Some(fromAccount.bank_id),
              account_id = Some(fromAccount.id),
              counterparty_id = None
            )

            creditor <- ObpApi.getOrCreateCounterparty(
              bankId = BankId(fromAccount.bank_id),
              accountId = AccountId(fromAccount.id),
              name = transactionRequest.name,
              iban = Iban(creditTransferTransaction.creditorAccount.map(_.iban).getOrElse("")),
              bic = toAccountBic
            ).map(counterparty =>
              HistoricalTransactionAccountJsonV310(
                bank_id = None,
                account_id = None,
                counterparty_id = Some(counterparty.counterparty_id)
              )
            )

            // We send the request to save the transaction in OBP-API
            createdObpTransaction <- ObpApi.saveHistoricalTransaction(
              debtor = debtor,
              creditor = creditor,
              amount = creditTransferTransaction.amount,
              description = creditTransferTransaction.description.getOrElse("")
            )

            createdObpTransactionId = TransactionId(createdObpTransaction.transaction_id)

            // If it succeed, we save the transaction in the SEPA Adapter
            _ <- creditTransferTransaction.insert()
            // We now look for the unprocessed credit transfer message
            unprocessedCreditTransferMessage <- SepaMessage.getUnprocessedByType(SepaMessageType.B2B_CREDIT_TRANSFER)
              .map(_.headOption.getOrElse {
                // if we don't find one, we create a new message and save it into the Adapter database
                val sepaMessageId = UUID.randomUUID()
                val message = SepaMessage(
                  sepaMessageId, LocalDateTime.now(), SepaMessageType.B2B_CREDIT_TRANSFER,
                  SepaMessageStatus.UNPROCESSED, sepaFileId = None, SepaUtil.removeDashesToUUID(sepaMessageId),
                  numberOfTransactions = 0, totalAmount = 0, None, None, None,
                  customFields = Some(Json.fromJsonObject(JsonObject.empty
                    .add(SepaMessageCustomField.CREDIT_TRANFER_SETTLEMENT_INFORMATION.toString,
                      SettlementInformation(settlementMethod = SettlementMethod.CLEARING_SYSTEM).toJson)
                    .add(SepaMessageCustomField.CREDIT_TRANFER_PAYMENT_TYPE_INFORMATION.toString,
                      PaymentTypeInformation(serviceLevelCode = Some(ServiceLevelCode.SEPA)).toJson)
                  ))
                )
                message.insert()
                message
              })

            // We can now link the transaction with the credit transfer message
            _ <- creditTransferTransaction.linkMessage(
              sepaMessageId = unprocessedCreditTransferMessage.id,
              transactionStatusIdInSepaFile = creditTransferTransaction.transactionIdInSepaFile,
              obpTransactionRequestId = Some(transactionRequest.id),
              obpTransactionId = Some(createdObpTransactionId)
            )
            // We also update the message total amount and number of transactions
            _ <- unprocessedCreditTransferMessage.copy(
              numberOfTransactions = unprocessedCreditTransferMessage.numberOfTransactions + 1,
              totalAmount = unprocessedCreditTransferMessage.totalAmount + creditTransferTransaction.amount
            ).update()

            // We send the inBound message to the OBP-API connector with the newly created transactionId in OBP
            result = InBoundMakePaymentV400(
              inboundAdapterCallContext = InboundAdapterCallContext(
                callContext.correlationId,
                callContext.sessionId,
                callContext.generalContext
              ),
              status = successInBoundStatus,
              data = TransactionId(createdObpTransactionId.toString)
            )
          } yield {
            obpAkkaConnector ! result
          })
            .recover {
              case exception: Exception =>
                log.error(exception.getMessage)

                val result = InBoundMakePaymentV400(
                  inboundAdapterCallContext = InboundAdapterCallContext(
                    callContext.correlationId,
                    callContext.sessionId,
                    callContext.generalContext
                  ),
                  status = Status("Payment error", List(InboundStatusMessage(
                    "", "Error in MakePayment", "", exception.getMessage
                  ))),
                  data = null
                )
                obpAkkaConnector ! result
            }
      }

    // The adapter receive a notifyTransactionRequest message from the OBP-API connector
    case OutBoundNotifyTransactionRequest(callContext, fromAccount, toAccount, transactionRequest) =>
      println(s"Notify transaction request received : ${transactionRequest.id}")
      println(transactionRequest)

      val obpAkkaConnector = sender

      // We are doing something here only when we need to send a message that don't create a transaction
      // Example : Recall request, Recall rejection
      // A recall is when an App account want to recover sended funds
      val newTransactionRequestStatus = (transactionRequest.`type`, TransactionRequestStatus.withName(transactionRequest.status)) match {
        // if the transactionRequest type is REFUND and is initiated
        case ("REFUND", TransactionRequestStatus.INITIATED) =>
          // We try to parse the recallReasonCode
          val maybePaymentRecallReasonCode = Try(withName(transactionRequest.body.description.split(" - ").last.split(":").last.trim)).toOption
          maybePaymentRecallReasonCode match {
            // If we have a recall reason code
            case Some(paymentRecallReasonCode) =>
              // We parse the original OBP transaction id in the description
              val originalObpTransactionId = TransactionId(transactionRequest.body.description.split(" - ").reverse(1).split(Array('(', ')'))(1))
              for {
                originalTransaction <- SepaCreditTransferTransaction.getByObpTransactionId(originalObpTransactionId)
                transactionRequestValid = (for {
                  // We get the IBANs from the TransactionRequest and the original transaction to see if all is ok
                  fromAccountIban <- fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address))
                  toAccountIban <- toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address))
                  originalTransactionDebtorIban <- originalTransaction.debtorAccount
                  originalTransactionCreditorIban <- originalTransaction.creditorAccount
                  // If all is ok, the following condition should be true, otherwise, one IBAN is not corresponding to the original transaction
                } yield fromAccountIban == originalTransactionCreditorIban && toAccountIban == originalTransactionDebtorIban).getOrElse(false)
                newTransactionRequestStatus <- if (transactionRequestValid) {
                  // TODO : We can use the sepa file direction to detect which account to request to the API
                  // Here, we want to detect who of the debtor/creditor is the owner of an OBP Account
                  val fromAccountIban = fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)).getOrElse(Iban(""))
                  val toAccountIban = toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)).getOrElse(Iban(""))
                  // So one of the two following OBP request will fail and one will success (WARNING : this is not a good method to do this)
                  val accountFrom = ObpApi.getAccountByIban(Some(Adapter.BANK_ID), fromAccountIban)
                  val accountTo = ObpApi.getAccountByIban(Some(Adapter.BANK_ID), toAccountIban)

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
                        originalTransaction.debtor.flatMap(_.name).getOrElse(Adapter.BANK_BIC.bic)
                    }
                    // we parse the additional information from the transaction request description
                    val additionalInformation = transactionRequest.body.description.split(" - ").reverse.drop(2).reverse.mkString(" - ")
                    for {
                      // Then we recall the transaction (this method update the transaction, create a new message and link it with the original transaction)
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

        // If the transaction request type is REFUND and the status is rejected
        case ("REFUND", TransactionRequestStatus.REJECTED) =>
          // We parse the recall reject reason code from the description
          val paymentRecallNegativeAnswerReasonCode: PaymentRecallNegativeAnswerReasonCode =
            PaymentRecallNegativeAnswerReasonCode.withName(transactionRequest.body.description
              .split(" - Refund reject reason code : ").last.split(" ").head)
          // We parse the recall reject additional information from the description
          val paymentRecallNegativeAnswerAditionalInformation = transactionRequest.body.description
            .split(" - Refund reject additional information : ").lastOption
          (for {
            // we get the REFUND transaction Request
            originalCreditTransferTransaction <- SepaCreditTransferTransaction.getByObpTransactionRequestId(transactionRequest.id)
            // we get the original recall message
            originalRecallMessage <- SepaMessage.getBySepaCreditTransferTransactionId(originalCreditTransferTransaction.id)
              .map(_.find(_.messageType == SepaMessageType.B2B_PAYMENT_RECALL))
            // we get the original SEPA file
            originalRecallMessageFile <- originalRecallMessage match {
              case Some(recallMessage) =>
                recallMessage.sepaFileId match {
                  case Some(fileId) => SepaFile.getById(fileId)
                  case None => Future.failed(new Exception(s"originalRecallMessage ${recallMessage.id} is not linked to a SepaFileId"))
                }
              case None => Future.failed(new Exception(s"originalRecallMessage linked with with SepaCreditTransferTransactionId(${originalCreditTransferTransaction.id}) not found"))
            }
            newTransactionRequestStatus <- originalRecallMessageFile.fileType match {
              // If the original SEPA file is an incoming one, the notifyTransactionRequest message is the result of
              // a REJECT response to a received recall message, so in this case, we need to create the recallNegativeAnswer message
              case SepaFileType.SCT_IN =>
                for {
                  _ <- PaymentRecallNegativeAnswerMessage.sendRecallNegativeAnswer(
                    originalCreditTransferTransaction,
                    Seq(PaymentRecallNegativeAnswerMessage.ReasonInformation(
                      originator = paymentRecallNegativeAnswerReasonCode match {
                        case CUSTOMER_DECISION => originalCreditTransferTransaction.creditor.flatMap(_.name)
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

              // If the origianl recall SEPA file is an outgoing one,
              // it means that the REJECT message was generated by an incoming file
              // So we don't need to do nothing here because the NotifyTransactionRequest message
              // is the result of the SEPA Adapter after calling the answerTransactionRequestChallenge (REJECT) endpoint
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
        ), successInBoundStatus, TransactionRequestStatusValue(status.toString))
        obpAkkaConnector ! result
      }).recover {
        case exception: Exception => log.error(exception.getMessage)
      }

    case _ => println(s"Message received but not implemented")
  }

  def successInBoundStatus: Status = Status("", Nil)

}