package sepa.scheduler

import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import model.SepaFile
import model.enums.{SepaFileStatus, SepaFileType, SepaMessageType}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.xml.XML

object ProcessIncomingFilesActorSystem extends App {

  val config = ConfigFactory.parseString(
    s"""
      akka.remote.netty.tcp.port=0
      """).withFallback(ConfigFactory.load())
  val systemName = "OBPSepaAdapterProcessIncomingFiles"
  val system = ActorSystem.create(systemName, config)
  val processIncomingFileActor = system.actorOf(Props.create(classOf[ProcessIncomingFileActor]), "obp-api-request-actor")

  val filesToProcess = Seq[File](
    new File(s"src/main/scala/sepa/SEPA_SCT_IN_pacs.008.001.02.xml")
  )

  filesToProcess.foreach(file => {
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

    val xmlFile = XML.loadFile(file)
    val messageType = SepaMessageType.withName(xmlFile.namespace.split(":").last)

    messageType match {
      case SepaMessageType.B2B_CREDIT_TRANSFER => processIncomingFileActor ! ProcessIncomingCreditTransferMessage(xmlFile, sepaFile)
      case SepaMessageType.B2B_PAYMENT_REJECT => processIncomingFileActor ! ProcessIncomingPaymentRejectMessage(xmlFile, sepaFile)
      case SepaMessageType.B2B_PAYMENT_RETURN => processIncomingFileActor ! ProcessIncomingPaymentReturnMessage(xmlFile, sepaFile)
      case SepaMessageType.B2B_PAYMENT_RECALL => processIncomingFileActor ! ProcessIncomingPaymentRecallMessage(xmlFile, sepaFile)
      case SepaMessageType.B2B_PAYMENT_RECALL_NEGATIVE_ANSWER =>
      case SepaMessageType.B2B_INQUIRY_CLAIM_NON_RECEIP =>
      case SepaMessageType.B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION =>
      case SepaMessageType.B2B_INQUIRY_RESPONSE =>
      case SepaMessageType.B2B_REQUEST_STATUS_UPDATE =>
      case _ => sys.error(s"Message received and can't be parsed, file path : ${file.getAbsolutePath}")
    }

  })

}
