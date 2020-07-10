package adapter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.{Date, UUID}

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import com.openbankproject.commons.dto._
import com.openbankproject.commons.model._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{JsonObject, parser}
import model.enums.{SepaCreditTransferTransactionStatus, SepaMessageStatus, SepaMessageType}
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import sepa.SepaUtil

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

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

      val jsonDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

      val saveHistoricalTransactionResponse = Http(context.system).singleRequest(HttpRequest(
        method = HttpMethods.POST,
        uri = "http://localhost:8080/obp/v4.0.0/management/historical/transactions",
        headers = Seq(
          Authorization(GenericHttpCredentials("DirectLogin", "token=eyJhbGciOiJIUzI1NiJ9.eyIiOiIifQ.CeA_QUnsF4xBScAYy3ZtK64f7uE28nHbXSFoAlodUQM"))
        ),
        entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          HistoricalTransactionJson(
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
          ).asJson.toString()
        )
      ))

      implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

      saveHistoricalTransactionResponse.onComplete {
        case Success(res) =>
          res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(body => body.utf8String).map(response => {
            parser.parse(response).map(jsonResult => {
              println(jsonResult)
              (jsonResult \\ "transaction_id").headOption.flatMap(_.asString).map(createdTransactionId => {
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
                    obpTransactionId = Some(UUID.fromString(createdTransactionId))
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
                  data = TransactionId(createdTransactionId)
                )
                obpAkkaConnector ! result
              })
            })
          })
        case Failure(exception) => sys.error(exception.getMessage)
      }


    case _ => println(s"Message received but not implemented")
  }

  def successInBoundStatus: Status = Status("", Nil)

  def getObpAccountIdByIban(bankId: BankId, viewId: ViewId, iban: Iban): Future[AccountId] = {
    implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
    Http(context.system).singleRequest(
      HttpRequest(
        method = HttpMethods.POST,
        uri = s"http://localhost:8080/obp/v4.0.0/banks/${bankId.value}/accounts/${viewId.value}/account",
        headers = Seq(
          Authorization(GenericHttpCredentials("DirectLogin", "token=eyJhbGciOiJIUzI1NiJ9.eyIiOiIifQ.CeA_QUnsF4xBScAYy3ZtK64f7uE28nHbXSFoAlodUQM"))
        ),
        entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          JsonObject.fromMap(Map(("iban", iban.iban.asJson))).asJson.toString()
        )
      )
    ).flatMap(_.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(body => body.utf8String).flatMap(response => {
      parser.parse(response).toTry match {
        case Success(jsonResult) if (jsonResult \\ "id").nonEmpty =>
          (jsonResult \\ "id").headOption.flatMap(_.asString) match {
            case Some(accountId) => Future.successful(AccountId(accountId))
            case None => Future.failed(new Exception("Impossible to convert id to String"))
          }
        case Success(jsonResult) if (jsonResult \\ "code").nonEmpty && (jsonResult \\ "message").nonEmpty =>
          val errorCode = (jsonResult \\ "code").headOption.flatMap(_.asNumber.flatMap(_.toInt))
          val errorMessage = (jsonResult \\ "message").headOption.flatMap(_.asString.flatMap(_.split(":").headOption))
          (errorCode, errorMessage) match {
            case (Some(404), Some("OBP-30018")) =>
                 Future.failed(new ObpAccountNotFoundException(s"Account not found in OBP-API : ${iban}"))
            case _ => Future.failed(new Exception(s"Unknow error in getObpAccountIdByIban: ${errorMessage.getOrElse("")}"))
          }

        case Failure(exception) => Future.failed(new Exception(s"Impossible to parse json response : $response | exception : ${exception.getMessage}"))
      }
    }))
  }
}