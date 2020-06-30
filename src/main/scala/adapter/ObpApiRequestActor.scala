package adapter

import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import com.openbankproject.commons.model._
import io.circe.generic.auto._
import io.circe.syntax._
import model.SepaCreditTransferTransaction

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

case class SaveHistoricalTransactionFromCounterparty(transaction: SepaCreditTransferTransaction)

class ObpApiRequestActor extends Actor with ActorLogging {

  def receive: Receive = {
    case SaveHistoricalTransactionFromCounterparty(transaction: SepaCreditTransferTransaction) => {

      val jsonDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

      val saveHistoricalTransactionResponse = Http(context.system).singleRequest(HttpRequest(
        method = HttpMethods.POST,
        uri = "http://localhost:8080/obp/v4.0.0/management/historical/transactions",
        headers = Seq(
          Authorization(GenericHttpCredentials("DirectLogin", "token=eyJhbGciOiJIUzI1NiJ9.eyIiOiIifQ.3Wlgq06imwoeSDrMYrPRmhG4v3A2qBOBvPkbKnfm0gY"))
        ),
        entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          HistoricalTransactionJson(
            from = CounterpartyAccountReference(
              counterparty_iban = transaction.debtorAccount.map(_.iban).getOrElse(""),
              bank_bic = transaction.debtorAgent.map(_.bic),
              counterparty_name = transaction.debtorName
            ).asJson,
            to = CustomerAccountReference(
              account_iban = transaction.creditorAccount.map(_.iban).getOrElse(""),
              bank_bic = transaction.creditorAgent.map(_.bic)
            ).asJson,
            value = AmountOfMoney(
              currency = "EUR",
              amount = transaction.amount.toString
            ).asJson,
            description = transaction.descripton.getOrElse(""),
            posted = transaction.creationDateTime.format(jsonDateTimeFormatter),
            completed = transaction.creationDateTime.format(jsonDateTimeFormatter),
            `type` = "SEPA",
            charge_policy = "SHARED"
          ).asJson.toString()
        )
      ))

      implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

      saveHistoricalTransactionResponse.onComplete {
        case Success(res) => res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach(body =>
          log.info(body.utf8String))
        case Failure(exception) => sys.error(exception.getMessage)
      }

    }

    case _ => log.warning(s"${context.self.path.name} : Message received but not implemented")
  }
}