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
import io.circe.parser
import io.circe.syntax._
import model.enums.{SepaCreditTransferTransactionStatus, SepaFileStatus, SepaMessageStatus}
import model.{SepaCreditTransferTransaction, SepaFile}
import sepa.CreditTransferMessage

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

case class ProcessIncomingCreditTransferMessage(sepaFile: SepaFile, xmlFile: Elem)

class ProcessIncomingFileActor extends Actor with ActorLogging {

  def receive: Receive = {

    case ProcessIncomingCreditTransferMessage(sepaFile, xmlFile) =>
      val result = CreditTransferMessage.fromXML(xmlFile, UUID.randomUUID(), sepaFile.id) match {
        case Success(creditTransferMessage) =>
          for {
            _ <- creditTransferMessage.message.insert()
            _ <- Future.sequence(creditTransferMessage.creditTransferTransactions.map(_.insert()))
            _ <- Future.sequence(creditTransferMessage.creditTransferTransactions.map(transaction =>
              transaction.linkMessage(creditTransferMessage.message.id, transaction.transactionIdInSepaFile, None, None)))
            integratedTransactions <- Future.sequence(creditTransferMessage.creditTransferTransactions.map(transaction =>
              saveHistoricalTransactionFromCounterparty(transaction).flatMap(obpTransactionId =>
                for {
                  _ <- transaction.updateMessageLink(creditTransferMessage.message.id, transaction.transactionIdInSepaFile, None, Some(obpTransactionId))
                  _ <- transaction.copy(status = SepaCreditTransferTransactionStatus.PROCESSED).update()
                } yield ()
              ).fallbackTo(transaction.copy(status = SepaCreditTransferTransactionStatus.PROCESSING_ERROR).update().failed)
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

    case _ => sys.error(s"Message received but not implemented yet")

  }


  def saveHistoricalTransactionFromCounterparty(creditTransferTransaction: SepaCreditTransferTransaction): Future[UUID] = {

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
          description = creditTransferTransaction.descripton.getOrElse(""),
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
          parser.parse(response).toOption.flatMap(jsonResult => {
            (jsonResult \\ "transaction_id").headOption.flatMap(_.asString)
          }) match {
            case Some(obpTransactionIdString) => Try(UUID.fromString(obpTransactionIdString)) match {
              case Success(obpTransactionId) => Future.successful(obpTransactionId)
              case Failure(exception) => Future.failed(exception)
            }
            case None => Future.failed(new Throwable(s"Error during Json parsing: $response"))
          }
        })
      }
    }

  }
}
