package adapter

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit
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
import io.circe.parser
import io.circe.syntax._
import model.enums.{SepaCreditTransferTransactionStatus, SepaMessageStatus, SepaMessageType}
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import sepa.SepaUtil

import scala.collection.immutable.{List, Seq}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
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
        data = InboundAdapterInfoInternal("", Nil, "Adapter-Akka-CBS", "Jun2020", APIUtil.gitCommit, new Date().toString)
      )
      sender ! result

    case OutBoundGetBanks(callContext) =>
      val result = InBoundGetBanks(
        inboundAdapterCallContext = InboundAdapterCallContext(
          callContext.correlationId,
          callContext.sessionId,
          callContext.generalContext
        ),
        status = successInBoundStatus,
        data = List(BankCommons(
          bankId = BankId("OBP_BANK"),
          shortName = "OPBB",
          fullName = "Open Bank Project",
          logoUrl = "https://static.openbankproject.com/images/OBP_full_web_25pc.png",
          websiteUrl = "https://openbankproject.com",
          bankRoutingScheme = "IBAN",
          bankRoutingAddress = "BANK INBAN HERE",
          swiftBic = "DEOBPBB1XXX",
          nationalIdentifier = "National Identifier"
        )
        ))
      sender ! result

    case OutBoundGetBank(callContext, bankId) =>
      val result = InBoundGetBank(
        inboundAdapterCallContext = InboundAdapterCallContext(
          callContext.correlationId,
          callContext.sessionId,
          callContext.generalContext
        ),
        status = successInBoundStatus,
        data = BankCommons(
          bankId = BankId("OBP_BANK"),
          shortName = "OPBB",
          fullName = "Open Bank Project",
          logoUrl = "https://static.openbankproject.com/images/OBP_full_web_25pc.png",
          websiteUrl = "https://openbankproject.com",
          bankRoutingScheme = "IBAN",
          bankRoutingAddress = "BANK INBAN HERE",
          swiftBic = "DEOBPBB1XXX",
          nationalIdentifier = "National Identifier"
        )
      )
      sender ! result

    case OutBoundGetBankAccount(callContext, bankId, accountId) =>
      val result = InBoundGetBankAccount(
        inboundAdapterCallContext = InboundAdapterCallContext(
          callContext.correlationId,
          callContext.sessionId,
          callContext.generalContext
        ),
        status = successInBoundStatus,
        data = BankAccountCommons(
          bankId = BankId("OBP_BANK"),
          branchId = "thatBranch",
          accountId = AccountId("1"),
          accountType = "CURRENT",
          balance = BigDecimal(100),
          currency = "EUR",
          accountRoutingScheme = "",
          accountRoutingAddress = "",
          name = "account",
          label = "account label",
          iban = Some("DE52500105172997185866"),
          number = "16786686",
          lastUpdate = Date.from(Instant.now()),
          accountRoutings = List.empty,
          accountRules = List.empty,
          accountHolder = "Account holder"
        )
      )
      sender ! result

    case OutBoundCreateTransactionRequestv400(callContext, initiator, viewId, fromAccount, toAccount, transactionRequestType, transactionRequestCommonBody, detailsPlain, chargePolicy, challengeType, scaMethod) =>
      println("transaction request received")

      val creditTransferId = UUID.randomUUID()

      val creditTransferTransaction = SepaCreditTransferTransaction(
        id = creditTransferId,
        amount = BigDecimal(transactionRequestCommonBody.value.amount),
        debtorName = Some(fromAccount.accountHolder),
        debtorAccount = fromAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)),
        debtorAgent = Some(Bic(fromAccount.bankId.toString())),
        creditorName = Some(toAccount.accountHolder),
        creditorAccount = toAccount.accountRoutings.find(_.scheme == "IBAN").map(a => Iban(a.address)),
        creditorAgent = Some(Bic(toAccount.bankId.toString())),
        purposeCode = None,
        descripton = Some(transactionRequestCommonBody.description),
        creationDateTime = LocalDateTime.now(),
        transactionIdInSepaFile = SepaUtil.removeDashesToUUID(creditTransferId),
        instructionId = None,
        endToEndId = SepaUtil.removeDashesToUUID(creditTransferId),
        status = SepaCreditTransferTransactionStatus.UNPROCESSED
      )

      creditTransferTransaction.insert()

      val transactionRequest = TransactionRequest(
        id = TransactionRequestId(creditTransferId.toString),
        `type` = transactionRequestType.value,
        from = TransactionRequestAccount(
          bank_id = fromAccount.bankId.toString(),
          account_id = fromAccount.accountId.toString()
        ),
        body = TransactionRequestBodyAllTypes(
          to_sepa = None,
          to_sandbox_tan = None,
          to_counterparty = None,
          to_transfer_to_phone = None,
          to_transfer_to_atm = None,
          to_transfer_to_account = None,
          to_sepa_credit_transfers = Some(SepaCreditTransfers(
            debtorAccount = PaymentAccount(creditTransferTransaction.debtorAccount.map(_.iban).getOrElse("")),
            instructedAmount = AmountOfMoneyJsonV121(
              currency = transactionRequestCommonBody.value.currency,
              amount = transactionRequestCommonBody.value.amount
            ),
            creditorAccount = PaymentAccount(creditTransferTransaction.creditorAccount.map(_.iban).getOrElse("")),
            creditorName = toAccount.accountHolder
          )),
          value = AmountOfMoney(
            currency = transactionRequestCommonBody.value.currency,
            amount = transactionRequestCommonBody.value.amount
          ),
          description = transactionRequestCommonBody.description
        ),
        transaction_ids = creditTransferId.toString,
        status = "COMPLETED",
        start_date = Date.from(Instant.now()),
        end_date = null,
        challenge = TransactionRequestChallenge(
          id = UUID.randomUUID().toString,
          allowed_attempts = 3,
          challenge_type = challengeType.getOrElse("")
        ),
        charge = TransactionRequestCharge(
          summary = "Transaction fees",
          value = AmountOfMoney(
            currency = "EUR",
            amount = "1"
          )
        ),
        charge_policy = chargePolicy,
        counterparty_id = CounterpartyId("Counterpart ID"),
        name = "Name of the transaction request ?",
        this_bank_id = fromAccount.bankId,
        this_account_id = fromAccount.accountId,
        this_view_id = viewId,
        other_account_routing_scheme = "string",
        other_account_routing_address = "string",
        other_bank_routing_scheme = "string",
        other_bank_routing_address = "string",
        is_beneficiary = true,
        future_date = Some("string")
      )
      val result = InBoundCreateTransactionRequestv400(
        inboundAdapterCallContext = InboundAdapterCallContext(
          callContext.correlationId,
          callContext.sessionId,
          callContext.generalContext
        ),
        status = successInBoundStatus,
        data = transactionRequest
      )
      sender ! result

    case OutBoundGetTransactionRequestImpl(callContext, transactionRequestId) =>

      println("getTransactionRequest Received")

      val inboundAdapterCallContext = InboundAdapterCallContext(
        callContext.correlationId,
        callContext.sessionId,
        callContext.generalContext
      )

      SepaCreditTransferTransaction.getById(UUID.fromString(transactionRequestId.value)).map(_.map(creditTransfer =>
        creditTransfer
      ))

      val result = SepaCreditTransferTransaction.getById(UUID.fromString(transactionRequestId.value)).map {
        case Some(creditTransfer) =>
          println("transaction found")
          InBoundGetTransactionRequestImpl(
            inboundAdapterCallContext = inboundAdapterCallContext,
            status = successInBoundStatus,
            data = TransactionRequest(
              id = TransactionRequestId(creditTransfer.id.toString),
              `type` = "SEPA",
              from = TransactionRequestAccount(
                creditTransfer.debtorAgent.map(_.bic).getOrElse(""),
                account_id = creditTransfer.debtorAccount.map(_.iban).getOrElse("")
              ),
              body = TransactionRequestBodyAllTypes(
                to_sepa = None,
                to_sandbox_tan = None,
                to_counterparty = None,
                to_transfer_to_phone = None,
                to_transfer_to_atm = None,
                to_transfer_to_account = None,
                to_sepa_credit_transfers = Some(SepaCreditTransfers(
                  debtorAccount = PaymentAccount(creditTransfer.debtorAccount.map(_.iban).getOrElse("")),
                  instructedAmount = AmountOfMoneyJsonV121(
                    currency = "EUR",
                    amount = creditTransfer.amount.toString()
                  ),
                  creditorAccount = PaymentAccount(creditTransfer.creditorAccount.map(_.iban).getOrElse("")),
                  creditorName = creditTransfer.creditorName.getOrElse("")
                )),
                value = AmountOfMoney(
                  currency = "EUR",
                  amount = creditTransfer.amount.toString()
                ),
                description = creditTransfer.descripton.getOrElse("")
              ),
              transaction_ids = creditTransfer.id.toString,
              status = creditTransfer.status.toString,
              start_date = Date.from(creditTransfer.creationDateTime.toInstant(ZoneOffset.UTC)),
              end_date = Date.from(Instant.now()),
              challenge = TransactionRequestChallenge(
                id = UUID.randomUUID().toString,
                allowed_attempts = 3,
                challenge_type = ""
              ),
              charge = TransactionRequestCharge(
                summary = "Transaction fees",
                value = AmountOfMoney(
                  currency = "EUR",
                  amount = "1"
                )
              ),
              charge_policy = "",
              counterparty_id = CounterpartyId("Counterpart ID"),
              name = "Name of the transaction request ?",
              this_bank_id = BankId(""),
              this_account_id = AccountId(""),
              this_view_id = ViewId(""),
              other_account_routing_scheme = "string",
              other_account_routing_address = "string",
              other_bank_routing_scheme = "string",
              other_bank_routing_address = "string",
              is_beneficiary = true,
              future_date = Some("string")
            )
          )
        case None =>
          InBoundGetTransactionRequestImpl(
            inboundAdapterCallContext = inboundAdapterCallContext,
            status = Status(errorCode = "ERROR", List(InboundStatusMessage("SEPA Adapter", "ERROR", "400", "error here"))),
            data = null
          )
      }
      result.map {
        println("message sent")
        sender ! _
      }
      Await.result(result, Duration(1, TimeUnit.MINUTES)).wait()

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
        descripton = Some(description),
        creationDateTime = LocalDateTime.now(),
        transactionIdInSepaFile = SepaUtil.removeDashesToUUID(creditTransferTransactionId),
        instructionId = None,
        endToEndId = SepaUtil.removeDashesToUUID(creditTransferTransactionId),
        status = SepaCreditTransferTransactionStatus.UNPROCESSED
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
            description = creditTransferTransaction.descripton.getOrElse(""),
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
}