import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import adapter.obpApiModel.{ChallengeAnswerJson400, ObpApi}
import com.openbankproject.commons.model._
import com.openbankproject.commons.model.enums.TransactionRequestStatus
import fileGeneration.{CreditTransferFileTest, PaymentRecallFileTest}
import model.enums._
import model.enums.sepaReasonCodes.{PaymentRecallReasonCode, PaymentReturnReasonCode}
import model.{SepaCreditTransferTransaction, SepaMessage, SepaTransactionMessage}
import org.scalatest.featurespec.AsyncFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{GivenWhenThen, OptionValues}
import sepa.SepaUtil
import sepa.scheduler.ProcessIncomingFilesActorSystem
import util.{ObpAccountTest, ObpCounterpartyTest}

import scala.concurrent.Future

// TODO : test the outgoing file generation

class PaymentRecallSpec extends AsyncFeatureSpec with GivenWhenThen with Matchers with OptionValues with AkkaActorSystemTest {

  Feature("Receive a recall for a received transaction from its originator") {
    Scenario("Receive a recall and send a positive answer to return the transaction") {
      for {
        _ <- Future(Given("an OBP account and an existing received transaction"))
        creditTransferFileTest = CreditTransferFileTest(
          messageIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID()),
          transactionAmount = BigDecimal(785),
          currentDateTime = LocalDateTime.now(),
          settlementDate = LocalDate.now().plusDays(1),
          transactionIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID()),
          debtorName = ObpCounterpartyTest.NAME,
          debtorIban = ObpCounterpartyTest.IBAN,
          debtorBic = ObpCounterpartyTest.BIC,
          creditorName = ObpAccountTest.ACCOUNT_HOLDER_NAME,
          creditorIban = ObpAccountTest.IBAN,
          creditorBic = ObpAccountTest.BIC,
          transactionDescription = "An original received transaction")
        filepath = creditTransferFileTest.write()
        _ <- Future(ProcessIncomingFilesActorSystem.main(Array(filepath.path)))
        _ <- Future(Thread.sleep(3000))
        sepaCreditTransferTransaction <- SepaCreditTransferTransaction.getByTransactionIdInSepaFile(creditTransferFileTest.transactionIdInSepaFile)
        sepaMessage <- SepaMessage.getByMessageIdInSepaFile(creditTransferFileTest.messageIdInSepaFile)
        sepaMessageTransaction <- SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(
          sepaCreditTransferTransaction.id, sepaMessage.id)
        _ <- sepaCreditTransferTransaction.status should be(SepaCreditTransferTransactionStatus.TRANSFERED)
        _ <- sepaMessageTransaction.obpTransactionId should not be None

        _ <- Future(When("we receive a payment recall request"))
        paymentRecallFileTest = PaymentRecallFileTest(
          messageIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID()),
          originalTransactionAmount = creditTransferFileTest.transactionAmount,
          currentDateTime = LocalDateTime.now(),
          originalSettlementDate = creditTransferFileTest.settlementDate,
          originalMessageIdInSepaFile = creditTransferFileTest.messageIdInSepaFile,
          paymentRecallIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID()),
          originalEndToEndId = creditTransferFileTest.transactionIdInSepaFile,
          originalTransactionIdInSepaFile = creditTransferFileTest.transactionIdInSepaFile,
          paymentRecallOriginatorName = creditTransferFileTest.debtorName,
          paymentRecallReasonCode = PaymentRecallReasonCode.REQUESTED_BY_CUSTOMER,
          paymentRecallAdditionalInformation = "I want to get my money back",
          originalDebtorName = creditTransferFileTest.debtorName,
          originalDebtorIban = creditTransferFileTest.debtorIban,
          originalDebtorBic = creditTransferFileTest.debtorBic,
          originalCreditorName = creditTransferFileTest.creditorName,
          originalCreditorIban = creditTransferFileTest.creditorIban,
          originalCreditorBic = creditTransferFileTest.creditorBic,
          originalTransactionDescription = creditTransferFileTest.transactionDescription
        )
        filepath = paymentRecallFileTest.write()
        _ <- Future(ProcessIncomingFilesActorSystem.main(Array(filepath.path)))
        _ <- Future(Thread.sleep(3000))

        _ <- Future(Then("the recall message should exist on the SEPA Adapter"))
        paymentRecallMessage <- SepaMessage.getBySepaCreditTransferTransactionId(sepaCreditTransferTransaction.id)
          .map(_.find(_.messageType == SepaMessageType.B2B_PAYMENT_RECALL).value)
        _ <- paymentRecallMessage.status should be(SepaMessageStatus.PROCESSED)
        _ <- paymentRecallMessage.messageIdInSepaFile should be(paymentRecallFileTest.messageIdInSepaFile)
        _ <- paymentRecallMessage.instigatingAgent.value should be(paymentRecallFileTest.originalDebtorBic)
        _ <- paymentRecallMessage.instigatedAgent.value should be(paymentRecallFileTest.originalCreditorBic)

        _ <- Future(And("the original transaction should be updated on the SEPA Adapter"))
        updatedSepaCreditTransferTransaction <- SepaCreditTransferTransaction.getById(sepaCreditTransferTransaction.id)
        _ <- updatedSepaCreditTransferTransaction.status should be(SepaCreditTransferTransactionStatus.RECALLED)
        _ <- (updatedSepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).head.asString.value should be(sepaMessage.messageIdInSepaFile)
        _ <- (updatedSepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ORIGINAL_MESSAGE_TYPE.toString).head.asString.value should be(SepaMessageType.B2B_CREDIT_TRANSFER.toString)
        _ <- (updatedSepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ORIGINATOR.toString).head.asString.value should be(paymentRecallFileTest.paymentRecallOriginatorName)
        _ <- (updatedSepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_REASON_CODE.toString).head.asString.value should be(paymentRecallFileTest.paymentRecallReasonCode.toString)
        _ <- (updatedSepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ADDITIONAL_INFORMATION.toString).head.asString.value should be(paymentRecallFileTest.paymentRecallAdditionalInformation)

        _ <- Future(Then("a refund transaction request should be created on the OBP-API"))
        recallTransactionMessage <- SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(sepaCreditTransferTransaction.id, paymentRecallMessage.id)
        obpRefundTransactionRequestId = recallTransactionMessage.obpTransactionRequestId.value
        obpRefundTransactionRequest <- ObpApi.getTransactionRequest(ObpAccountTest.BANK_ID, ObpAccountTest.ACCOUNT_ID, obpRefundTransactionRequestId)
        _ <- obpRefundTransactionRequest.`type` should be("REFUND")
        _ <- obpRefundTransactionRequest.transaction_ids should be(List(""))
        _ <- obpRefundTransactionRequest.status should be(TransactionRequestStatus.INITIATED.toString)
        _ <- obpRefundTransactionRequest.challenge.id should not be (None)
        _ <- obpRefundTransactionRequest.details.description should include(paymentRecallFileTest.paymentRecallAdditionalInformation)
        _ <- BigDecimal(obpRefundTransactionRequest.details.value.amount) should be(sepaCreditTransferTransaction.amount)
        transactionRequestAttributes <- ObpApi.getTransactionRequestAttributes(ObpAccountTest.BANK_ID, ObpAccountTest.ACCOUNT_ID, obpRefundTransactionRequestId)
        _ <- transactionRequestAttributes.transaction_request_attributes.find(_.name == "original_transaction_id").value.value should be(sepaMessageTransaction.obpTransactionId.value.value)
        _ <- transactionRequestAttributes.transaction_request_attributes.find(_.name == "refund_reason_code").value.value should be(PaymentRecallReasonCode.REQUESTED_BY_CUSTOMER.toString)
        // TODO: Add transaction request attribute creation on refund_additional_information in the code before uncommenting this test
        // _ <- transactionRequestAttributes.transaction_request_attributes.find(_.name == "refund_additional_information").value.value should be(paymentRecallFileTest.paymentRecallAdditionalInformation)

        _ <- Future(When("we send a positive answer to the refund transaction request"))
        refundTransactionRequestAfterChallenge <- ObpApi.answerTransactionRequestChallenge(
          ObpAccountTest.BANK_ID,
          ObpAccountTest.ACCOUNT_ID,
          TransactionRequestType("REFUND"),
          obpRefundTransactionRequestId,
          ChallengeAnswerJson400(obpRefundTransactionRequest.challenge.id.value, "123")
        )

        _ <- Future(Then("the refund transaction should be created on the OBP-API"))
        _ <- refundTransactionRequestAfterChallenge.transaction_ids should not be (List.empty)
        _ <- refundTransactionRequestAfterChallenge.status should be(TransactionRequestStatus.COMPLETED.toString)
        obpTransactionIdRefund = TransactionId(refundTransactionRequestAfterChallenge.transaction_ids.head)
        obpTransactionRefund <- ObpApi.getTransactionById(ObpAccountTest.BANK_ID, ObpAccountTest.ACCOUNT_ID, obpTransactionIdRefund)
        _ <- obpTransactionRefund.details.`type` should be("SEPA")
        _ <- BigDecimal(obpTransactionRefund.details.value.amount) should be(-sepaCreditTransferTransaction.amount)

        _ <- Future(And("the return message should be created on the SEPA Adapter"))
        returnMessage <- SepaMessage.getBySepaCreditTransferTransactionId(sepaCreditTransferTransaction.id)
          .map(_.find(_.messageType == SepaMessageType.B2B_PAYMENT_RETURN).value)
        _ <- returnMessage.status should be(SepaMessageStatus.UNPROCESSED)
        updatedSepaCreditTransferTransaction <- SepaCreditTransferTransaction.getById(sepaCreditTransferTransaction.id)
        _ <- updatedSepaCreditTransferTransaction.status should be(SepaCreditTransferTransactionStatus.TO_RETURN)
        _ <- (updatedSepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).head.asString.value should be(sepaMessage.messageIdInSepaFile)
        _ <- (updatedSepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINAL_MESSAGE_TYPE.toString).head.asString.value should be(SepaMessageType.B2B_CREDIT_TRANSFER.toString)
        _ <- (updatedSepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINATOR.toString).head.asString.value should be(sepaCreditTransferTransaction.creditor.value.name.value)
        _ <- (updatedSepaCreditTransferTransaction.customFields.value \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_REASON_CODE.toString).head.asString.value should be(PaymentReturnReasonCode.FOLLOWING_CANCELLATION_REQUEST.toString)

      } yield succeed
    }

    // TODO : Negative answer scenario

  }

  // TODO : Test the feature "send a recall request"

}
