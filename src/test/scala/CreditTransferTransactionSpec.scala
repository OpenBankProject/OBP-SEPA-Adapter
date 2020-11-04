import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import adapter.obpApiModel.{AccountRoutingJsonV121, ObpApi}
import adapter.{Adapter, ObpAccountNotFoundException}
import com.openbankproject.commons.model._
import model.enums.sepaReasonCodes.PaymentReturnReasonCode
import model.enums.{SepaCreditTransferTransactionCustomField, SepaCreditTransferTransactionStatus, SepaMessageType}
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage, SepaTransactionMessage}
import org.scalatest.Assertions.succeed
import org.scalatest.featurespec.AsyncFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{GivenWhenThen, OptionValues}
import sepa.SepaUtil
import sepa.scheduler.ProcessIncomingFilesActorSystem

import scala.concurrent.Future

// TODO : test the outgoing file generation

class CreditTransferTransactionSpec extends AsyncFeatureSpec with GivenWhenThen with Matchers with OptionValues with AkkaActorSystemTest {

  Feature("Send a credit transfer transaction from an OBP account to an external account") {
    Scenario("Send a SEPA transaction to an existing counterparty") {
      for {
        _ <- Future(Given("an OBP account and transaction content"))
        bankId: BankId = BankId("THE_DEFAULT_BANK_ID")
        accountId: AccountId = AccountId("1023794c-a412-11ea-bb37-0242ac130002")
        amount = BigDecimal(100)
        counterpartyIban: Iban = Iban("DE89370400440532013002")
        description = "Sepa transaction description"

        _ <- Future(When("we send the request to create the SEPA transaction request"))
        transactionRequest <- ObpApi.createSepaTransactionRequest(bankId, accountId, amount, counterpartyIban, description)

        _ <- Future(Then("we should get a successful response"))
        _ <- transactionRequest.`type` should be("SEPA")
        _ <- transactionRequest.status should be("COMPLETED")
        _ <- transactionRequest.challenge should be(None)
        _ <- transactionRequest.details.description should be(description)
        _ <- transactionRequest.details.value.amount should be(amount.toString())
        _ <- transactionRequest.details.value.currency should be("EUR")
        _ <- transactionRequest.from.bank_id should be(bankId.value)
        _ <- transactionRequest.from.account_id should be(accountId.value)

        _ <- Future(And("the transaction should exist on the OBP-API"))
        transaction <- ObpApi.getTransactionById(bankId, accountId, TransactionId(transactionRequest.transaction_ids.head))
        _ <- transaction.other_account.account_routings should contain(AccountRoutingJsonV121("IBAN", counterpartyIban.iban))
        _ <- transaction.details.`type` should be("SEPA")
        _ <- transaction.details.description should be(description)
        _ <- BigDecimal(transaction.details.value.amount) should be(-amount)

        _ <- Future(And("the transaction should be saved in the SEPA Adapter"))
        creditTransferTransaction <- SepaCreditTransferTransaction.getByObpTransactionId(TransactionId(transactionRequest.transaction_ids.head))
        _ <- creditTransferTransaction.status should be(SepaCreditTransferTransactionStatus.UNPROCESSED)
        _ <- creditTransferTransaction.description.value should be(description)
        _ <- creditTransferTransaction.amount should be(amount)
        _ <- creditTransferTransaction.creditorAccount.value should be(counterpartyIban)
      } yield succeed
    }
  }

  Feature("Receive a SEPA transaction from an external account to an OBP account") {
    Scenario("We receive a transaction to an existing OBP account") {
      for {
        _ <- Future(Given("An XML file containing the received transaction"))
        creditTransferFileTest = CreditTransferFileTest(
          messageIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID()),
          transactionAmount = BigDecimal(25),
          currentDateTime = LocalDateTime.now(),
          settlementDate = LocalDate.now().plusDays(1),
          transactionIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID()),
          debtorName = ObpCounterpartyTest.NAME,
          debtorIban = ObpCounterpartyTest.IBAN,
          debtorBic = ObpCounterpartyTest.BIC,
          creditorName = ObpAccountTest.ACCOUNT_HOLDER_NAME,
          creditorIban = ObpAccountTest.IBAN,
          creditorBic = ObpAccountTest.BIC,
          transactionDescription = "A transaction message for example"
        )
        filepath = creditTransferFileTest.write()

        _ <- Future(When("we integrate the SEPA file in the SEPA Adapter"))
        _ <- Future(ProcessIncomingFilesActorSystem.main(Array(filepath.path)))
        _ <- Future(Thread.sleep(5000))

        _ <- Future(Then("the transaction should be integrated in the SEPA Adapter"))
        creditTransferTransaction <- SepaCreditTransferTransaction.getByTransactionIdInSepaFile(creditTransferFileTest.transactionIdInSepaFile)
        _ <- creditTransferTransaction.creditor.value.name.value should be(creditTransferFileTest.creditorName)
        _ <- creditTransferTransaction.creditorAccount.value should be(creditTransferFileTest.creditorIban)
        _ <- creditTransferTransaction.creditorAgent.value should be(creditTransferFileTest.creditorBic)
        _ <- creditTransferTransaction.debtor.value.name.value should be(creditTransferFileTest.debtorName)
        _ <- creditTransferTransaction.debtorAccount.value should be(creditTransferFileTest.debtorIban)
        _ <- creditTransferTransaction.debtorAgent.value should be(creditTransferFileTest.debtorBic)
        _ <- creditTransferTransaction.amount should be(creditTransferFileTest.transactionAmount)
        _ <- creditTransferTransaction.description.value should be(creditTransferFileTest.transactionDescription)
        _ <- creditTransferTransaction.status should be(SepaCreditTransferTransactionStatus.TRANSFERED)

        _ <- Future(Then("the transaction should be integrated in the OBP-API"))
        sepaAdapterMessage <- SepaMessage.getBySepaCreditTransferTransactionId(creditTransferTransaction.id)
          .map(_.find(_.messageType == SepaMessageType.B2B_CREDIT_TRANSFER).value)
        sepaMessageTransaction <- SepaTransactionMessage
          .getBySepaCreditTransferTransactionIdAndSepaMessageId(creditTransferTransaction.id, sepaAdapterMessage.id)
        obpAccount <- ObpApi.getAccountByIban(Some(Adapter.BANK_ID), creditTransferTransaction.creditorAccount.value)
        bankId = BankId(obpAccount.bank_id)
        accountId = AccountId(obpAccount.id)
        obpApiTransaction <- ObpApi.getTransactionById(bankId, accountId, sepaMessageTransaction.obpTransactionId.value)
        _ <- obpApiTransaction.this_account.account_routings should contain(AccountRoutingJsonV121("IBAN", creditTransferFileTest.creditorIban.iban))
        _ <- obpApiTransaction.other_account.account_routings should contain(AccountRoutingJsonV121("IBAN", creditTransferFileTest.debtorIban.iban))
        _ <- obpApiTransaction.details.`type` should be("SEPA")
        _ <- obpApiTransaction.details.description should be(creditTransferFileTest.transactionDescription)
        _ <- BigDecimal(obpApiTransaction.details.value.amount) should be(creditTransferFileTest.transactionAmount)
      } yield succeed
    }
  }

  Scenario("We receive a transaction to a non-existing OBP account") {
    for {
      _ <- Future(Given("An XML file containing the received transaction"))
      creditTransferFileTest = CreditTransferFileTest(
        messageIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID()),
        transactionAmount = BigDecimal(666),
        currentDateTime = LocalDateTime.now(),
        settlementDate = LocalDate.now().plusDays(1),
        transactionIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID()),
        debtorName = ObpCounterpartyTest.NAME,
        debtorIban = ObpCounterpartyTest.IBAN,
        debtorBic = ObpCounterpartyTest.BIC,
        creditorName = "Nonexisting Account",
        creditorIban = Iban("DE11111111111111111111"),
        creditorBic = Bic("OBPBDEB1XXX"),
        transactionDescription = "A transaction message for example"
      )
      filepath = creditTransferFileTest.write()

      _ <- Future(When("we integrate the SEPA file in the SEPA Adapter"))
      _ <- Future(ProcessIncomingFilesActorSystem.main(Array(filepath.path)))
      _ <- Future(Thread.sleep(5000))

      _ <- Future(Then("the transaction should be integrated in the SEPA Adapter with a TO RETURN status"))
      creditTransferTransaction <- SepaCreditTransferTransaction.getByTransactionIdInSepaFile(creditTransferFileTest.transactionIdInSepaFile)
      _ <- creditTransferTransaction.creditor.value.name.value should be(creditTransferFileTest.creditorName)
      _ <- creditTransferTransaction.creditorAccount.value should be(creditTransferFileTest.creditorIban)
      _ <- creditTransferTransaction.creditorAgent.value should be(creditTransferFileTest.creditorBic)
      _ <- creditTransferTransaction.debtor.value.name.value should be(creditTransferFileTest.debtorName)
      _ <- creditTransferTransaction.debtorAccount.value should be(creditTransferFileTest.debtorIban)
      _ <- creditTransferTransaction.debtorAgent.value should be(creditTransferFileTest.debtorBic)
      _ <- creditTransferTransaction.amount should be(creditTransferFileTest.transactionAmount)
      _ <- creditTransferTransaction.description.value should be(creditTransferFileTest.transactionDescription)
      _ <- creditTransferTransaction.status should be(SepaCreditTransferTransactionStatus.TO_RETURN)
      _ <- (creditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_REASON_CODE.toString).head.asString.value should be(PaymentReturnReasonCode.INCORRECT_ACCOUNT_NUMBER.toString)

      _ <- Future(Then("the transaction should not be integrated in the OBP-API"))
      sepaAdapterMessage <- SepaMessage.getBySepaCreditTransferTransactionId(creditTransferTransaction.id)
        .map(_.find(_.messageType == SepaMessageType.B2B_CREDIT_TRANSFER).value)
      sepaMessageTransaction <- SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(creditTransferTransaction.id, sepaAdapterMessage.id)
      _ <- recoverToSucceededIf[ObpAccountNotFoundException] {
        ObpApi.getAccountByIban(Some(Adapter.BANK_ID), creditTransferTransaction.creditorAccount.value)
      }
      _ <- sepaMessageTransaction.obpTransactionId should be(None)
    } yield succeed
  }
}
