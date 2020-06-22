package sepa.scheduler

import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID

import model.enums.{SepaFileStatus, SepaFileType, SepaMessageType}
import model.{SepaCreditTransferTransaction, SepaFile}
import sepa.{CreditTransferMessage, SepaUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.xml.XML


object ProcessOutgoingFile extends App {

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
    status = SepaFileStatus.PROCESSED,
    receiptDate = None,
    processedDate = Some(date)
  )

  val creditTransferMessageId = UUID.randomUUID()

  val creditTransferMessage = SepaCreditTransferTransaction.getUnprocessed.flatMap(transactions =>
    transactions.length match {
      case _ if transactions.nonEmpty => Future.successful(CreditTransferMessage(
        id = creditTransferMessageId,
        creationDateTime = date,
        messageType = sepaMessageType,
        content = None,
        sepaFileId = Some(outgoingFile.id),
        idInSepaFile = SepaUtil.removeDashesToUUID(creditTransferMessageId),
        interbankSettlementDate = Some(date.toLocalDate.plusDays(1)),
        instigatingAgent = None,
        instigatedAgent = None,
        creditTransferTransactions = transactions
      ))
      case _ => Future.failed[CreditTransferMessage](new Exception("No transaction to process"))
    }
  )

  val waitable = for {
    sctMessage <- creditTransferMessage
    _ <- Future(XML.save(outgoingFile.path.toString, sctMessage.toXML.head, "UTF-8", xmlDecl = true, null))
    _ <- outgoingFile.insert()
    _ <- sctMessage.insert()
    _ <- Future(sctMessage.creditTransferTransactions.map(_.copy(sepaMessageId = Some(sctMessage.id)).update()))
    _ <- Future(println(s"${sctMessage.creditTransferTransactions.length} transactions accounted in file $fileName"))
  } yield ()

  Await.result(waitable, Duration.Inf)
}
