package adapter

import java.time.{Instant, LocalDateTime}
import java.util.{Date, UUID}

import akka.actor.{Actor, ActorLogging, DeadLetter}
import akka.cluster.Cluster
import com.openbankproject.commons.dto._
import com.openbankproject.commons.model._
import model.enums.SepaMessageType
import model.types.Bic
import sepa.{CreditTransferMessage, SepaCreditTransferTransaction, SepaUtil}

import scala.collection.immutable.List

class CbsActor extends Actor with ActorLogging {

  val cluster: Cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.join(self.path.address)
    context.system.eventStream.subscribe(self, classOf[DeadLetter])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive: Receive = {
    case OutBoundGetAdapterInfo(callContext) => {
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
    }

    case OutBoundGetBanks(callContext) => {
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
    }

    case OutBoundGetBank(callContext, bankId) => {
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
    }

    case OutBoundGetBankAccount(callContext, bankId, accountId) => {
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
    }

    case OutBoundCreateTransactionRequestv400(callContext, initiator, viewId, fromAccount, toAccount, transactionRequestType, transactionRequestCommonBody, detailsPlain, chargePolicy, challengeType, scaMethod) => {
      println("transaction request received")

      val creditTransferMessageId = UUID.randomUUID()
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
        sepaMessageId = creditTransferMessageId,
        idInSepaFile = SepaUtil.removeDashesToUUID(creditTransferId),
        instructionId = None,
        endToEndId = SepaUtil.removeDashesToUUID(creditTransferId)
      )


      val creditTransferMessage = CreditTransferMessage(
        id = creditTransferMessageId,
        creationDateTime = LocalDateTime.now(),
        messageType = SepaMessageType.B2B_CREDIT_TRANSFER,
        content = None,
        sepaFileId = None,
        idInSepaFile = SepaUtil.removeDashesToUUID(creditTransferMessageId),
        interbankSettlementDate = None,
        instigatingAgent = None,
        instigatedAgent = None,
        creditTransferTransactions = Seq(creditTransferTransaction)
      )

      println(fromAccount.accountRoutings)

      val transactionRequest = TransactionRequest(
        id = TransactionRequestId(UUID.randomUUID().toString),
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
        status = "INITIATED",
        start_date = Date.from(Instant.now()),
        end_date = Date.from(Instant.now()),
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
    }

    case _ => println(s"Message received but not implemented")
  }

  def successInBoundStatus: Status = Status("", Nil)
}