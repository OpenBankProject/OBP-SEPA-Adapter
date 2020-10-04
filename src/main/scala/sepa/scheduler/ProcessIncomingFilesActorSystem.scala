package sepa.scheduler

import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import model.SepaFile
import model.enums.sepaReasonCodes.{ClaimNonReceiptResponseStatusCode, ClaimValueDateCorrectionResponseStatusCode}
import model.enums.{SepaFileStatus, SepaFileType, SepaMessageType}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try
import scala.xml.XML

/** App to process incoming XML files
 *
 * Process all the files int the `filesToProcess` list
 */
object ProcessIncomingFilesActorSystem extends App {

  // Creation of the actor system
  val config = ConfigFactory.parseString(
    s"""
      akka.remote.netty.tcp.port=0
      """).withFallback(ConfigFactory.load())
  val systemName = "OBPSepaAdapterProcessIncomingFiles"
  val system = ActorSystem.create(systemName, config)
  val processIncomingFileActor = system.actorOf(Props.create(classOf[ProcessIncomingFileActor]), "obp-api-request-actor")

  // list of files that will be processed
  val filesToProcess = Seq[File](
    new File(s"src/main/scala/sepa/sct/file/in/SEPA_SCT_IN_pacs.008.001.02.xml")
  )

  filesToProcess.foreach(file => {

    // File creation and insertion in the database
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

    // We can then match on the file spacename and others attributes to determine the message type
    messageType match {
      case SepaMessageType.B2B_CREDIT_TRANSFER => processIncomingFileActor ! ProcessIncomingCreditTransferMessage(xmlFile, sepaFile)
      case SepaMessageType.B2B_PAYMENT_REJECT => processIncomingFileActor ! ProcessIncomingPaymentRejectMessage(xmlFile, sepaFile)
      case SepaMessageType.B2B_PAYMENT_RETURN => processIncomingFileActor ! ProcessIncomingPaymentReturnMessage(xmlFile, sepaFile)
      case SepaMessageType.B2B_PAYMENT_RECALL => processIncomingFileActor ! ProcessIncomingPaymentRecallMessage(xmlFile, sepaFile)
      case SepaMessageType.B2B_PAYMENT_RECALL_NEGATIVE_ANSWER => processIncomingFileActor ! ProcessIncomingPaymentRecallNegativeAnswerMessage(xmlFile, sepaFile)
      case SepaMessageType.B2B_INQUIRY_CLAIM_NON_RECEIP => processIncomingFileActor ! ProcessIncomingInquiryClaimNonReceiptMessage(xmlFile, sepaFile)
      case SepaMessageType.B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION => processIncomingFileActor ! ProcessIncomingInquiryClaimValueDateCorrectionMessage(xmlFile, sepaFile)
      case SepaMessageType.B2B_INQUIRY_RESPONSE =>
        (xmlFile \\ "Conf").headOption.map(_.text) match {
          case Some(statusCode) if Try(ClaimNonReceiptResponseStatusCode.withName(statusCode)).isSuccess => {
            val claimNonReceiptResponseStatusCode = ClaimNonReceiptResponseStatusCode.withName(statusCode)
            claimNonReceiptResponseStatusCode match {
              case ClaimNonReceiptResponseStatusCode.ACCEPTED_CLAIM_NON_RECEIPT => processIncomingFileActor ! ProcessIncomingInquiryClaimNonReceiptPositiveAnswerMessage(xmlFile, sepaFile)
              case ClaimNonReceiptResponseStatusCode.REJECTED_CLAIM_NON_RECEIPT => processIncomingFileActor ! ProcessIncomingInquiryClaimNonReceiptNegativeAnswerMessage(xmlFile, sepaFile)
            }
          }
          case Some(statusCode) if Try(ClaimValueDateCorrectionResponseStatusCode.withName(statusCode)).isSuccess => {
            val claimValueDateCorrectionResponseStatusCode = ClaimValueDateCorrectionResponseStatusCode.withName(statusCode)
            claimValueDateCorrectionResponseStatusCode match {
              case ClaimValueDateCorrectionResponseStatusCode.ACCEPTED_VALUE_DATE_ADJUSTMENT | ClaimValueDateCorrectionResponseStatusCode.MODIFIED_AS_PER_REQUEST =>
                processIncomingFileActor ! ProcessIncomingInquiryClaimValueDateCorrectionPositiveAnswerMessage(xmlFile, sepaFile)
              case ClaimValueDateCorrectionResponseStatusCode.CORRECT_VALUE_DATE_ALREADY_APPLIED | ClaimValueDateCorrectionResponseStatusCode.REJECTED_VALUE_DATE_ADJUSTMENT =>
                processIncomingFileActor ! ProcessIncomingInquiryClaimValueDateCorrectionNegativeAnswerMessage(xmlFile, sepaFile)
            }
          }
          case None => sys.error(s"B2B_INQUIRY_RESPONSE Message received but no status code match, file path : ${file.getAbsolutePath}")
        }
      case SepaMessageType.B2B_REQUEST_STATUS_UPDATE => processIncomingFileActor ! ProcessIncomingRequestStatusUpdateMessage(xmlFile, sepaFile)
      case _ => sys.error(s"Message received and can't be parsed, file path : ${file.getAbsolutePath}")
    }

  })

}
