import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import adapter.obpApiModel.{AccountRoutingJsonV121, ChallengeAnswerJson400, ObpApi, TransactionRequestRefundFrom}
import com.openbankproject.commons.model._
import fileGeneration.{CreditTransferFileTest, PaymentReturnFileTest}
import model.enums._
import model.enums.sepaReasonCodes.PaymentReturnReasonCode
import model.{SepaCreditTransferTransaction, SepaMessage, SepaTransactionMessage}
import org.scalatest.featurespec.AsyncFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{GivenWhenThen, OptionValues}
import sepa.SepaUtil
import sepa.scheduler.{ProcessIncomingFilesActorSystem, ProcessOutgoingFiles}
import util.{ObpAccountTest, ObpCounterpartyTest}

import scala.concurrent.Future

// TODO : test the outgoing file generation
// TODO: Add tests related to the transaction request attributes original_transaction_id and refund_reason_code

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
          challengeAnswer = ChallengeAnswerJson400(transactionRequestRefund.challenges.value.headOption.value.id, "123")
        )
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

  Feature("Receive a payment return") {
    Scenario("Receive a payment return from a counterparty on a existing transaction") {
      for {
        _ <- Future(Given("an OBP account, an existing sent transaction"))
        originalTransactionAmount = BigDecimal(845.25)
        transactionRequest <- ObpApi.createSepaTransactionRequest(
          bankId = ObpAccountTest.BANK_ID,
          accountId = ObpAccountTest.ACCOUNT_ID,
          amount = originalTransactionAmount,
          counterpartyIban = ObpCounterpartyTest.IBAN,
          description = "Here is a transaction description"
        )
        obpTransactionId = TransactionId(transactionRequest.transaction_ids.head)
        obpTransaction <- ObpApi.getTransactionById(ObpAccountTest.BANK_ID, ObpAccountTest.ACCOUNT_ID, obpTransactionId)
        _ <- Future(ProcessOutgoingFiles.main(Array()))
        _ <- Future(Thread.sleep(3000))
        originalSepaCreditTransferTransaction <- SepaCreditTransferTransaction.getByObpTransactionId(obpTransactionId)
        _ <- originalSepaCreditTransferTransaction.status should be(SepaCreditTransferTransactionStatus.TRANSFERED)
        originalSepaCreditTransferMessage <- SepaMessage.getBySepaCreditTransferTransactionId(originalSepaCreditTransferTransaction.id)
          .map(_.find(_.messageType == SepaMessageType.B2B_CREDIT_TRANSFER).value)

        _ <- Future(When("we receive a payment return"))
        paymentReturnFile = PaymentReturnFileTest(
          messageIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID()),
          originalTransactionAmount = originalSepaCreditTransferTransaction.amount,
          currentDateTime = LocalDateTime.now(),
          settlementDate = LocalDate.now().plusDays(1),
          originalMessageIdInSepaFile = originalSepaCreditTransferMessage.messageIdInSepaFile,
          paymentReturnIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID()),
          originalEndToEndId = originalSepaCreditTransferTransaction.endToEndId,
          originalTransactionIdInSepaFile = originalSepaCreditTransferTransaction.transactionIdInSepaFile,
          paymentReturnOriginatorBic = originalSepaCreditTransferTransaction.creditorAgent.value,
          paymentReturnReasonCode = PaymentReturnReasonCode.CLOSED_ACCOUNT_NUMBER,
          originalDebtorName = originalSepaCreditTransferTransaction.debtor.value.name.value,
          originalDebtorIban = originalSepaCreditTransferTransaction.debtorAccount.value,
          originalDebtorBic = originalSepaCreditTransferTransaction.debtorAgent.value,
          originalCreditorName = originalSepaCreditTransferTransaction.creditor.value.name.value,
          originalCreditorIban = originalSepaCreditTransferTransaction.creditorAccount.value,
          originalCreditorBic = originalSepaCreditTransferTransaction.creditorAgent.value,
          originalTransactionDescription = originalSepaCreditTransferTransaction.description.getOrElse("")
        )
        filepath = paymentReturnFile.write()
        _ <- Future(ProcessIncomingFilesActorSystem.main(Array(filepath.path)))
        _ <- Future(Thread.sleep(3000))

        _ <- Future(Then("the payment return message should be integrated in the SEPA Adapter"))
        paymentReturnMessage <- SepaMessage.getBySepaCreditTransferTransactionId(originalSepaCreditTransferTransaction.id)
          .map(_.find(_.messageType == SepaMessageType.B2B_PAYMENT_RETURN).value)
        _ <- paymentReturnMessage.messageIdInSepaFile should be(paymentReturnFile.messageIdInSepaFile)
        _ <- paymentReturnMessage.status should be(SepaMessageStatus.PROCESSED)
        _ <- (paymentReturnMessage.customFields.value \\ SepaMessageCustomField.ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).head.asString.value should be(originalSepaCreditTransferMessage.messageIdInSepaFile)
        _ <- (paymentReturnMessage.customFields.value \\ SepaMessageCustomField.ORIGINAL_MESSAGE_TYPE.toString).head.asString.value should be(originalSepaCreditTransferMessage.messageType.toString)
        sepaCreditTransferTransaction <- SepaCreditTransferTransaction.getById(originalSepaCreditTransferTransaction.id)
        _ <- sepaCreditTransferTransaction.status should be(SepaCreditTransferTransactionStatus.RETURNED)
        _ <- (sepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).head.asString.value should be(originalSepaCreditTransferMessage.messageIdInSepaFile)
        _ <- (sepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINAL_MESSAGE_TYPE.toString).head.asString.value should be(originalSepaCreditTransferMessage.messageType.toString)
        _ <- (sepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINATOR.toString).head.asString.value should be(paymentReturnFile.paymentReturnOriginatorBic.bic)
        _ <- (sepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_REASON_CODE.toString).head.asString.value should be(paymentReturnFile.paymentReturnReasonCode.toString)

        _ <- Future(And("the refund transaction should be integrated in the OBP-API"))
        sepaTransactionMessage <- SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(originalSepaCreditTransferTransaction.id, paymentReturnMessage.id)
        obpTransactionRefundId = sepaTransactionMessage.obpTransactionId.value
        obpTransactionRefund <- ObpApi.getTransactionById(ObpAccountTest.BANK_ID, ObpAccountTest.ACCOUNT_ID, obpTransactionRefundId)
        _ <- obpTransactionRefund.details.value.amount should be(originalTransactionAmount.toString())
        _ <- obpTransactionRefund.details.description should include(obpTransaction.id)
        _ <- obpTransactionRefund.details.description should include(paymentReturnFile.paymentReturnReasonCode.toString)
      } yield succeed
    }
  }
}
