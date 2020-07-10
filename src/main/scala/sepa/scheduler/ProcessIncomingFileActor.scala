package sepa.scheduler


import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import adapter.{CounterpartyAccountReference, CustomerAccountReference, HistoricalTransactionJson}
import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import com.openbankproject.commons.model.AmountOfMoney
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Json, JsonObject, parser}
import model.enums.sepaReasonCodes.PaymentReturnReasonCode
import model.enums.{SepaCreditTransferTransactionStatus, SepaFileStatus, SepaMessageStatus}
import model.{SepaCreditTransferTransaction, SepaFile, SepaMessage}
import sepa.{CreditTransferMessage, PaymentRejectMessage, PaymentReturnMessage}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

case class ProcessIncomingCreditTransferMessage(xmlFile: Elem, sepaFile: SepaFile)

case class ProcessIncomingPaymentReturnMessage(xmlFile: Elem, sepaFile: SepaFile)

case class ProcessIncomingPaymentRejectMessage(xmlFile: Elem, sepaFile: SepaFile)

class ProcessIncomingFileActor extends Actor with ActorLogging {

  def receive: Receive = {

    case ProcessIncomingCreditTransferMessage(xmlFile, sepaFile) =>
      // TODO : Check if the message already exist in database to avoid duplication
      val result = CreditTransferMessage.fromXML(xmlFile, sepaFile.id) match {
        case Success(creditTransferMessage) =>
          for {
            _ <- creditTransferMessage.message.insert()
            _ <- Future.sequence(creditTransferMessage.creditTransferTransactions.map(_.insert()))
            _ <- Future.sequence(creditTransferMessage.creditTransferTransactions.map(transaction =>
              transaction.linkMessage(creditTransferMessage.message.id, transaction.transactionIdInSepaFile, None, None)))
            integratedTransactions <- Future.sequence(creditTransferMessage.creditTransferTransactions.map(transaction =>
              saveHistoricalTransactionFromCounterparty(transaction, creditTransferMessage.message).flatMap(obpTransactionId =>
                for {
                  _ <- transaction.updateMessageLink(creditTransferMessage.message.id, transaction.transactionIdInSepaFile, None, Some(obpTransactionId))
                  _ <- transaction.copy(status = SepaCreditTransferTransactionStatus.PROCESSED).update()
                } yield ()
              )
            ))
            _ <- creditTransferMessage.message.copy(status = SepaMessageStatus.PROCESSED).update()
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
            _ <- Future(println(s"${integratedTransactions.length} integrated transactions from file ${sepaFile.name}"))
          } yield integratedTransactions

        case Failure(exception) =>
          for {
            _ <- sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update()
          } yield exception
      }
      result.onComplete {
        case Success(_) => context.system.terminate()
        case Failure(exception) =>
          log.error(exception, exception.getMessage)
          context.system.terminate()
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
                  saveReturnedHistoricalTransaction(transaction._1, "RETURN").map(obpTransactionId => {
                    for {
                      _ <- transaction._1.updateMessageLink(paymentReturnMessage.message.id, transaction._2, None, Some(obpTransactionId))
                      _ <- transaction._1.copy(status = SepaCreditTransferTransactionStatus.RETURNED).update()
                    } yield ()
                  }
                  ).recoverWith {
                    case e: Throwable =>
                      log.error(e.getMessage)
                      transaction._1.copy(status = SepaCreditTransferTransactionStatus.RETURN_ERROR).update()
                  })))
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
                  saveReturnedHistoricalTransaction(transaction._1, "REJECT").map(obpTransactionId =>
                    transaction._1.updateMessageLink(paymentRejectMessage.message.id, transaction._2, None, Some(obpTransactionId))
                  ).recoverWith {
                    case e: Throwable =>
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


    case _ => sys.error(s"Message received but not implemented yet")

  }


  // TODO : Refactorize this function with the next one
  def saveHistoricalTransactionFromCounterparty(creditTransferTransaction: SepaCreditTransferTransaction, sepaMessage: SepaMessage): Future[UUID] = {

    val jsonDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

    val saveHistoricalTransactionResponse = Http(context.system).singleRequest(HttpRequest(
      method = HttpMethods.POST,
      uri = "http://localhost:8080/obp/v4.0.0/management/historical/transactions",
      headers = Seq(
        Authorization(GenericHttpCredentials("DirectLogin", "token=eyJhbGciOiJIUzI1NiJ9.eyIiOiIifQ.CeA_QUnsF4xBScAYy3ZtK64f7uE28nHbXSFoAlodUQM"))
      ),
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        HistoricalTransactionJson(
          from = CounterpartyAccountReference(
            counterparty_iban = creditTransferTransaction.debtorAccount.map(_.iban).getOrElse(""),
            bank_bic = creditTransferTransaction.debtorAgent.map(_.bic),
            counterparty_name = creditTransferTransaction.debtorName
          ).asJson,
          to = CustomerAccountReference(
            account_iban = creditTransferTransaction.creditorAccount.map(_.iban).getOrElse(""),
            bank_bic = creditTransferTransaction.creditorAgent.map(_.bic)
          ).asJson,
          value = AmountOfMoney(
            currency = "EUR",
            amount = creditTransferTransaction.amount.toString
          ).asJson,
          description = creditTransferTransaction.description.getOrElse(""),
          posted = creditTransferTransaction.creationDateTime.format(jsonDateTimeFormatter),
          completed = creditTransferTransaction.creationDateTime.format(jsonDateTimeFormatter),
          `type` = "SEPA",
          charge_policy = "SHARED"
        ).asJson.toString()
      )
    ))

    saveHistoricalTransactionResponse.flatMap {
      res => {
        res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(body => body.utf8String).flatMap(response => {
          println(response)
          parser.parse(response).toOption match {
            case Some(jsonResult) if (jsonResult \\ "transaction_id").nonEmpty =>
              Try(UUID.fromString((jsonResult \\ "transaction_id").headOption.flatMap(_.asString).get)) match {
                case Success(obpTransactionId) => Future.successful(obpTransactionId)
                case Failure(exception) => Future.failed(exception)
              }
            case Some(jsonResult) if (jsonResult \\ "code").nonEmpty && (jsonResult \\ "message").nonEmpty =>
              val errorCode = (jsonResult \\ "code").headOption.flatMap(_.asNumber.flatMap(_.toInt))
              val errorMessage = (jsonResult \\ "message").headOption.flatMap(_.asString.flatMap(_.split(":").headOption))
              println((errorCode.getOrElse(""), errorMessage.getOrElse("")))
              (errorCode, errorMessage) match {
                case (Some(404), Some("OBP-30018")) =>
                  PaymentReturnMessage.returnTransaction(creditTransferTransaction, sepaMessage, PaymentReturnReasonCode.INCORRECT_ACCOUNT_NUMBER)
                    .flatMap(_ => Future.failed(new Throwable(s"Transaction ${creditTransferTransaction.id} returned : Account not found : ${creditTransferTransaction.creditorAccount}")))
                case _ => Future.failed(new Throwable(s"Unknow error in saveHistoricalTransactionResponse: ${errorMessage.getOrElse("")}"))
              }
            case None => Future.failed(new Throwable(s"Error during Json parsing: $response"))
          }
        })
      }
    }

  }

  def saveReturnedHistoricalTransaction(returnedCreditTransferTransaction: SepaCreditTransferTransaction, returnType: String): Future[UUID] = {

    val jsonDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

    val saveHistoricalTransactionResponse = Http(context.system).singleRequest(HttpRequest(
      method = HttpMethods.POST,
      uri = "http://localhost:8080/obp/v4.0.0/management/historical/transactions",
      headers = Seq(
        Authorization(GenericHttpCredentials("DirectLogin", "token=eyJhbGciOiJIUzI1NiJ9.eyIiOiIifQ.CeA_QUnsF4xBScAYy3ZtK64f7uE28nHbXSFoAlodUQM"))
      ),
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        HistoricalTransactionJson(
          from = CounterpartyAccountReference(
            counterparty_iban = returnedCreditTransferTransaction.creditorAccount.map(_.iban).getOrElse(""),
            bank_bic = returnedCreditTransferTransaction.creditorAgent.map(_.bic),
            counterparty_name = returnedCreditTransferTransaction.creditorName
          ).asJson,
          to = CustomerAccountReference(
            account_iban = returnedCreditTransferTransaction.debtorAccount.map(_.iban).getOrElse(""),
            bank_bic = returnedCreditTransferTransaction.debtorAgent.map(_.bic)
          ).asJson,
          value = AmountOfMoney(
            currency = "EUR",
            amount = returnedCreditTransferTransaction.amount.toString
          ).asJson,
          description = s"TRANSACTION ${returnType + "ED"}. Original description : " + returnedCreditTransferTransaction.description.getOrElse(""),
          posted = LocalDateTime.now.format(jsonDateTimeFormatter),
          completed = LocalDateTime.now.format(jsonDateTimeFormatter),
          `type` = "SEPA",
          charge_policy = "SHARED"
        ).asJson.toString()
      )
    ))

    saveHistoricalTransactionResponse.flatMap {
      res => {
        res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(body => body.utf8String).flatMap(response => {
          println(response)
          parser.parse(response).toOption match {
            case Some(jsonResult) if (jsonResult \\ "transaction_id").nonEmpty =>
              Future.fromTry(Try(UUID.fromString((jsonResult \\ "transaction_id").headOption.flatMap(_.asString).get)))
            case Some(jsonResult) if (jsonResult \\ "code").nonEmpty && (jsonResult \\ "message").nonEmpty =>
              val errorCode = (jsonResult \\ "code").headOption.flatMap(_.asNumber.flatMap(_.toInt))
              val errorMessage = (jsonResult \\ "message").headOption.flatMap(_.asString.flatMap(_.split(":").headOption))
              (errorCode, errorMessage) match {
                case (Some(404), Some("OBP-30018")) =>
                  Future.failed(new Throwable(s"Impossible to integrate returned transaction ${returnedCreditTransferTransaction.id} in OBP. Account not found : ${returnedCreditTransferTransaction.debtorAccount}"))
                case _ =>
                  Future.failed(new Throwable(s"Unknow error in saveHistoricalTransactionResponse: ${errorMessage.getOrElse("")}"))
              }
            case None => Future.failed(new Throwable(s"Error during Json parsing: $response"))
          }
        })
      }
    }

  }
}
