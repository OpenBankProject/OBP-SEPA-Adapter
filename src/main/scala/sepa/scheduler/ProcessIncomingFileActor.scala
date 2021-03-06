package sepa.scheduler


import java.time.{LocalDateTime, ZoneId, ZoneOffset}

import adapter.obpApiModel._
import adapter.{Adapter, ObpAccountNotFoundException}
import akka.actor.{Actor, ActorLogging}
import com.openbankproject.commons.model.{AccountId, AmountOfMoney, Iban, TransactionId, TransactionRequestId}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Json, JsonObject}
import model.enums.SepaMessageType.{B2B_INQUIRY_CLAIM_NON_RECEIP_NEGATIVE_RESPONSE, B2B_INQUIRY_CLAIM_NON_RECEIP_POSITIVE_RESPONSE, B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION_NEGATIVE_RESPONSE, B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION_POSITIVE_RESPONSE}
import model.enums._
import model.enums.sepaReasonCodes.PaymentReturnReasonCode
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaFile, SepaMessage, SepaTransactionMessage}
import sepa.sct.message.{SctMessage, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

case class ProcessIncomingCreditTransferMessage(xmlFile: Elem, sepaFile: SepaFile)
case class ProcessIncomingPaymentReturnMessage(xmlFile: Elem, sepaFile: SepaFile)
case class ProcessIncomingPaymentRejectMessage(xmlFile: Elem, sepaFile: SepaFile)
case class ProcessIncomingPaymentRecallMessage(xmlFile: Elem, sepaFile: SepaFile)
case class ProcessIncomingPaymentRecallNegativeAnswerMessage(xmlFile: Elem, sepaFile: SepaFile)
case class ProcessIncomingInquiryClaimNonReceiptMessage(xmlFile: Elem, sepaFile: SepaFile)
case class ProcessIncomingInquiryClaimValueDateCorrectionMessage(xmlFile: Elem, sepaFile: SepaFile)
case class ProcessIncomingInquiryClaimNonReceiptPositiveAnswerMessage(xmlFile: Elem, sepaFile: SepaFile)
case class ProcessIncomingInquiryClaimNonReceiptNegativeAnswerMessage(xmlFile: Elem, sepaFile: SepaFile)
case class ProcessIncomingInquiryClaimValueDateCorrectionPositiveAnswerMessage(xmlFile: Elem, sepaFile: SepaFile)
case class ProcessIncomingInquiryClaimValueDateCorrectionNegativeAnswerMessage(xmlFile: Elem, sepaFile: SepaFile)
case class ProcessIncomingRequestStatusUpdateMessage(xmlFile: Elem, sepaFile: SepaFile)

class ProcessIncomingFileActor extends Actor with ActorLogging {

  def receive: Receive = {

    case ProcessIncomingCreditTransferMessage(xmlFile, sepaFile) =>
      CreditTransferMessage.fromXML(xmlFile, sepaFile.id) match {
        // If we receive a credit transfer transaction
        case Success(creditTransferMessage) =>
          for {
            // We store the message and the transactions
            validTransactions <- preProcessSctMessageTransactions(creditTransferMessage)
            // We update the transactions status
            _ <- Future.sequence(validTransactions.map(_._1.copy(status = SepaCreditTransferTransactionStatus.TO_TRANSFER).update()))
            // We call the saveHistoricalTransaction endpoint to save each transaction in OBP
            integratedTransactions <- Future.sequence(validTransactions.map(transaction => {
              (for {

                creditor <- ObpApi.getAccountByIban(
                  bankId = Some(Adapter.BANK_ID),
                  iban = Iban(transaction._1.creditorAccount.map(_.iban).getOrElse(""))
                ).map(account =>
                  HistoricalTransactionAccountJsonV310(
                    bank_id = Some(Adapter.BANK_ID.value),
                    account_id = Some(account.id),
                    counterparty_id = None
                  )
                )

                debtor <- ObpApi.getOrCreateCounterparty(
                  bankId = Adapter.BANK_ID,
                  accountId = AccountId(creditor.account_id.get),
                  name = transaction._1.debtor.flatMap(_.name).getOrElse(""),
                  iban = Iban(transaction._1.debtorAccount.map(_.iban).getOrElse("")),
                  bic = Bic(transaction._1.debtorAgent.map(_.bic).getOrElse(""))
                ).map(counterparty =>
                  HistoricalTransactionAccountJsonV310(
                    bank_id = None,
                    account_id = None,
                    counterparty_id = Some(counterparty.counterparty_id)
                  )
                )

                obpTransaction <- ObpApi.saveHistoricalTransaction(
                  debtor = debtor,
                  creditor = creditor,
                  amount = transaction._1.amount,
                  description = transaction._1.description.getOrElse("")
                )

                obpTransactionId = TransactionId(obpTransaction.transaction_id)

                _ <- transaction._1.updateMessageLink(creditTransferMessage.message.id, transaction._1.transactionIdInSepaFile, None, Some(obpTransactionId))
                _ <- transaction._1.copy(status = SepaCreditTransferTransactionStatus.TRANSFERED).update()

              } yield ())
                .recoverWith {
                // if we don't find the account, we return the transaction to the originator
                case e: ObpAccountNotFoundException =>
                  log.error(e.getMessage)
                  log.error(s"Credit transfer transaction ${transaction._1.id} returned")
                  PaymentReturnMessage.returnTransaction(transaction._1, transaction._1.creditorAgent.map(_.bic).getOrElse(Adapter.BANK_BIC.bic), PaymentReturnReasonCode.INCORRECT_ACCOUNT_NUMBER)
                case e: Exception =>
                  log.error(e.getMessage)
                  transaction._1.copy(status = SepaCreditTransferTransactionStatus.TRANSFER_ERROR).update()
              }
            }
            ))
            // we the update the message and the file status to PROCESSED
            _ <- creditTransferMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
            _ <- Future(log.info(s"${integratedTransactions.length} integrated transactions from file ${sepaFile.name}"))
          } yield ()

        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }


    case ProcessIncomingPaymentReturnMessage(xmlFile, sepaFile) =>
      PaymentReturnMessage.fromXML(xmlFile, sepaFile.id) match {
        // If we receive a payment return message
        case Success(paymentReturnMessage) =>
          for {
            // we get the original transaction
            validReturnedTransactions <- preProcessSctMessageTransactions(paymentReturnMessage)
            _ <- Future.sequence(validReturnedTransactions.map(_._1.copy(status = SepaCreditTransferTransactionStatus.RETURNED).update()))
            _ <- Future.sequence(validReturnedTransactions.map(transaction =>
              (transaction._1.customFields.flatMap(json =>
                (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_REASON_CODE.toString).headOption.flatMap(_.asString)
                  .flatMap(reasonCode => Try(PaymentReturnReasonCode.withName(reasonCode)).toOption)) match {
                // if the reason code is FOLLOWING_CANCELLATION_REQUEST, this is a return after a recall
                case Some(PaymentReturnReasonCode.FOLLOWING_CANCELLATION_REQUEST) =>
                  (for {
                    // we get the original recall message
                    recallMessage <- SepaMessage.getBySepaCreditTransferTransactionId(transaction._1.id).map(_.find(_.messageType == SepaMessageType.B2B_PAYMENT_RECALL))
                    transactionMessageLink <- recallMessage match {
                      case Some(message) => SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(transaction._1.id, message.id)
                      case None => Future.failed(new Exception(s"Original recall message of returned transaction ${transaction._1.id} not found"))
                    }
                    // we get the accountID from the account IBAN
                    account <- ObpApi.getAccountByIban(Some(Adapter.BANK_ID), transaction._1.debtorAccount.getOrElse(Iban("")))
                    accountId = AccountId(account.id)
                    // we get the transaction Request challenge ID
                    challengeId <- ObpApi.getTransactionRequestChallengeId(Adapter.BANK_ID, accountId, Adapter.VIEW_ID,
                      TransactionRequestId(transactionMessageLink.obpTransactionRequestId.map(_.toString).getOrElse("")))
                    transactionRequestChallengeAnswer = TransactionRequestChallengeAnswer(challengeId, "123")
                    // we answer the challenge with a positive response (Dummy answer `123`) and we get the created transactionId
                    createdObpTransactionId <- ObpApi.answerTransactionRequestChallenge(Adapter.BANK_ID, accountId, Adapter.VIEW_ID,
                      TransactionRequestId(transactionMessageLink.obpTransactionRequestId.map(_.toString).getOrElse("")),
                      transactionRequestChallengeAnswer).flatMap {
                      case Some(obpTransactionId) => Future.successful(obpTransactionId)
                      case None => Future.failed(new Exception(s"ObpTransactionId is missing after challenge $challengeId completed"))
                    }
                  } yield (transactionMessageLink.obpTransactionRequestId, createdObpTransactionId))
                    .recoverWith {
                      case e: Exception =>
                        log.error(e.getMessage)
                        for {
                          obpTransactionId <- saveReturnedHistoricalTransaction(transaction._1)
                        } yield (None, obpTransactionId)
                    }
                // In the other cases, we just call the OBP-API to save the transaction
                case _ =>
                  for {
                    obpTransactionId <- saveReturnedHistoricalTransaction(transaction._1)
                  } yield (None, obpTransactionId)
              }).map(res => (for {
                _ <- transaction._1.updateMessageLink(paymentReturnMessage.message.id, transaction._2, res._1, Some(res._2))
                _ <- transaction._1.copy(status = SepaCreditTransferTransactionStatus.RETURNED).update()
              } yield ()).recoverWith {
                case e: Exception => log.error(e.getMessage)
                  transaction._1.copy(status = SepaCreditTransferTransactionStatus.RETURN_ERROR).update()
              }))
            )
            _ <- paymentReturnMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          } yield ()
        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }

    case ProcessIncomingPaymentRejectMessage(xmlFile, sepaFile) =>
      PaymentRejectMessage.fromXML(xmlFile, sepaFile.id) match {
        // If we receive a reject message
        case Success(paymentRejectMessage) =>
          for {
            // we save the message and get the original transaction
            validRejectedTransactions <- preProcessSctMessageTransactions(paymentRejectMessage)
            _ <- Future.sequence(validRejectedTransactions.map(_._1.copy(status = SepaCreditTransferTransactionStatus.REJECTED).update()))
            // we save the rejected transactions in OBP
            _ <- Future.sequence(validRejectedTransactions.map(transaction =>
              saveReturnedHistoricalTransaction(transaction._1).map(obpTransactionId =>
                transaction._1.updateMessageLink(paymentRejectMessage.message.id, transaction._2, None, Some(obpTransactionId))
              ).recoverWith {
                case e: Exception =>
                  log.error(e.getMessage)
                  transaction._1.copy(status = SepaCreditTransferTransactionStatus.REJECT_ERROR).update()
              }))
            _ <- paymentRejectMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          } yield ()

        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield exception
      }

    case ProcessIncomingPaymentRecallMessage(xmlFile: Elem, sepaFile: SepaFile) =>
      PaymentRecallMessage.fromXML(xmlFile, sepaFile.id) match {
        // If we receive a recall message
        case Success(paymentRecallMessage) =>
          for {
            // we save the message and get the original transaction reference
            validRecalledTransactions <- preProcessSctMessageTransactions(paymentRecallMessage)
            _ <- Future.sequence(validRecalledTransactions.map(_._1.copy(status = SepaCreditTransferTransactionStatus.RECALLED).update()))
            _ <- Future.sequence(validRecalledTransactions.map(transaction => {
              for {
                // we get the original transaction message
                originalSepaMessage <- SepaMessage.getBySepaCreditTransferTransactionId(transaction._1.id)
                  .map(_.find(_.messageType == SepaMessageType.B2B_CREDIT_TRANSFER))
                // we get the transaction-message link
                transactionMessageLink <- originalSepaMessage match {
                  case Some(creditTransferMesage) =>
                    SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(
                      transaction._1.id, creditTransferMesage.id)
                  case None => Future.failed(new Exception(s"Original sepa credit transfer message linked with sepaCreditTransferTransactionId ${transaction._1.id} not found"))
                }

                // we get the accountId by IBAN
                account <- ObpApi.getAccountByIban(Some(Adapter.BANK_ID), transaction._1.creditorAccount.getOrElse(Iban("")))
                accountId = AccountId(account.id)
                // We get the counterparty that sended the recall
                counterparty <- ObpApi.getOrCreateCounterparty(
                  bankId = Adapter.BANK_ID,
                  accountId = accountId,
                  name = transaction._1.debtor.flatMap(_.name).getOrElse(""),
                  iban = transaction._1.debtorAccount.get,
                  bic = transaction._1.debtorAgent.get
                )
                refundDescription = transaction._1.customFields.flatMap(json =>
                  (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ADDITIONAL_INFORMATION.toString)
                    .headOption.flatMap(_.asString)).orElse(transaction._1.description).getOrElse("")
                reasonCode = transaction._1.customFields.flatMap(json =>
                  (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_REASON_CODE.toString).headOption.flatMap(_.asString)).getOrElse("")

                // we create the transaction request REFUND
                transactionRequest <- ObpApi.createRefundTransactionRequest(
                  bankId = Adapter.BANK_ID,
                  accountId = accountId,
                  from = Some(TransactionRequestRefundFrom(counterparty_id = counterparty.counterparty_id)),
                  to = None,
                  refundAmount = transaction._1.amount,
                  refundDescription = refundDescription,
                  originalObpTransactionId = transactionMessageLink.obpTransactionId.get,
                  reasonCode = reasonCode
                )
                transactionRequestId = TransactionRequestId(transactionRequest.id)
                // we update the message link with the recall message by adding the transactionRequestId
                _ <- transaction._1.updateMessageLink(paymentRecallMessage.message.id, transaction._2, Some(transactionRequestId), None)
              } yield ()
            }))
            // we finish by updating the recall message and file status
            _ <- paymentRecallMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          } yield ()
        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }

    case ProcessIncomingPaymentRecallNegativeAnswerMessage(xmlFile: Elem, sepaFile: SepaFile) =>
      PaymentRecallNegativeAnswerMessage.fromXML(xmlFile, sepaFile.id) match {
        // If we receive a recall negative answer message
        case Success(paymentRecallNegativeAnswerMessage) =>
          for {
            // we save the message and get the original transaction
            validRecallNegativeAnswerTransactions <- preProcessSctMessageTransactions(paymentRecallNegativeAnswerMessage)
            _ <- Future.sequence(validRecallNegativeAnswerTransactions.map(_._1.copy(status = SepaCreditTransferTransactionStatus.RECALL_REJECTED).update()))
            _ <- Future.sequence(validRecallNegativeAnswerTransactions.map(transaction => {
              for {
                // we get the original recall message
                originalRecallSepaMessage <- SepaMessage.getBySepaCreditTransferTransactionId(transaction._1.id)
                  .map(_.find(_.messageType == SepaMessageType.B2B_PAYMENT_RECALL))
                // we get the transaction message link
                transactionRecallMessageLink <- originalRecallSepaMessage match {
                  case Some(creditTransferMesage) =>
                    SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(
                      transaction._1.id, creditTransferMesage.id)
                  case None => Future.failed(new Exception(s"Original sepa recall message linked with sepaCreditTransferTransactionId ${transaction._1.id} not found"))
                }
                // we get the transaction request id
                recallTransactionRequestId = transactionRecallMessageLink.obpTransactionRequestId.orNull
                // we get the account id by IBAN
                account <- ObpApi.getAccountByIban(Some(Adapter.BANK_ID), transaction._1.debtorAccount.orNull)
                accountId = AccountId(account.id)
                // we get the transaction request challenge id
                transactionRequestChallengeId <- ObpApi.getTransactionRequestChallengeId(Adapter.BANK_ID, accountId, Adapter.VIEW_ID, recallTransactionRequestId)
                recallRejectReasoninformation = transaction._1.customFields.flatMap(json =>
                  (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION.toString)
                    .headOption.flatMap(_.asArray).flatMap(_.headOption))
                // we create the answer transaction request (REJECT) body
                transactionRequestChallengeAnswer = TransactionRequestChallengeAnswer(
                  id = transactionRequestChallengeId,
                  answer = "REJECT",
                  reason_code = recallRejectReasoninformation.flatMap(j =>
                    (j \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION_REASON_CODE.toString)
                      .headOption.flatMap(_.asString)),
                  additional_information = recallRejectReasoninformation.flatMap(j =>
                    (j \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION_ADDITIONAL_INFORMATION.toString)
                      .headOption.flatMap(_.asString))
                )
                // we reject the transaction request REFUND by answering the challenge
                _ <- ObpApi.answerTransactionRequestChallenge(Adapter.BANK_ID, accountId, Adapter.VIEW_ID, recallTransactionRequestId, transactionRequestChallengeAnswer)
                _ <- transaction._1.updateMessageLink(paymentRecallNegativeAnswerMessage.message.id, transaction._2, Some(recallTransactionRequestId), None)
              } yield ()
            }
            ))
            _ <- paymentRecallNegativeAnswerMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          } yield ()

        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }

    case ProcessIncomingInquiryClaimNonReceiptMessage(xmlFile: Elem, sepaFile: SepaFile) =>
      InquiryClaimNonReceiptMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(inquiryClaimNonReceiptMessage) =>
          for {
            claimNonReceiptTransaction <- preProcessSctMessageTransactions(inquiryClaimNonReceiptMessage).map(_.head)
            _ <- claimNonReceiptTransaction._1.copy(status = SepaCreditTransferTransactionStatus.CLAIMED_NON_RECEIPT).update()
            // TODO : Add the logic for the reception of a claim non receipt message
            _ <- inquiryClaimNonReceiptMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          } yield ()

        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }

    case ProcessIncomingInquiryClaimValueDateCorrectionMessage(xmlFile: Elem, sepaFile: SepaFile) =>
      InquiryClaimValueDateCorrectionMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(inquiryClaimValueDateCorrectionMessage) =>
          for {
            claimValueDateCorrectionTransaction <- preProcessSctMessageTransactions(inquiryClaimValueDateCorrectionMessage).map(_.head)
            _ <- claimValueDateCorrectionTransaction._1.copy(status = SepaCreditTransferTransactionStatus.CLAIMED_VALUE_DATE_CORRECTION).update()
            // TODO : Add the logic for the reception of a claim value date correction message
            _ <- inquiryClaimValueDateCorrectionMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          } yield ()

        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }

    case ProcessIncomingInquiryClaimNonReceiptPositiveAnswerMessage(xmlFile: Elem, sepaFile: SepaFile) =>
      InquiryClaimNonReceiptPositiveAnswerMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(inquiryClaimNonReceiptPositiveAnswerMessage) =>
          for {
            claimNonReceiptPositiveAnswerTransaction <- preProcessSctMessageTransactions(inquiryClaimNonReceiptPositiveAnswerMessage).map(_.head)
            _ <- claimNonReceiptPositiveAnswerTransaction._1.copy(status = SepaCreditTransferTransactionStatus.CLAIM_NON_RECEIPT_ACCEPTED).update()
            // TODO : Add the logic for the reception of a claim non receipt positive answer message
            _ <- inquiryClaimNonReceiptPositiveAnswerMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          } yield ()

        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }

    case ProcessIncomingInquiryClaimNonReceiptNegativeAnswerMessage(xmlFile: Elem, sepaFile: SepaFile) =>
      InquiryClaimNonReceiptNegativeAnswerMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(inquiryClaimNonReceiptNegativeAnswerMessage) =>
          for {
            claimNonReceiptNegativeAnswerTransaction <- preProcessSctMessageTransactions(inquiryClaimNonReceiptNegativeAnswerMessage).map(_.head)
            _ <- claimNonReceiptNegativeAnswerTransaction._1.copy(status = SepaCreditTransferTransactionStatus.CLAIM_NON_RECEIPT_REJECTED).update()
            // TODO : Add the logic for the reception of a claim non receipt negative answer message
            _ <- inquiryClaimNonReceiptNegativeAnswerMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          } yield ()

        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }

    case ProcessIncomingInquiryClaimValueDateCorrectionPositiveAnswerMessage(xmlFile: Elem, sepaFile: SepaFile) =>
      InquiryClaimValueDateCorrectionPositiveAnswerMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(inquiryClaimValueDateCorrectionPositiveAnswerMessage) =>
          for {
            claimValueDateCorrectionPositiveAnswerTransaction <- preProcessSctMessageTransactions(inquiryClaimValueDateCorrectionPositiveAnswerMessage).map(_.head)
            _ <- claimValueDateCorrectionPositiveAnswerTransaction._1.copy(status = SepaCreditTransferTransactionStatus.CLAIM_VALUE_DATE_CORRECTION_ACCEPTED).update()
            // TODO : Add the logic for the reception of a claim value date correction positive answer message
            _ <- inquiryClaimValueDateCorrectionPositiveAnswerMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          } yield ()

        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }

    case ProcessIncomingInquiryClaimValueDateCorrectionNegativeAnswerMessage(xmlFile: Elem, sepaFile: SepaFile) =>
      InquiryClaimValueDateCorrectionNegativeAnswerMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(inquiryClaimValueDateCorrectionNegativeAnswerMessage) =>
          for {
            claimValueDateCorrectionNegativeAnswerTransaction <- preProcessSctMessageTransactions(inquiryClaimValueDateCorrectionNegativeAnswerMessage).map(_.head)
            _ <- claimValueDateCorrectionNegativeAnswerTransaction._1.copy(status = SepaCreditTransferTransactionStatus.CLAIM_VALUE_DATE_CORRECTION_REJECTED).update()
            // TODO : Add the logic for the reception of a claim value date correction negative answer message
            _ <- inquiryClaimValueDateCorrectionNegativeAnswerMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          } yield ()

        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }

    case ProcessIncomingRequestStatusUpdateMessage(xmlFile: Elem, sepaFile: SepaFile) =>
      RequestStatusUpdateMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(requestStatusUpdateMessage) =>
          for {
            requestStatusUpdateTransactions <- preProcessSctMessageTransactions(requestStatusUpdateMessage)
            _ <- Future.sequence(requestStatusUpdateTransactions.map(_._1.copy(status = SepaCreditTransferTransactionStatus.REQUESTED_STATUS_UPDATE).update()))
            // TODO : Add the logic for the reception of a request status update message
            _ <- requestStatusUpdateMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          } yield ()

        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }


    case _ => sys.error(s"Message received but not implemented yet")

  }

  def saveReturnedHistoricalTransaction(transaction: SepaCreditTransferTransaction): Future[TransactionId] = {
    for {
      originalCreditTransferMessage <- SepaMessage.getBySepaCreditTransferTransactionId(transaction.id).map(_.find(_.messageType == SepaMessageType.B2B_CREDIT_TRANSFER))
      originalObpTransactionId <- originalCreditTransferMessage match {
        case Some(creditTransferMessage) =>
          SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(transaction.id, creditTransferMessage.id)
            .map(_.obpTransactionId).fallbackTo(Future.successful(None))
        case None => Future.successful(None)
      }
      returnReasonCode = transaction.customFields.flatMap(json =>
        (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_REASON_CODE.toString).headOption.flatMap(_.asString))

      creditor <- ObpApi.getAccountByIban(
        bankId = Some(Adapter.BANK_ID),
        iban = Iban(transaction.debtorAccount.map(_.iban).getOrElse(""))
      ).map(account =>
        HistoricalTransactionAccountJsonV310(
          bank_id = Some(Adapter.BANK_ID.value),
          account_id = Some(account.id),
          counterparty_id = None
        )
      )

      debtor <- ObpApi.getOrCreateCounterparty(
        bankId = Adapter.BANK_ID,
        accountId = AccountId(creditor.account_id.get),
        name = transaction.debtor.flatMap(_.name).getOrElse(""),
        iban = Iban(transaction.debtorAccount.map(_.iban).getOrElse("")),
        bic = Bic(transaction.debtorAgent.map(_.bic).getOrElse(""))
      ).map(counterparty =>
        HistoricalTransactionAccountJsonV310(
          bank_id = None,
          account_id = None,
          counterparty_id = Some(counterparty.counterparty_id)
        )
      )

      returnedObpTransaction <- ObpApi.saveHistoricalTransaction(
        debtor = debtor,
        creditor = creditor,
        amount = transaction.amount,
        description = s"Refund for transaction_id: (${originalObpTransactionId.map(_.value).getOrElse("")}) from ${transaction.creditor.flatMap(_.name).getOrElse("")} - Reason code : ${returnReasonCode.getOrElse("")}"
      )

      returnedObpTransactionId = TransactionId(returnedObpTransaction.transaction_id)

    } yield returnedObpTransactionId
  }

  /** This method pre-process messages by saving message and getting the original transaction reference if there is one
   *
   */
  def preProcessSctMessageTransactions(sctMessage: SctMessage[_]): Future[Seq[(SepaCreditTransferTransaction, String)]] = {
    for {
      _ <- checkMessageIdInSepaFileNotExist(sctMessage.message.messageIdInSepaFile)
      _ <- sctMessage.message.insert()
      validTransactions <- Future.sequence(sctMessage.creditTransferTransactions.map(receivedTransaction =>
        for {
          _ <- sctMessage.message.messageType match {
            case B2B_INQUIRY_CLAIM_NON_RECEIP_POSITIVE_RESPONSE | B2B_INQUIRY_CLAIM_NON_RECEIP_NEGATIVE_RESPONSE |
                 B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION_POSITIVE_RESPONSE | B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION_NEGATIVE_RESPONSE =>
              Future.successful()
            case _ => checkTransactionStatusIdInSepaFileNotExist(receivedTransaction._2)
          }
          validTransaction <- sctMessage.message.messageType match {
            case SepaMessageType.B2B_CREDIT_TRANSFER => for {
              _ <- receivedTransaction._1.insert()
            } yield (receivedTransaction._1, receivedTransaction._2)
            case _ => SepaCreditTransferTransaction.getByTransactionIdInSepaFile(receivedTransaction._1.transactionIdInSepaFile)
              .flatMap { sepaCreditTransferTransaction =>
                val updatedTransaction = sepaCreditTransferTransaction.copy(
                  customFields = receivedTransaction._1.customFields.map(transactionJson =>
                    sepaCreditTransferTransaction.customFields.getOrElse(Json.fromJsonObject(JsonObject.empty))
                      .deepMerge(transactionJson)))
                for {
                  _ <- updatedTransaction.update()
                } yield (updatedTransaction, receivedTransaction._2)
              }.recoverWith {
              case exception: Exception =>
                log.warning(exception.getMessage)
                log.warning(s"Received transaction with OriginalTransactionIdInSepaFile (${receivedTransaction._1.transactionIdInSepaFile}) received. But original transaction not found in the database. Transaction integrated with id (${receivedTransaction._1.id})")
                for {
                  _ <- receivedTransaction._1.insert()
                } yield (receivedTransaction._1, receivedTransaction._2)
            }
          }
        } yield validTransaction
      ))
      _ <- Future.sequence(validTransactions.map(transaction =>
        transaction._1.linkMessage(sctMessage.message.id, transaction._2, None, None)))
    } yield validTransactions
  }

  def checkMessageIdInSepaFileNotExist(sepaMessageIdInSepaFile: String): Future[Unit] = {
    SepaMessage.getByMessageIdInSepaFile(sepaMessageIdInSepaFile).flatMap(existingMessage =>
      Future.failed(new Exception(s"SepaMessage with idInSepaFile ($sepaMessageIdInSepaFile) already exist in database with the SepaMessageId (${existingMessage.id})"))
    ).fallbackTo(Future.successful(Unit))
  }

  def checkTransactionStatusIdInSepaFileNotExist(transactionStatusIdInSepaFile: String): Future[Unit] = {
    SepaCreditTransferTransaction.getByTransactionStatusIdInSepaFile(transactionStatusIdInSepaFile).flatMap(existingTransaction =>
      Future.failed(new Exception(s"SepaCreditTransferTransaction with transactionStatusIdInSepaFile ($transactionStatusIdInSepaFile) already exist in database with the SepaCreditTransferTransactionId (${existingTransaction.id})"))
    ).fallbackTo(Future.successful(Unit))
  }

}
