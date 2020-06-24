package sepa.scheduler


import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID

import model.SepaFile
import model.enums.{SepaFileStatus, SepaFileType, SepaMessageType}
import sepa.CreditTransferMessage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scala.xml.XML

object ProcessIncomingFiles extends App {

  val fileName = "SEPA_SCT_OUT_pacs.008.001.02_2020-06-24_2.xml"

  val filesToProcess = Seq[File](new File(s"src/main/scala/sepa/$fileName"))

  filesToProcess.foreach(file => {
    val xmlFile = XML.loadFile(file)

    val sepaFile = SepaFile(
      id = UUID.randomUUID(),
      name = file.getName,
      path = Path.of(file.getPath),
      fileType = SepaFileType.SCT_IN,
      status = SepaFileStatus.PROCESSING_IN_PROGRESS,
      receiptDate = Some(LocalDateTime.now()),
      processedDate = None
    )

    Await.result(sepaFile.insert(), Duration.Inf)

    val messageType = SepaMessageType.withName(xmlFile.namespace.split(":").last)

    messageType match {
      case SepaMessageType.B2B_CREDIT_TRANSFER =>
        CreditTransferMessage.fromXML(xmlFile, UUID.randomUUID(), sepaFile.id) match {
          case Success(creditTransferMessage) =>
            val requests = for {
              _ <- creditTransferMessage.insert()
              _ <- Future(creditTransferMessage.creditTransferTransactions.map(_.insert()))
              _ <- Future(creditTransferMessage.creditTransferTransactions.map(t => t.linkMessage(creditTransferMessage.id, t.transactionIdInSepaFile)))
            } yield ()
            Await.result(requests, Duration.Inf)

          case Failure(exception) =>
            Await.result(sepaFile.copy(status = SepaFileStatus.PROCESSING_ERROR).update(), Duration.Inf)
            println(exception.getMessage)
        }
      case SepaMessageType.B2B_PAYMENT_REJECT =>
      case SepaMessageType.B2B_PAYMENT_RETURN =>
      case SepaMessageType.B2B_PAYMENT_RECALL =>
      case SepaMessageType.B2B_PAYMENT_RECALL_NEGATIVE_ANSWER =>
      case SepaMessageType.B2B_INQUIRY_CLAIM_NON_RECEIP =>
      case SepaMessageType.B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION =>
      case SepaMessageType.B2B_INQUIRY_RESPONSE =>
      case SepaMessageType.B2B_REQUEST_STATUS_UPDATE =>
    }

  }


  )

}
