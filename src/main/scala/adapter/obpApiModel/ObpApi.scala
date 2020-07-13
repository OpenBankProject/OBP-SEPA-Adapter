package adapter.obpApiModel

import java.util.UUID

import adapter.ObpAccountNotFoundException
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.openbankproject.commons.model._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Json, JsonObject, parser}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

object ObpApi {

  def saveHistoricalTransaction(historicalTransactionJson: HistoricalTransactionJson): Future[UUID] = {
    val body = historicalTransactionJson.asJson.toString()
    val callResult = call("http://localhost:8080/obp/v4.0.0/management/historical/transactions", HttpMethods.POST, body)
    callResult.map(println)
    callResult.flatMap {
      case jsonResult if (jsonResult \\ "transaction_id").nonEmpty =>
        Future.fromTry(Try(UUID.fromString((jsonResult \\ "transaction_id").headOption.flatMap(_.asString).get)))
      case jsonResult if (jsonResult \\ "code").nonEmpty && (jsonResult \\ "message").nonEmpty =>
        val errorCode = (jsonResult \\ "code").headOption.flatMap(_.asNumber.flatMap(_.toInt))
        val errorMessage = (jsonResult \\ "message").headOption.flatMap(_.asString)
        (errorCode, errorMessage.flatMap(_.split(":").headOption)) match {
          case (Some(404), Some("OBP-30018")) =>
            Future.failed(new ObpAccountNotFoundException(errorMessage.getOrElse("")))
          case _ =>
            Future.failed(new Exception(s"Unknow error in saveHistoricalTransactionResponse: ${errorMessage.getOrElse("")}"))
        }
    }
  }

  private def call(uri: String, httpMethod: HttpMethod, body: String): Future[Json] = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    Http().singleRequest(HttpRequest(
      method = httpMethod,
      uri = uri,
      headers = Seq(
        Authorization(GenericHttpCredentials("DirectLogin", "token=eyJhbGciOiJIUzI1NiJ9.eyIiOiIifQ.CeA_QUnsF4xBScAYy3ZtK64f7uE28nHbXSFoAlodUQM"))
      ),
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        body
      ))
    ).flatMap(_.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(body => body.utf8String).flatMap(response =>
      Future.fromTry(parser.parse(response).toTry)))
  }

  def getAccountIdByIban(bankId: BankId, viewId: ViewId, iban: Iban): Future[AccountId] = {
    val body = JsonObject.fromMap(Map(("iban", iban.iban.asJson))).asJson.toString()
    val callResult = call(s"http://localhost:8080/obp/v4.0.0/banks/${bankId.value}/accounts/${viewId.value}/account", HttpMethods.POST, body)

    callResult.flatMap {
      case jsonResult if (jsonResult \\ "id").headOption.flatMap(_.asString).isDefined =>
        Future.successful(AccountId((jsonResult \\ "id").headOption.flatMap(_.asString).get))
      case jsonResult if (jsonResult \\ "code").nonEmpty && (jsonResult \\ "message").nonEmpty =>
        val errorCode = (jsonResult \\ "code").headOption.flatMap(_.asNumber.flatMap(_.toInt))
        val errorMessage = (jsonResult \\ "message").headOption.flatMap(_.asString)
        (errorCode, errorMessage.flatMap(_.split(":").headOption)) match {
          case (Some(404), Some("OBP-30018")) =>
            Future.failed(new ObpAccountNotFoundException(errorMessage.getOrElse("")))
          case _ => Future.failed(new Exception(s"Unknow error in getObpAccountIdByIban: ${errorMessage.getOrElse("")}"))
        }
    }
  }

  def createRefundTransactionRequest(bankId: BankId, accountId: AccountId, viewId: ViewId, refundTransactionRequest: RefundTransactionRequest): Future[TransactionRequestId] = {
    val body = refundTransactionRequest.asJson.toString()
    val callResult = call(s"http://localhost:8080/obp/v4.0.0/banks/${bankId.value}/accounts/${accountId.value}/${viewId.value}/transaction-request-types/REFUND/transaction-requests", HttpMethods.POST, body)
    callResult.map(println)
    callResult.flatMap {
      case jsonResult if (jsonResult \\ "id").headOption.flatMap(_.asString).isDefined =>
        Future.successful(TransactionRequestId((jsonResult \\ "id").headOption.flatMap(_.asString).get))
      case jsonResult if (jsonResult \\ "code").nonEmpty && (jsonResult \\ "message").nonEmpty =>
        val errorCode = (jsonResult \\ "code").headOption.flatMap(_.asNumber.flatMap(_.toInt))
        val errorMessage = (jsonResult \\ "message").headOption.flatMap(_.asString)
        (errorCode, errorMessage.flatMap(_.split(":").headOption)) match {
          case _ => Future.failed(new Exception(s"Unknow error in getObpAccountIdByIban: ${errorMessage.getOrElse("")}"))
        }
    }
  }
}