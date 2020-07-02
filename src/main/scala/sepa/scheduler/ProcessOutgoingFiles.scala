package sepa.scheduler

import java.nio.file.Path
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import akka.actor.FSM.Failure
import model.enums._
import model.{SepaCreditTransferTransaction, SepaFile, SepaMessage}
import sepa.CreditTransferMessage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.xml.XML


object ProcessOutgoingFiles extends App {

  val sepaFileType = SepaFileType.SCT_OUT
  val sepaMessageType = SepaMessageType.B2B_CREDIT_TRANSFER
  val dayCycleNumber = 2
  val date = LocalDateTime.now()

  val fileName = "SEPA" + "_" + sepaFileType + "_" + sepaMessageType + "_" + date.toLocalDate.toString + "_" + dayCycleNumber + ".xml"

  val outgoingFile = SepaFile(
    id = UUID.randomUUID(),
    name = fileName,
    path = Path.of(s"src/main/scala/sepa/$fileName"),
    fileType = sepaFileType,
    status = SepaFileStatus.PROCESSING_IN_PROGRESS,
    receiptDate = Some(date),
    processedDate = Some(date)
  )

  val result = SepaMessage.getUnprocessedByType(SepaMessageType.B2B_CREDIT_TRANSFER).map(_.headOption).flatMap {
    case Some(message) =>
      val updatedMessage = message.copy(status = SepaMessageStatus.PROCESSING_IN_PROGRESS, settlementDate = Some(LocalDate.now().plusDays(1)))
      for {
        _ <- updatedMessage.update()
        creditTransferMessage <- SepaCreditTransferTransaction.getBySepaMessageId(updatedMessage.id).map(transactions =>
          CreditTransferMessage(updatedMessage, transactions.map(_._1)))
        _ <- Future(XML.save(outgoingFile.path.toString, creditTransferMessage.toXML.head, "UTF-8", xmlDecl = true, null))
        _ <- outgoingFile.insert()
        _ <- Future.sequence(creditTransferMessage.creditTransferTransactions.map(_.copy(status = SepaCreditTransferTransactionStatus.PROCESSED).update()))
        _ <- creditTransferMessage.message.copy(sepaFileId = Some(outgoingFile.id), status = SepaMessageStatus.PROCESSED).update()
        _ <- outgoingFile.copy(status = SepaFileStatus.PROCESSED).update()
        _ <- Future(println(s"${creditTransferMessage.creditTransferTransactions.length} transactions accounted in file $fileName"))
      } yield ()
    case None => Future.successful(println(s"No transactions to process for $sepaMessageType"))
  }

  Await.result(result, Duration.Inf)
}
