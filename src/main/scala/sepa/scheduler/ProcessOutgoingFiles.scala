package sepa.scheduler

import java.nio.file.Path
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import model.enums.SepaMessageType.{SepaMessageType, _}
import model.enums._
import model.{SepaCreditTransferTransaction, SepaFile, SepaMessage}
import sepa.{CreditTransferMessage, PaymentRecallMessage, PaymentRecallNegativeAnswerMessage, PaymentReturnMessage, SctMessage}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.xml.XML


object ProcessOutgoingFiles extends App {

  val result = SepaMessage.getUnprocessed.flatMap(messages => Future.sequence(messages.map { message =>
    val outgoingFile = generateSepaFileFromSepaMessageType(message.messageType)
    val settlementDate = LocalDate.now.plusDays(1)

    for {
      updatedMessage <- Future(message.copy(status = SepaMessageStatus.PROCESSING_IN_PROGRESS, settlementDate = Some(settlementDate)))
      _ <- updatedMessage.update()
      sctMessage: SctMessage <- SepaCreditTransferTransaction.getBySepaMessageId(updatedMessage.id).map(transactions =>
        message.messageType match {
          case B2B_CREDIT_TRANSFER => CreditTransferMessage(updatedMessage, transactions)
          case B2B_PAYMENT_RETURN => PaymentReturnMessage(updatedMessage, transactions)
          case B2B_PAYMENT_RECALL => PaymentRecallMessage(updatedMessage, transactions)
          case B2B_PAYMENT_RECALL_NEGATIVE_ANSWER => PaymentRecallNegativeAnswerMessage(updatedMessage, transactions)
        })
      _ <- Future(XML.save(outgoingFile.path.toString, sctMessage.toXML.head, "UTF-8", xmlDecl = true, null))
      _ <- outgoingFile.insert()
      _ <- Future.sequence(sctMessage.creditTransferTransactions.map(transaction =>
        (message.messageType match {
          case B2B_CREDIT_TRANSFER => transaction._1.copy(status = SepaCreditTransferTransactionStatus.TRANSFERED, settlementDate = Some(settlementDate))
          case B2B_PAYMENT_RETURN => transaction._1.copy(status = SepaCreditTransferTransactionStatus.RETURNED)
          case B2B_PAYMENT_RECALL => transaction._1.copy(status = SepaCreditTransferTransactionStatus.RECALLED)
          case B2B_PAYMENT_RECALL_NEGATIVE_ANSWER => transaction._1.copy(status = SepaCreditTransferTransactionStatus.RECALL_REFUSED)
        }).update()))
      _ <- sctMessage.message.copy(sepaFileId = Some(outgoingFile.id), status = SepaMessageStatus.PROCESSED).update()
      _ <- outgoingFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
      _ <- Future(println(s"${sctMessage.creditTransferTransactions.length} transactions accounted in file ${outgoingFile.name}"))
    } yield ()
  }))

  Await.result(result, Duration.Inf)

  def generateSepaFileFromSepaMessageType(sepaMessageType: SepaMessageType): SepaFile = {
    val fileName = "SEPA" + "_" + SepaFileType.SCT_OUT + "_" + sepaMessageType + "_" + LocalDateTime.now.toString.replace(":", "-") + "_" + ".xml"
    SepaFile(
      id = UUID.randomUUID(),
      name = fileName,
      path = Path.of(s"src/main/scala/sepa/$fileName"),
      fileType = SepaFileType.SCT_OUT,
      status = SepaFileStatus.PROCESSING_IN_PROGRESS,
      receiptDate = Some(LocalDateTime.now()),
      processedDate = None
    )
  }
}
