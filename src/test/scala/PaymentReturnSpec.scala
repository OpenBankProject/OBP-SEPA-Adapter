import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import adapter.obpApiModel.{AccountRoutingJsonV121, ChallengeAnswerJson400, ObpApi, TransactionRequestRefundFrom}
import com.openbankproject.commons.model._
import model.enums.sepaReasonCodes.PaymentReturnReasonCode
import model.enums.{SepaCreditTransferTransactionCustomField, SepaCreditTransferTransactionStatus, SepaMessageType}
import model.{SepaCreditTransferTransaction, SepaMessage, SepaTransactionMessage}
import org.scalatest.Assertions.succeed
import org.scalatest.featurespec.AsyncFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{GivenWhenThen, OptionValues}
import sepa.SepaUtil
import sepa.scheduler.ProcessIncomingFilesActorSystem

import scala.concurrent.Future

// TODO : test the outgoing file generation

class PaymentReturnSpec extends AsyncFeatureSpec with GivenWhenThen with Matchers with OptionValues with AkkaActorSystemTest {

  Feature("Return a received transaction to its originator") {
    Scenario("Refund a SEPA transaction to an existing counterparty") {
      for {
        _ <- Future(Given("an OBP account and an existing received transaction"))
        creditTransferFileTest = CreditTransferFileTest(
          messageIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID()),
          transactionAmount = BigDecimal(458),
          currentDateTime = LocalDateTime.now(),
          settlementDate = LocalDate.now().plusDays(1),
          transactionIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID()),
          debtorName = ObpCounterpartyTest.NAME,
          debtorIban = ObpCounterpartyTest.IBAN,
          debtorBic = ObpCounterpartyTest.BIC,
          creditorName = ObpAccountTest.ACCOUNT_HOLDER_NAME,
          creditorIban = ObpAccountTest.IBAN,
          creditorBic = ObpAccountTest.BIC,
          transactionDescription = "A transaction message for example")
        filepath = creditTransferFileTest.write()
        _ <- Future(ProcessIncomingFilesActorSystem.main(Array(filepath.path)))
        _ <- Future(Thread.sleep(3000))
        sepaCreditTransferTransaction <- SepaCreditTransferTransaction.getByTransactionIdInSepaFile(creditTransferFileTest.transactionIdInSepaFile)
        sepaMessage <- SepaMessage.getByMessageIdInSepaFile(creditTransferFileTest.messageIdInSepaFile)
        sepaMessageTransaction <- SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(
          sepaCreditTransferTransaction.id, sepaMessage.id)
        _ <- sepaCreditTransferTransaction.status should be(SepaCreditTransferTransactionStatus.TRANSFERED)
        _ <- sepaMessageTransaction.obpTransactionId should not be None

        _ <- Future(When("we send the refund request"))
        originalObpTransactionId = sepaMessageTransaction.obpTransactionId.value
        obpCounterparty <- ObpApi.getCounterpartyByIban(ObpAccountTest.BANK_ID, ObpAccountTest.ACCOUNT_ID, ObpCounterpartyTest.IBAN)
        transactionRequestRefund <- ObpApi.createRefundTransactionRequest(
          bankId = ObpAccountTest.BANK_ID,
          accountId = ObpAccountTest.ACCOUNT_ID,
          from = Some(TransactionRequestRefundFrom(obpCounterparty.value.counterparty_id)),
          to = None,
          refundAmount = creditTransferFileTest.transactionAmount,
          refundDescription = "I don't need this money",
          originalObpTransactionId = originalObpTransactionId,
          reasonCode = PaymentReturnReasonCode.NOT_SPECIFIED_REASON_CUSTOMER_GENERATED.toString)
        transactionRequestRefundAfterChallenge <- ObpApi.answerTransactionRequestChallenge(
                 bankId = ObpAccountTest.BANK_ID,
                 accountId = ObpAccountTest.ACCOUNT_ID,
                 transactionRequestType = TransactionRequestType(transactionRequestRefund.`type`),
                 transactionRequestId = TransactionRequestId(transactionRequestRefund.id),
                 challengeAnswer = ChallengeAnswerJson400(transactionRequestRefund.challenges.value.headOption.value.id, "123"))
        _ <- transactionRequestRefund.status should be("INITIATED")
        _ <- transactionRequestRefund.challenges should not be None
        _ <- transactionRequestRefundAfterChallenge.`type` should be("REFUND")
        _ <- transactionRequestRefundAfterChallenge.status should be("COMPLETED")
        _ <- transactionRequestRefundAfterChallenge.challenge should not be None
        _ <- transactionRequestRefundAfterChallenge.details.value.amount should be(creditTransferFileTest.transactionAmount.toString())
        _ <- transactionRequestRefundAfterChallenge.details.value.currency should be("EUR")
        _ <- transactionRequestRefundAfterChallenge.from.bank_id should be(ObpAccountTest.BANK_ID.value)
        _ <- transactionRequestRefundAfterChallenge.from.account_id should be(ObpAccountTest.ACCOUNT_ID.value)

        _ <- Future(Then("the refund transaction should exist on the OBP-API"))
        refundTransactionId = TransactionId(transactionRequestRefundAfterChallenge.transaction_ids.head)
        refundTransaction <- ObpApi.getTransactionById(ObpAccountTest.BANK_ID, ObpAccountTest.ACCOUNT_ID, refundTransactionId)
        _ <- refundTransaction.other_account.account_routings should contain(AccountRoutingJsonV121("IBAN", ObpCounterpartyTest.IBAN.iban))
        _ <- refundTransaction.details.`type` should be("SEPA")
        _ <- BigDecimal(refundTransaction.details.value.amount) should be(-creditTransferFileTest.transactionAmount)

        _ <- Future(And("the transaction should be updated on the SEPA Adapter"))
        sepaCreditTransferTransaction <- SepaCreditTransferTransaction.getByTransactionIdInSepaFile(creditTransferFileTest.transactionIdInSepaFile)
        sepaRefundMessage <- SepaMessage.getBySepaCreditTransferTransactionId(sepaCreditTransferTransaction.id)
          .map(_.find(_.messageType == SepaMessageType.B2B_PAYMENT_RETURN))
        sepaMessageTransaction <- SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(
          sepaCreditTransferTransaction.id, sepaRefundMessage.value.id)
        _ <- sepaCreditTransferTransaction.status should be(SepaCreditTransferTransactionStatus.TO_RETURN)
        _ <- (sepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_REASON_CODE.toString).head.asString.value should be(PaymentReturnReasonCode.NOT_SPECIFIED_REASON_CUSTOMER_GENERATED.toString)
        _ <- sepaRefundMessage should not be None
        _ <- sepaMessageTransaction.obpTransactionRequestId.value.value should be(transactionRequestRefund.id)
        _ <- sepaMessageTransaction.obpTransactionId.value.value should be(refundTransaction.id)
      } yield succeed
    }
  }
}
