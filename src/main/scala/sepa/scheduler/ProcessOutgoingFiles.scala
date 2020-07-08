package sepa.scheduler

import java.nio.file.Path
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import model.enums.SepaMessageType.SepaMessageType
import model.enums._
import model.{SepaCreditTransferTransaction, SepaFile, SepaMessage}
import sepa.{CreditTransferMessage, PaymentRecallMessage, PaymentReturnMessage}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.xml.XML


object ProcessOutgoingFiles extends App {

  val result = SepaMessage.getUnprocessed.flatMap(messages => Future.sequence(messages.map { message =>
    val outgoingFile = generateSepaFileFromSepaMessageType(message.messageType)

    message match {
      case message: SepaMessage if message.messageType == SepaMessageType.B2B_CREDIT_TRANSFER =>
        for {
          updatedMessage <- Future(message.copy(status = SepaMessageStatus.PROCESSING_IN_PROGRESS, settlementDate = Some(LocalDate.now().plusDays(1))))
          _ <- updatedMessage.update()
          creditTransferMessage <- SepaCreditTransferTransaction.getBySepaMessageId(updatedMessage.id).map(transactions => CreditTransferMessage(updatedMessage, transactions.map(_._1)))
          _ <- Future(XML.save(outgoingFile.path.toString, creditTransferMessage.toXML.head, "UTF-8", xmlDecl = true, null))
          _ <- outgoingFile.insert()
          _ <- Future.sequence(creditTransferMessage.creditTransferTransactions.map(_.copy(status = SepaCreditTransferTransactionStatus.PROCESSED).update()))
          _ <- creditTransferMessage.message.copy(sepaFileId = Some(outgoingFile.id), status = SepaMessageStatus.PROCESSED).update()
          _ <- outgoingFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          _ <- Future(println(s"${creditTransferMessage.creditTransferTransactions.length} transactions accounted in file ${outgoingFile.name}"))
        } yield ()


      case message: SepaMessage if message.messageType == SepaMessageType.B2B_PAYMENT_RETURN =>
        for {
          updatedMessage <- Future(message.copy(status = SepaMessageStatus.PROCESSING_IN_PROGRESS, settlementDate = Some(LocalDate.now().plusDays(1))))
          _ <- updatedMessage.update()
          paymentReturnMessage <- SepaCreditTransferTransaction.getBySepaMessageId(updatedMessage.id).map(transactions => PaymentReturnMessage(updatedMessage, transactions))
          _ <- Future(XML.save(outgoingFile.path.toString, paymentReturnMessage.toXML.head, "UTF-8", xmlDecl = true, null))
          _ <- outgoingFile.insert()
          _ <- Future.sequence(paymentReturnMessage.creditTransferTransactions.map(_._1.copy(status = SepaCreditTransferTransactionStatus.RETURNED).update()))
          _ <- paymentReturnMessage.message.copy(sepaFileId = Some(outgoingFile.id), status = SepaMessageStatus.PROCESSED).update()
          _ <- outgoingFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          _ <- Future(println(s"${paymentReturnMessage.creditTransferTransactions.length} transactions returned in file ${outgoingFile.name}"))
        } yield ()

      case message: SepaMessage if message.messageType == SepaMessageType.B2B_PAYMENT_RECALL =>
        for {
          updatedMessage <- Future(message.copy(status = SepaMessageStatus.PROCESSING_IN_PROGRESS))
          _ <- updatedMessage.update()
          paymentRecallMessage <- SepaCreditTransferTransaction.getBySepaMessageId(updatedMessage.id).map(transactions => PaymentRecallMessage(updatedMessage, transactions))
          _ <- Future(XML.save(outgoingFile.path.toString, paymentRecallMessage.toXML.head, "UTF-8", xmlDecl = true, null))
          _ <- outgoingFile.insert()
          _ <- Future.sequence(paymentRecallMessage.creditTransferTransactions.map(_._1.copy(status = SepaCreditTransferTransactionStatus.RECALLED).update()))
          _ <- paymentRecallMessage.message.copy(sepaFileId = Some(outgoingFile.id), status = SepaMessageStatus.PROCESSED).update()
          _ <- outgoingFile.copy(status = SepaFileStatus.PROCESSED, processedDate = Some(LocalDateTime.now())).update()
          _ <- Future(println(s"${paymentRecallMessage.creditTransferTransactions.length} transactions recalled in file ${outgoingFile.name}"))
        } yield ()

      case _ => Future(println("Unknow SEPA message type"))
    }
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
