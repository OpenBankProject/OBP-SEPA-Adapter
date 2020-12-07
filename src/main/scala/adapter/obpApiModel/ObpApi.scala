package adapter.obpApiModel

import java.time.{Instant, LocalDate, LocalDateTime}
import java.util.Date

import adapter.{ObpAccountNotFoundException, ObpBankNotFoundException}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import com.openbankproject.commons.model._
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Json, parser}
import model.types.Bic

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * This object is used to make calls to the OBP-API
 */

// TODO : refactor endpoints with extract method like in `getCounterpartyByIban`
object ObpApi {

  val hostName: String = ConfigFactory.load.getString("obp-api.hostname")
  val versionRoute = "/obp/v4.0.0"
  val endpointPrefix: String = hostName + versionRoute

  implicit val decodeDate: Decoder[Date] = Decoder.decodeString.map(dateString =>
    Date.from(Instant.parse(dateString))
  )

  def saveHistoricalTransaction(debtor: HistoricalTransactionAccountJsonV310, creditor: HistoricalTransactionAccountJsonV310, amount: BigDecimal, description: String)(implicit system: ActorSystem): Future[PostHistoricalTransactionResponseJson] = {
    val dateTimeNow = LocalDateTime.now().format(PostHistoricalTransactionJson.jsonDateTimeFormatter)
    val historicalTransaction = PostHistoricalTransactionJson(
      from = debtor,
      to = creditor,
      value = AmountOfMoneyJsonV121(
        currency = "EUR",
        amount = amount.toString()
      ),
      description = description,
      posted = dateTimeNow,
      completed = dateTimeNow,
      `type` = "SEPA",
      charge_policy = "SHARED"
    )
    
    callObpApi(s"$endpointPrefix/management/historical/transactions", HttpMethods.POST, historicalTransaction.asJson.toString())
      .flatMap(json => Future.fromTry(json.as[PostHistoricalTransactionResponseJson].toTry))
  }

  def getAccountByIban(bankId: Option[BankId], iban: Iban)(implicit system: ActorSystem): Future[ModeratedAccountJSON400] = {
    val bankAccountRouting = BankAccountRoutingJson(
      bank_id = bankId.map(_.value),
      account_routing = AccountRoutingJsonV121(
        scheme = "IBAN",
        address = iban.iban
      )
    )

    callObpApi(s"$endpointPrefix/management/accounts/account-routing-query", HttpMethods.POST, bankAccountRouting.asJson.toString())
      .flatMap(json =>
        json.as[ModeratedAccountJSON400].toTry match {
          case Success(account) => Future.successful(account)
          case Failure(_) =>
            json.as[ObpApiError].toTry match {
              case Success(error) =>
                (error.code, error.message.split(":").headOption) match {
                  case (404, Some("OBP-30073")) => Future.failed(new ObpAccountNotFoundException(error.message))
                  case _ => Future.failed(new Exception(s"Unknown error in getObpAccountByIban: ${error.message}"))
                }
              case Failure(exception) =>
                Future.failed(new Exception(s"Unknown error in getAccountByIban: $json"))
            }
        }
      )
    }

  def getAccountByAccountId(bankId: BankId, accountId: AccountId)(implicit system: ActorSystem): Future[ModeratedAccountJSON400] = {
    callObpApi(s"$endpointPrefix/banks/${bankId.value}/accounts/${accountId.value}/owner/account", HttpMethods.GET)
      .flatMap(json => Future.fromTry(json.as[ModeratedAccountJSON400].toTry))
  }

  def getBicByBankId(bankId: BankId)(implicit system: ActorSystem): Future[Bic] = {
    val callResult = callObpApi(s"$endpointPrefix/banks/${bankId.value}", HttpMethods.GET)
    callResult.flatMap {
      case jsonResult if (jsonResult \\ "bank_routings").headOption.flatMap(_.asArray)
        .flatMap(_.find(bankRouting => (bankRouting \\ "scheme").headOption.flatMap(_.asString).contains("BIC"))).nonEmpty =>
        Future.successful((jsonResult \\ "bank_routings").headOption.flatMap(_.asArray)
          .flatMap(_.find(bankRouting => (bankRouting \\ "scheme").headOption.flatMap(_.asString).contains("BIC"))
            .flatMap(bankBicRouting => (bankBicRouting \\ "address").headOption.flatMap(_.asString))).map(Bic).get)
      case jsonResult if (jsonResult \\ "code").nonEmpty && (jsonResult \\ "message").nonEmpty =>
        val errorCode = (jsonResult \\ "code").headOption.flatMap(_.asNumber.flatMap(_.toInt))
        val errorMessage = (jsonResult \\ "message").headOption.flatMap(_.asString)
        (errorCode, errorMessage.flatMap(_.split(":").headOption)) match {
          case (Some(404), Some("OBP-30001")) =>
            Future.failed(new ObpBankNotFoundException(errorMessage.getOrElse("")))
          case _ => Future.failed(new Exception(s"Unknow error in getBicByBankId: ${errorMessage.getOrElse("")}"))
        }
      case jsonResult => Future.failed(new Exception(s"Unknow error in getBicByBankId: $jsonResult"))
    }
  }

  def getCounterpartyByIban(bankId: BankId, accountId: AccountId, iban: Iban)(implicit system: ActorSystem): Future[Option[CounterpartyJson400]] =
    callObpApi(s"$endpointPrefix/banks/${bankId.value}/accounts/${accountId.value}/owner/counterparties", HttpMethods.GET)
      .flatMap(json => Future.fromTry(json.as[CounterpartiesJson400].toTry))
      .map(counterparties =>
        counterparties.counterparties.find(counterparty =>
          counterparty.other_account_secondary_routing_scheme == "IBAN" && counterparty.other_account_secondary_routing_address == iban.iban)
      )

  def createCounterparty(bankId: BankId, accountId: AccountId, name: String, iban: Iban, bic: Bic)(implicit system: ActorSystem): Future[CounterpartyWithMetadataJson400] = {
    val counterparty = PostCounterpartyJson400(
      name = name,
      description = "Counterparty added by SEPA Adapter",
      currency = "EUR",
      other_account_routing_scheme = "",
      other_account_routing_address = "",
      other_account_secondary_routing_scheme = "IBAN",
      other_account_secondary_routing_address = iban.iban,
      other_bank_routing_scheme = "BIC",
      other_bank_routing_address = bic.bic,
      other_branch_routing_scheme = "",
      other_branch_routing_address = "",
      is_beneficiary = true,
      bespoke = Nil
    )
    
    callObpApi(s"$endpointPrefix/banks/${bankId.value}/accounts/${accountId.value}/owner/counterparties",
      HttpMethods.POST, counterparty.asJson.toString())
      .flatMap(json => Future.fromTry(json.as[CounterpartyWithMetadataJson400].toTry))
  }

  def getOrCreateCounterparty(bankId: BankId, accountId: AccountId, name: String, iban: Iban, bic: Bic)(implicit system: ActorSystem): Future[CounterpartyJson400] =
    for {
      // TODO: Check the counterparties names before inserting a new one (check if the name already exist)
      maybeCounterparty <- getCounterpartyByIban(bankId, accountId, iban)
      counterparty <- maybeCounterparty match {
        case Some(counterparty) => Future.successful(counterparty)
        case None => createCounterparty(bankId, accountId, name, iban, bic).map(CounterpartyJson400.fromCounterpartyWithMetadataJson400)
      }
    } yield counterparty

  def createRefundTransactionRequest(bankId: BankId, accountId: AccountId, from: Option[TransactionRequestRefundFrom], to: Option[TransactionRequestRefundTo],
                                    refundAmount: BigDecimal, refundDescription: String, originalObpTransactionId: TransactionId, reasonCode: String)(implicit system: ActorSystem): Future[TransactionRequestWithChargeJSON400] = {
    val transactionRequestRefundBody = TransactionRequestBodyRefundJsonV400(
      to = to,
      from = from,
      value = AmountOfMoneyJsonV121(currency = "EUR", amount = refundAmount.toString()),
      description = refundDescription,
      refund = RefundJson(transaction_id = originalObpTransactionId.value, reason_code = reasonCode)
    )

    callObpApi(s"$endpointPrefix/banks/${bankId.value}/accounts/${accountId.value}/owner/transaction-request-types/REFUND/transaction-requests",
      HttpMethods.POST, transactionRequestRefundBody.asJson.toString())
      .flatMap(json => Future.fromTry(json.as[TransactionRequestWithChargeJSON400].toTry))
  }

  def createSepaTransactionRequest(bankId: BankId, accountId: AccountId, amount: BigDecimal, counterpartyIban: Iban, description: String)(implicit system: ActorSystem): Future[TransactionRequestWithChargeJSON400] = {
    val currentDate = LocalDate.now()
    val transactionRequestSepaBody = TransactionRequestBodySEPAJsonV400(
      value = AmountOfMoneyJsonV121(currency = "EUR", amount = amount.toString()),
      to = IbanJson(counterpartyIban.iban),
      description = description,
      charge_policy = "SHARED",
      future_date = Some(currentDate.getYear.toString + currentDate.getMonthValue.toString + currentDate.getDayOfMonth.toString),
      reasons = None
    )

    callObpApi(s"$endpointPrefix/banks/${bankId.value}/accounts/${accountId.value}/owner/transaction-request-types/SEPA/transaction-requests",
      HttpMethods.POST, transactionRequestSepaBody.asJson.toString())
      .flatMap(json => Future.fromTry(json.as[TransactionRequestWithChargeJSON400].toTry))
  }

  def getTransactionById(bankId: BankId, accountId: AccountId, transactionId: TransactionId)(implicit system: ActorSystem): Future[TransactionJsonV300] = {
    callObpApi(s"$endpointPrefix/banks/${bankId.value}/accounts/${accountId.value}/owner/transactions/${transactionId.value}/transaction",
      HttpMethods.GET).flatMap(json => Future.fromTry(json.as[TransactionJsonV300].toTry))
  }

  def getTransactionRequest(bankId: BankId, accountId: AccountId, transactionRequestId: TransactionRequestId)(implicit system: ActorSystem): Future[TransactionRequestWithChargeJSON210] = {
    callObpApi(s"$endpointPrefix/banks/${bankId.value}/accounts/${accountId.value}/owner/transaction-requests/${transactionRequestId.value}",
      HttpMethods.GET).flatMap(json => Future.fromTry(json.as[TransactionRequestWithChargeJSON210].toTry))
  }

  @deprecated
  def getTransactionRequestChallengeId(bankId: BankId, accountId: AccountId, viewId: ViewId, transactionRequestId: TransactionRequestId)(implicit system: ActorSystem): Future[String] = {
    val callResult = callObpApi(s"$endpointPrefix/banks/${bankId.value}/accounts/${accountId.value}/${viewId.value}/transaction-requests/${transactionRequestId.value}", HttpMethods.GET)
    callResult.flatMap {
      case jsonResult if (jsonResult \\ "challenge").headOption.flatMap(challenge => (challenge \\ "id").headOption.flatMap(_.asString)).isDefined =>
        Future.successful((jsonResult \\ "challenge").headOption.flatMap(challenge => (challenge \\ "id").headOption.flatMap(_.asString)).get)
      case jsonResult if (jsonResult \\ "code").nonEmpty && (jsonResult \\ "message").nonEmpty =>
        val errorCode = (jsonResult \\ "code").headOption.flatMap(_.asNumber.flatMap(_.toInt))
        val errorMessage = (jsonResult \\ "message").headOption.flatMap(_.asString)
        (errorCode, errorMessage.flatMap(_.split(":").headOption)) match {
          case _ => Future.failed(new Exception(s"Unknow error in getTransactionRequestChallengeId: ${errorMessage.getOrElse("")}"))
        }
      case jsonResult => Future.failed(new Exception(s"No challenge Id in this transaction request : $jsonResult"))
    }
  }

  def answerTransactionRequestChallenge(bankId: BankId, accountId: AccountId, transactionRequestType: TransactionRequestType, transactionRequestId: TransactionRequestId, challengeAnswer: ChallengeAnswerJson400)(implicit system: ActorSystem): Future[TransactionRequestWithChargeJSON210] = {
    callObpApi(s"$endpointPrefix/banks/${bankId.value}/accounts/${accountId.value}/owner/transaction-request-types/${transactionRequestType.value}/transaction-requests/${transactionRequestId.value}/challenge",
      HttpMethods.POST, challengeAnswer.asJson.toString()).flatMap(json => Future.fromTry(json.as[TransactionRequestWithChargeJSON210].toTry))
  }

  def getTransactionRequestAttributes(bankId: BankId, accountId: AccountId, transactionRequestId: TransactionRequestId)(implicit system: ActorSystem): Future[TransactionRequestAttributesResponseJson] = {
    callObpApi(s"$endpointPrefix/banks/${bankId.value}/accounts/${accountId.value}/transaction-requests/${transactionRequestId.value}/attributes",
      HttpMethods.GET).flatMap(json => Future.fromTry(json.as[TransactionRequestAttributesResponseJson].toTry))
  }

  def createTransactionRequestAttribute(bankId: BankId, accountId: AccountId, transactionRequestId: TransactionRequestId, transactionRequestAttribute: TransactionRequestAttributeJsonV400)(implicit system: ActorSystem): Future[TransactionRequestAttributeResponseJson] = {
    callObpApi(s"$endpointPrefix/banks/${bankId.value}/accounts/${accountId.value}/transaction-requests/${transactionRequestId.value}/attribute",
      HttpMethods.POST, transactionRequestAttribute.asJson.toString()).flatMap(json => Future.fromTry(json.as[TransactionRequestAttributeResponseJson].toTry))
  }

  private def callObpApi(uri: String, httpMethod: HttpMethod, body: String = "")(implicit system: ActorSystem): Future[Json] = {
    implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
    
    Http(system).singleRequest(HttpRequest(
      method = httpMethod,
      uri = uri,
      headers = Seq(
        Authorization(GenericHttpCredentials("DirectLogin", s"token=${ConfigFactory.load().getString("obp-api.authorization.direct-login-token")}"))
      ),
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        body
      ))
    ).flatMap(_.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(body => body.utf8String).flatMap(response =>
      Future.fromTry(parser.parse(response).toTry)))
  }
}