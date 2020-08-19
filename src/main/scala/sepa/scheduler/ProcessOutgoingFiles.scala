package sepa.scheduler

import java.nio.file.Path
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import model.enums.SepaMessageType.{SepaMessageType, _}
import model.enums._
import model.{SepaCreditTransferTransaction, SepaFile, SepaMessage}
import sepa.sct.message._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.xml.XML

/** Process all the messages with the status `UNPROCESSED` */
object ProcessOutgoingFiles extends App {

  val result = SepaMessage.getUnprocessed.flatMap(messages => Future.sequence(messages.map { message =>
    val outgoingFile = generateSepaFileFromSepaMessageType(message.messageType)
    val settlementDate = LocalDate.now.plusDays(1)

    for {
      updatedMessage <- Future(message.copy(status = SepaMessageStatus.PROCESSING_IN_PROGRESS, settlementDate = Some(settlementDate)))
      _ <- updatedMessage.update()
      sctMessage <- SepaCreditTransferTransaction.getBySepaMessageId(updatedMessage.id).map(transactions =>
        getSctMessageByMessageType(updatedMessage, transactions))
      _ <- Future(XML.save(outgoingFile.path.toString, sctMessage.toXML.head, "UTF-8", xmlDecl = true, null))
      _ <- outgoingFile.insert()
      _ <- Future.sequence(sctMessage.creditTransferTransactions.map(transaction =>
        getProcessedTransactionStatusByMessageType(sctMessage.message, transaction._1, Some(settlementDate)).update()))
      _ <- sctMessage.message.copy(sepaFileId = Some(outgoingFile.id), status = SepaMessageStatus.PROCESSED).update()
      _ <- outgoingFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
      _ <- Future(println(s"${sctMessage.creditTransferTransactions.length} transactions accounted in file ${outgoingFile.name}"))
    } yield ()
    // TODO : add an error handler
  }))

  Await.result(result, Duration.Inf)

  def generateSepaFileFromSepaMessageType(sepaMessageType: SepaMessageType): SepaFile = {
    val fileName = "SEPA" + "_" + SepaFileType.SCT_OUT + "_" + sepaMessageType + "_" + LocalDateTime.now.toString.replace(":", "-") + "_" + ".xml"
    SepaFile(
      id = UUID.randomUUID(),
      name = fileName,
      path = Path.of(s"src/main/scala/sepa/sct/file/out/$fileName"),
      fileType = SepaFileType.SCT_OUT,
      status = SepaFileStatus.PROCESSING_IN_PROGRESS,
      receiptDate = Some(LocalDateTime.now()),
      processedDate = None
    )
  }

  def getSctMessageByMessageType(sepaMessage: SepaMessage, sepaCreditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]): SctMessage[_] = {
    sepaMessage.messageType match {
      case B2B_CREDIT_TRANSFER => CreditTransferMessage(sepaMessage, sepaCreditTransferTransactions)
      case B2B_PAYMENT_RETURN => PaymentReturnMessage(sepaMessage, sepaCreditTransferTransactions)
      case B2B_PAYMENT_RECALL => PaymentRecallMessage(sepaMessage, sepaCreditTransferTransactions)
      case B2B_PAYMENT_RECALL_NEGATIVE_ANSWER => PaymentRecallNegativeAnswerMessage(sepaMessage, sepaCreditTransferTransactions)
      case B2B_INQUIRY_CLAIM_NON_RECEIP => InquiryClaimNonReceiptMessage(sepaMessage, sepaCreditTransferTransactions)
      case B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION => InquiryClaimValueDateCorrectionMessage(sepaMessage, sepaCreditTransferTransactions)
      case B2B_INQUIRY_CLAIM_NON_RECEIP_POSITIVE_RESPONSE => InquiryClaimNonReceiptPositiveAnswerMessage(sepaMessage, sepaCreditTransferTransactions)
      case B2B_INQUIRY_CLAIM_NON_RECEIP_NEGATIVE_RESPONSE => InquiryClaimNonReceiptNegativeAnswerMessage(sepaMessage, sepaCreditTransferTransactions)
      case B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION_POSITIVE_RESPONSE => InquiryClaimValueDateCorrectionPositiveAnswerMessage(sepaMessage, sepaCreditTransferTransactions)
      case B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION_NEGATIVE_RESPONSE => InquiryClaimValueDateCorrectionNegativeAnswerMessage(sepaMessage, sepaCreditTransferTransactions)
      case B2B_REQUEST_STATUS_UPDATE => RequestStatusUpdateMessage(sepaMessage, sepaCreditTransferTransactions)
    }
  }

  def getProcessedTransactionStatusByMessageType(sepaMessage: SepaMessage, sepaCreditTransferTransaction: SepaCreditTransferTransaction, settlementDate: Option[LocalDate]): SepaCreditTransferTransaction = {
    sepaMessage.messageType match {
      case B2B_CREDIT_TRANSFER => sepaCreditTransferTransaction.copy(status = SepaCreditTransferTransactionStatus.TRANSFERED, settlementDate = settlementDate)
      case B2B_PAYMENT_RETURN => sepaCreditTransferTransaction.copy(status = SepaCreditTransferTransactionStatus.RETURNED)
      case B2B_PAYMENT_RECALL => sepaCreditTransferTransaction.copy(status = SepaCreditTransferTransactionStatus.RECALLED)
      case B2B_PAYMENT_RECALL_NEGATIVE_ANSWER => sepaCreditTransferTransaction.copy(status = SepaCreditTransferTransactionStatus.RECALL_REJECTED)
      case B2B_INQUIRY_CLAIM_NON_RECEIP => sepaCreditTransferTransaction.copy(status = SepaCreditTransferTransactionStatus.CLAIMED_NON_RECEIPT)
      case B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION => sepaCreditTransferTransaction.copy(status = SepaCreditTransferTransactionStatus.CLAIMED_VALUE_DATE_CORRECTION)
      case B2B_INQUIRY_CLAIM_NON_RECEIP_POSITIVE_RESPONSE => sepaCreditTransferTransaction.copy(status = SepaCreditTransferTransactionStatus.CLAIM_NON_RECEIPT_ACCEPTED)
      case B2B_INQUIRY_CLAIM_NON_RECEIP_NEGATIVE_RESPONSE => sepaCreditTransferTransaction.copy(status = SepaCreditTransferTransactionStatus.CLAIM_NON_RECEIPT_REJECTED)
      case B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION_POSITIVE_RESPONSE => sepaCreditTransferTransaction.copy(status = SepaCreditTransferTransactionStatus.CLAIM_VALUE_DATE_CORRECTION_ACCEPTED)
      case B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION_NEGATIVE_RESPONSE => sepaCreditTransferTransaction.copy(status = SepaCreditTransferTransactionStatus.CLAIM_VALUE_DATE_CORRECTION_REJECTED)
      case B2B_REQUEST_STATUS_UPDATE => sepaCreditTransferTransaction.copy(status = SepaCreditTransferTransactionStatus.REQUESTED_STATUS_UPDATE)
    }
  }

}
