package adapter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.{Date, UUID}

import adapter.obpApiModel.{CounterpartyAccountReference, CustomerAccountReference, HistoricalTransactionJson, RefundTransactionRequest}
import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import com.openbankproject.commons.dto._
import com.openbankproject.commons.model._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Json, JsonObject, parser}
import model.enums.sepaReasonCodes.PaymentReturnReasonCode.PaymentReturnReasonCode
import model.enums.sepaReasonCodes.{PaymentRecallReasonCode, PaymentReturnReasonCode}
import model.enums.{SepaCreditTransferTransactionStatus, SepaMessageStatus, SepaMessageType}
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import sepa.{PaymentReturnMessage, SepaUtil}

import scala.collection.immutable.Seq
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

          saveObpHistoricalTransaction(historicalTransactionJson).map { createdObpTransactionId =>
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
          val reasonCodeString = description.split(" - ").last.split(":").last.trim
          val refundReasonCode: PaymentReturnReasonCode = reasonCodeString match {
              case reasoncode if PaymentRecallReasonCode.values.map(_.toString).contains(reasoncode) => PaymentReturnReasonCode.FOLLOWING_CANCELLATION_REQUEST
              case _ => PaymentReturnReasonCode.withName(reasonCodeString)
            }

          val originalSepaCreditTransferTransaction = SepaCreditTransferTransaction.getByObpTransactionId(originalObpTransactionId)

          originalSepaCreditTransferTransaction.map {
            case Some(originalTransaction) =>
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
                createdObpTransactionId <- saveObpHistoricalTransaction(historicalTransactionJson)
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


            case None => Future.failed(new Exception(s"Original credit transfer transaction with obpTransactionId (${originalObpTransactionId.value}) not found in SEPA Adapter"))
          }


      }


    case _ => println(s"Message received but not implemented")
  }

  def successInBoundStatus: Status = Status("", Nil)

  def saveObpHistoricalTransaction(historicalTransactionJson: HistoricalTransactionJson): Future[UUID] = {
    val body = historicalTransactionJson.asJson.toString()
    val callResult = obpApiCall("http://localhost:8080/obp/v4.0.0/management/historical/transactions", HttpMethods.POST, body)
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

  def getObpAccountIdByIban(bankId: BankId, viewId: ViewId, iban: Iban): Future[AccountId] = {
    val body = JsonObject.fromMap(Map(("iban", iban.iban.asJson))).asJson.toString()
    val callResult = obpApiCall(s"http://localhost:8080/obp/v4.0.0/banks/${bankId.value}/accounts/${viewId.value}/account", HttpMethods.POST, body)

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

  def createObpRefundTransactionRequest(bankId: BankId, accountId: AccountId, viewId: ViewId, refundTransactionRequest: RefundTransactionRequest): Future[TransactionRequestId] = {
    val body = refundTransactionRequest.asJson.toString()
    val callResult = obpApiCall(s"http://localhost:8080/obp/v4.0.0/banks/${bankId.value}/accounts/${accountId.value}/${viewId.value}/transaction-request-types/REFUND/transaction-requests", HttpMethods.POST, body)
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

  def obpApiCall(uri: String, httpMethod: HttpMethod, body: String): Future[Json] = {
    implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

    Http(context.system).singleRequest(HttpRequest(
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
}