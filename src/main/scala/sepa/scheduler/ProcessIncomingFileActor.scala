package sepa.scheduler


import java.time.{LocalDateTime, ZoneId, ZoneOffset}

import adapter.obpApiModel._
import adapter.{Adapter, ObpAccountNotFoundException}
import akka.actor.{Actor, ActorLogging}
import com.openbankproject.commons.model.{AmountOfMoney, Iban, TransactionId, TransactionRequestId}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Json, JsonObject}
import model.enums._
import model.enums.sepaReasonCodes.PaymentReturnReasonCode
import model.{SepaCreditTransferTransaction, SepaFile, SepaMessage, SepaTransactionMessage}
import sepa._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

case class ProcessIncomingCreditTransferMessage(xmlFile: Elem, sepaFile: SepaFile)

case class ProcessIncomingPaymentReturnMessage(xmlFile: Elem, sepaFile: SepaFile)

case class ProcessIncomingPaymentRejectMessage(xmlFile: Elem, sepaFile: SepaFile)

case class ProcessIncomingPaymentRecallMessage(xmlFile: Elem, sepaFile: SepaFile)

case class ProcessIncomingPaymentRecallNegativeAnswerMessage(xmlFile: Elem, sepaFile: SepaFile)

class ProcessIncomingFileActor extends Actor with ActorLogging {

  def receive: Receive = {

    case ProcessIncomingCreditTransferMessage(xmlFile, sepaFile) =>
      // TODO : Check if the message already exist in database to avoid duplication
      val result = CreditTransferMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(creditTransferMessage) =>
          for {
            _ <- creditTransferMessage.message.insert()
            _ <- Future.sequence(creditTransferMessage.creditTransferTransactions.map(_._1.insert()))
            _ <- Future.sequence(creditTransferMessage.creditTransferTransactions.map(transaction =>
              transaction._1.linkMessage(creditTransferMessage.message.id, transaction._1.transactionIdInSepaFile, None, None)))
            integratedTransactions <- Future.sequence(creditTransferMessage.creditTransferTransactions.map(transaction => {
              val historicalTransactionJson = HistoricalTransactionJson(
                from = CounterpartyAccountReference(
                  counterparty_iban = transaction._1.debtorAccount.map(_.iban).getOrElse(""),
                  bank_bic = transaction._1.debtorAgent.map(_.bic),
                  counterparty_name = transaction._1.debtorName
                ).asJson,
                to = CustomerAccountReference(
                  account_iban = transaction._1.creditorAccount.map(_.iban).getOrElse(""),
                  bank_bic = transaction._1.creditorAgent.map(_.bic)
                ).asJson,
                value = AmountOfMoney(
                  currency = "EUR",
                  amount = transaction._1.amount.toString
                ).asJson,
                description = transaction._1.description.getOrElse(""),
                posted = transaction._1.creationDateTime.atZone(ZoneId.of("Europe/Paris")).withZoneSameInstant(ZoneOffset.UTC).format(HistoricalTransactionJson.jsonDateTimeFormatter),
                completed = transaction._1.creationDateTime.atZone(ZoneId.of("Europe/Paris")).withZoneSameInstant(ZoneOffset.UTC).format(HistoricalTransactionJson.jsonDateTimeFormatter),
                `type` = "SEPA",
                charge_policy = "SHARED"
              )
              ObpApi.saveHistoricalTransaction(historicalTransactionJson).flatMap(obpTransactionId =>
                for {
                  _ <- transaction._1.updateMessageLink(creditTransferMessage.message.id, transaction._1.transactionIdInSepaFile, None, Some(obpTransactionId))
                  _ <- transaction._1.copy(status = SepaCreditTransferTransactionStatus.PROCESSED).update()
                } yield ()
              ).recoverWith {
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
            _ <- creditTransferMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
            _ <- Future(log.info(s"${integratedTransactions.length} integrated transactions from file ${sepaFile.name}"))
          } yield integratedTransactions

        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield exception
      }
      result.onComplete {
        case Success(_) =>
        case Failure(exception) =>
          log.error(exception, exception.getMessage)
      }

    case ProcessIncomingPaymentReturnMessage(xmlFile, sepaFile) =>
      PaymentReturnMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(paymentReturnMessage) =>
          SepaMessage.getByMessageIdInSepaFile(paymentReturnMessage.message.messageIdInSepaFile).map {
            case Some(alreadyExistingReturnMessage) =>
              log.error(s"PaymentReturnMessage with idInSepaFile (${paymentReturnMessage.message.messageIdInSepaFile}) already exist in database with the messageId (${alreadyExistingReturnMessage.id}). File ${sepaFile.id} not integrated")
              sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
            case None =>
              for {
                _ <- paymentReturnMessage.message.insert()
                validReturnedTransactions <- Future.sequence(paymentReturnMessage.creditTransferTransactions.map(returnedTransaction =>
                  SepaCreditTransferTransaction.getByTransactionStatusIdInSepaFile(returnedTransaction._2) flatMap {
                    case Some(alreadyReturnedTransaction) =>
                      log.error(s"Transaction with sepaReturnId (${returnedTransaction._2}) in message ${paymentReturnMessage.message.id} already exist in database with the transactionId (${alreadyReturnedTransaction.id}). Returned Transaction ${returnedTransaction} not integrated")
                      Future(None)
                    case None =>
                      SepaCreditTransferTransaction.getByTransactionIdInSepaFile(returnedTransaction._1.transactionIdInSepaFile).flatMap {
                        case Some(sepaCreditTransferTransaction) =>
                          val updatedTransaction = sepaCreditTransferTransaction.copy(
                            status = SepaCreditTransferTransactionStatus.TO_RETURN,
                            customFields = returnedTransaction._1.customFields.map(returnedTransactionJson =>
                              sepaCreditTransferTransaction.customFields.getOrElse(Json.fromJsonObject(JsonObject.empty))
                                .deepMerge(returnedTransactionJson)))
                          for {
                            _ <- updatedTransaction.update()
                          } yield Some((updatedTransaction, returnedTransaction._2))
                        case None =>
                          log.warning(s"Returned transaction with OriginalTransactionIdInSepaFile (${returnedTransaction._1.transactionIdInSepaFile}) received. But original transaction not found in the database. Transaction integrated with id (${returnedTransaction._1.id})")
                          for {
                            _ <- returnedTransaction._1.insert()
                          } yield Some((returnedTransaction._1, returnedTransaction._2))
                      }
                  }
                ))
                _ <- Future.sequence(validReturnedTransactions.flatMap(_.map(transaction =>
                  transaction._1.linkMessage(paymentReturnMessage.message.id, transaction._2, None, None))))
                _ <- Future.sequence(validReturnedTransactions.flatMap(_.map(transaction =>
                  (transaction._1.customFields.flatMap(json =>
                    (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_REASON_CODE.toString).headOption.flatMap(_.asString)
                      .flatMap(reasonCode => Try(PaymentReturnReasonCode.withName(reasonCode)).toOption)) match {
                    case Some(PaymentReturnReasonCode.FOLLOWING_CANCELLATION_REQUEST) =>
                      (for {
                        recallMessage <- SepaMessage.getBySepaCreditTransferTransactionId(transaction._1.id)
                          .map(_.find(_.messageType == SepaMessageType.B2B_PAYMENT_RECALL))
                        transactionMessageLink <- recallMessage match {
                          case Some(message) => SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(transaction._1.id, message.id)
                          case None => Future.failed(new Exception(s"Original recall message of returned transaction ${transaction._1.id} not found"))
                        }
                        (obpTransactionRequestId, obpTransactionId) <- transactionMessageLink match {
                          case Some(transactionMessage) =>
                            for {
                              accountId <- ObpApi.getAccountIdByIban(Adapter.BANK_ID, Adapter.VIEW_ID, transaction._1.debtorAccount.getOrElse(Iban("")))
                              challengeId <- ObpApi.getTransactionRequestChallengeId(Adapter.BANK_ID, accountId, Adapter.VIEW_ID,
                                TransactionRequestId(transactionMessage.obpTransactionRequestId.map(_.toString).getOrElse("")))
                              transactionRequestChallengeAnswer = TransactionRequestChallengeAnswer(challengeId, "123")
                              createdObpTransactionId <- ObpApi.answerTransactionRequestChallenge(Adapter.BANK_ID, accountId, Adapter.VIEW_ID,
                                TransactionRequestId(transactionMessage.obpTransactionRequestId.map(_.toString).getOrElse("")),
                                transactionRequestChallengeAnswer).flatMap {
                                case Some(obpTransactionId) => Future.successful(obpTransactionId)
                                case None => Future.failed(new Exception(s"ObpTransactionId is missing after challenge $challengeId completed"))
                              }
                            } yield (transactionMessage.obpTransactionRequestId, createdObpTransactionId)
                          case None => Future.failed(new Exception(s"TransactionMessage link between SepaCreditTransferTransactionId ${transaction._1.id} and SepaMessageId ${recallMessage.map(_.id)} not found"))
                        }
                      } yield (obpTransactionRequestId, obpTransactionId))
                        .recoverWith {
                          case e: Exception =>
                            log.error(e.getMessage)
                            for {
                              obpTransactionId <- saveReturnedHistoricalTransaction(transaction._1)
                            } yield (None, obpTransactionId)
                        }
                    case _ =>
                      for {
                        obpTransactionId <- saveReturnedHistoricalTransaction(transaction._1)
                      } yield (None, obpTransactionId)
                  }).map(res => (for {
                    _ <- transaction._1.updateMessageLink(paymentReturnMessage.message.id, transaction._2, res._1, Some(res._2))
                    _ <- transaction._1.copy(status = SepaCreditTransferTransactionStatus.RETURNED).update()
                  } yield ()).recoverWith {
                    case e: Exception =>
                      log.error(e.getMessage)
                      transaction._1.copy(status = SepaCreditTransferTransactionStatus.RETURN_ERROR).update()
                  }))
                ))
                _ <- paymentReturnMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
                _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
              } yield ()
          }
        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }

    case ProcessIncomingPaymentRejectMessage(xmlFile, sepaFile) =>
      PaymentRejectMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(paymentRejectMessage) =>
          SepaMessage.getByMessageIdInSepaFile(paymentRejectMessage.message.messageIdInSepaFile).map {
            case Some(alreadyExistingRejectMessage) =>
              log.error(s"PaymentRejectMessage with idInSepaFile (${paymentRejectMessage.message.messageIdInSepaFile}) already exist in database with the messageId (${alreadyExistingRejectMessage.id}). File ${sepaFile.id} not integrated")
              sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
            case None =>
              for {
                _ <- paymentRejectMessage.message.insert()
                validRejectedTransactions <- Future.sequence(paymentRejectMessage.creditTransferTransactions.map(rejectedTransaction =>
                  SepaCreditTransferTransaction.getByTransactionStatusIdInSepaFile(rejectedTransaction._2) flatMap {
                    case Some(alreadyRejectedTransaction) =>
                      log.error(s"Transaction with sepaRejectId (${rejectedTransaction._2}) in message ${paymentRejectMessage.message.id} already exist in database with the transactionId (${alreadyRejectedTransaction.id}). Returned Transaction ${rejectedTransaction} not integrated")
                      Future(None)
                    case None =>
                      SepaCreditTransferTransaction.getByTransactionIdInSepaFile(rejectedTransaction._1.transactionIdInSepaFile).flatMap {
                        case Some(sepaCreditTransferTransaction) =>
                          val updatedTransaction = sepaCreditTransferTransaction.copy(
                            status = SepaCreditTransferTransactionStatus.REJECTED,
                            customFields = rejectedTransaction._1.customFields.map(rejectedTransactionJson =>
                              sepaCreditTransferTransaction.customFields.getOrElse(Json.fromJsonObject(JsonObject.empty))
                                .deepMerge(rejectedTransactionJson)))
                          for {
                            _ <- updatedTransaction.update()
                          } yield Some((updatedTransaction, rejectedTransaction._2))
                        case None =>
                          log.warning(s"Rejected transaction with OriginalTransactionIdInSepaFile (${rejectedTransaction._1.transactionIdInSepaFile}) received. But original transaction not found in the database. Transaction integrated with id (${rejectedTransaction._1.id})")
                          for {
                            _ <- rejectedTransaction._1.insert()
                          } yield Some((rejectedTransaction._1, rejectedTransaction._2))
                      }
                  }
                ))
                _ <- Future.sequence(validRejectedTransactions.flatMap(_.map(transaction =>
                  transaction._1.linkMessage(paymentRejectMessage.message.id, transaction._2, None, None))))
                _ <- Future.sequence(validRejectedTransactions.flatMap(_.map(transaction =>
                  saveReturnedHistoricalTransaction(transaction._1).map(obpTransactionId =>
                    transaction._1.updateMessageLink(paymentRejectMessage.message.id, transaction._2, None, Some(obpTransactionId))
                  ).recoverWith {
                    case e: Exception =>
                      log.error(e.getMessage)
                      transaction._1.copy(status = SepaCreditTransferTransactionStatus.REJECT_ERROR).update()
                  })))
                _ <- paymentRejectMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
                _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
              } yield ()
          }
        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield exception
      }

    case ProcessIncomingPaymentRecallMessage(xmlFile: Elem, sepaFile: SepaFile) =>
      PaymentRecallMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(paymentRecallMessage) =>
          SepaMessage.getByMessageIdInSepaFile(paymentRecallMessage.message.messageIdInSepaFile).map {
            case Some(alreadyExistingRecallMessage) =>
              log.error(s"PaymentRecallMessage with idInSepaFile (${paymentRecallMessage.message.messageIdInSepaFile}) already exist in database with the messageId (${alreadyExistingRecallMessage.id}). File ${sepaFile.id} not integrated")
              sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
            case None =>
              for {
                _ <- paymentRecallMessage.message.insert()
                validRecalledTransactions <- Future.sequence(paymentRecallMessage.creditTransferTransactions.map(recalledTransaction =>
                  SepaCreditTransferTransaction.getByTransactionStatusIdInSepaFile(recalledTransaction._2) flatMap {
                    case Some(alreadyRecalledTransaction) =>
                      log.error(s"Transaction with sepaRecallId (${recalledTransaction._2}) in message ${paymentRecallMessage.message.id} already exist in database with the transactionId (${alreadyRecalledTransaction.id}). Recalled Transaction ${recalledTransaction} not integrated")
                      Future(None)
                    case None =>
                      SepaCreditTransferTransaction.getByTransactionIdInSepaFile(recalledTransaction._1.transactionIdInSepaFile).flatMap {
                        case Some(sepaCreditTransferTransaction) =>
                          val updatedTransaction = sepaCreditTransferTransaction.copy(
                            status = SepaCreditTransferTransactionStatus.RECALLED,
                            customFields = recalledTransaction._1.customFields.map(recalledTransactionJson =>
                              sepaCreditTransferTransaction.customFields.getOrElse(Json.fromJsonObject(JsonObject.empty))
                                .deepMerge(recalledTransactionJson)))
                          for {
                            _ <- updatedTransaction.update()
                          } yield Some((updatedTransaction, recalledTransaction._2))
                        case None =>
                          log.warning(s"Recalled transaction with OriginalTransactionIdInSepaFile (${recalledTransaction._1.transactionIdInSepaFile}) received. But original transaction not found in the database. Transaction integrated with id (${recalledTransaction._1.id})")
                          for {
                            _ <- recalledTransaction._1.insert()
                          } yield Some((recalledTransaction._1, recalledTransaction._2))
                      }
                  }
                ))
                _ <- Future.sequence(validRecalledTransactions.flatMap(_.map(transaction =>
                  transaction._1.linkMessage(paymentRecallMessage.message.id, transaction._2, None, None))))
                _ <- Future.sequence(validRecalledTransactions.flatMap(_.map(transaction => {
                  for {
                    originalSepaMessage <- SepaMessage.getBySepaCreditTransferTransactionId(transaction._1.id)
                      .map(_.find(_.messageType == SepaMessageType.B2B_CREDIT_TRANSFER))
                    transactionMessageLink <- originalSepaMessage match {
                      case Some(creditTransferMesage) =>
                        SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(
                          transaction._1.id, creditTransferMesage.id)
                      case None => Future.failed(new Exception(s"Original sepa credit transfer message linked with sepaCreditTransferTransactionId ${transaction._1.id} not found"))
                    }
                    refundTransactionRequest = RefundTransactionRequest(
                      from = Some(RefundTransactionRequestCounterpartyIban(transaction._1.debtorAccount.map(_.iban).getOrElse("")).asJson),
                      value = AmountOfMoney(
                        currency = "EUR",
                        amount = transaction._1.amount.toString
                      ).asJson,
                      description = transaction._1.customFields.flatMap(json =>
                        (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ADDITIONAL_INFORMATION.toString)
                          .headOption.flatMap(_.asString)).orElse(transaction._1.description).getOrElse(""),
                      refund = RefundTransactionRequestContent(
                        transaction_id = transactionMessageLink.flatMap(_.obpTransactionId.map(_.value)).getOrElse(""),
                        reason_code = transaction._1.customFields.flatMap(json =>
                          (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_REASON_CODE.toString).headOption.flatMap(_.asString)).getOrElse("")
                      ).asJson
                    )
                    accountId <- ObpApi.getAccountIdByIban(Adapter.BANK_ID, Adapter.VIEW_ID, transaction._1.creditorAccount.getOrElse(Iban("")))
                    transactionRequestId <- ObpApi.createRefundTransactionRequest(Adapter.BANK_ID, accountId, Adapter.VIEW_ID, refundTransactionRequest)
                    _ <- transaction._1.updateMessageLink(paymentRecallMessage.message.id, transaction._2, Some(transactionRequestId), None)
                  } yield ()
                }
                )))
                _ <- paymentRecallMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
                _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
              } yield ()
          }
        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }

    // TODO : Refectorize this part with previous methods
    case ProcessIncomingPaymentRecallNegativeAnswerMessage(xmlFile: Elem, sepaFile: SepaFile) =>
      PaymentRecallNegativeAnswerMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(paymentRecallNegativeAnswerMessage) =>
          SepaMessage.getByMessageIdInSepaFile(paymentRecallNegativeAnswerMessage.message.messageIdInSepaFile).map {
            case Some(alreadyExistingRecallNegativeAnswerMessage) =>
              log.error(s"PaymentRecallNegativeAnswerMessage with idInSepaFile (${paymentRecallNegativeAnswerMessage.message.messageIdInSepaFile}) already exist in database with the messageId (${alreadyExistingRecallNegativeAnswerMessage.id}). File ${sepaFile.id} not integrated")
              sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
            case None =>
              for {
                _ <- paymentRecallNegativeAnswerMessage.message.insert()
                validRecallNegativeAnswerTransactions <- Future.sequence(paymentRecallNegativeAnswerMessage.creditTransferTransactions.map(recallNegativeAnswerTransaction =>
                  SepaCreditTransferTransaction.getByTransactionStatusIdInSepaFile(recallNegativeAnswerTransaction._2) flatMap {
                    case Some(alreadyRejectedRecallTransaction) =>
                      log.error(s"Transaction with sepaRecallNegativeAnswerId (${recallNegativeAnswerTransaction._2}) in message ${paymentRecallNegativeAnswerMessage.message.id} already exist in database with the transactionId (${alreadyRejectedRecallTransaction.id}). Recalled Transaction ${recallNegativeAnswerTransaction} not integrated")
                      Future(None)
                    case None =>
                      SepaCreditTransferTransaction.getByTransactionIdInSepaFile(recallNegativeAnswerTransaction._1.transactionIdInSepaFile).flatMap {
                        case Some(sepaCreditTransferTransaction) =>
                          val updatedTransaction = sepaCreditTransferTransaction.copy(
                            status = SepaCreditTransferTransactionStatus.RECALL_REJECT,
                            customFields = recallNegativeAnswerTransaction._1.customFields.map(transactionJson =>
                              sepaCreditTransferTransaction.customFields.getOrElse(Json.fromJsonObject(JsonObject.empty))
                                .deepMerge(transactionJson)))
                          for {
                            _ <- updatedTransaction.update()
                          } yield Some((updatedTransaction, recallNegativeAnswerTransaction._2))
                        case None =>
                          log.warning(s"Recall negative answer transaction with OriginalTransactionIdInSepaFile (${recallNegativeAnswerTransaction._1.transactionIdInSepaFile}) received. But original transaction not found in the database. Transaction integrated with id (${recallNegativeAnswerTransaction._1.id})")
                          for {
                            _ <- recallNegativeAnswerTransaction._1.insert()
                          } yield Some((recallNegativeAnswerTransaction._1, recallNegativeAnswerTransaction._2))
                      }
                  }
                ))
                _ <- Future.sequence(validRecallNegativeAnswerTransactions.flatMap(_.map(transaction =>
                  transaction._1.linkMessage(paymentRecallNegativeAnswerMessage.message.id, transaction._2, None, None))))
                _ <- Future.sequence(validRecallNegativeAnswerTransactions.flatMap(_.map(transaction => {
                  for {
                    originalRecallSepaMessage <- SepaMessage.getBySepaCreditTransferTransactionId(transaction._1.id)
                      .map(_.find(_.messageType == SepaMessageType.B2B_PAYMENT_RECALL))
                    transactionRecallMessageLink <- originalRecallSepaMessage match {
                      case Some(creditTransferMesage) =>
                        SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(
                          transaction._1.id, creditTransferMesage.id)
                      case None => Future.failed(new Exception(s"Original sepa recall message linked with sepaCreditTransferTransactionId ${transaction._1.id} not found"))
                    }
                    recallTransactionRequestId = transactionRecallMessageLink.flatMap(_.obpTransactionRequestId).orNull
                    accountId <- ObpApi.getAccountIdByIban(Adapter.BANK_ID, Adapter.VIEW_ID, transaction._1.debtorAccount.orNull)
                    transactionRequestChallengeId <- ObpApi.getTransactionRequestChallengeId(Adapter.BANK_ID, accountId, Adapter.VIEW_ID, recallTransactionRequestId)
                    recallRejectReasoninformation = transaction._1.customFields.flatMap(json =>
                      (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION.toString)
                        .headOption.flatMap(_.asArray).flatMap(_.headOption))
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
                    _ <- ObpApi.answerTransactionRequestChallenge(Adapter.BANK_ID, accountId, Adapter.VIEW_ID, recallTransactionRequestId, transactionRequestChallengeAnswer)
                    _ <- transaction._1.updateMessageLink(paymentRecallNegativeAnswerMessage.message.id, transaction._2, Some(recallTransactionRequestId), None)
                  } yield ()
                }
                )))
                _ <- paymentRecallNegativeAnswerMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
                _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
              } yield ()
          }
        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield log.error(exception.getMessage)
      }


    case _ => sys.error(s"Message received but not implemented yet")

  }

  def saveReturnedHistoricalTransaction(transaction: SepaCreditTransferTransaction): Future[TransactionId] = {
    val historicalTransactionJson = HistoricalTransactionJson(
      from = CounterpartyAccountReference(
        counterparty_iban = transaction.creditorAccount.map(_.iban).getOrElse(""),
        bank_bic = transaction.creditorAgent.map(_.bic),
        counterparty_name = transaction.creditorName
      ).asJson,
      to = CustomerAccountReference(
        account_iban = transaction.debtorAccount.map(_.iban).getOrElse(""),
        bank_bic = transaction.debtorAgent.map(_.bic)
      ).asJson,
      value = AmountOfMoney(
        currency = "EUR",
        amount = transaction.amount.toString
      ).asJson,
      // TODO : Try to include the originalObpTransactionId + reasonCode in the return description like in OBP
      description = s"TRANSACTION RETURNED. Original description : ${transaction.description.getOrElse("")}",
      posted = LocalDateTime.now(ZoneOffset.UTC).format(HistoricalTransactionJson.jsonDateTimeFormatter),
      completed = LocalDateTime.now(ZoneOffset.UTC).format(HistoricalTransactionJson.jsonDateTimeFormatter),
      `type` = "REFUND",
      charge_policy = "SHARED"
    )
    for {
      obpTransactionId <- ObpApi.saveHistoricalTransaction(historicalTransactionJson)
    } yield TransactionId(obpTransactionId.toString)
  }

}
