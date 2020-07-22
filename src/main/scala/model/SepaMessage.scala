package model

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import io.circe.Json
import model.Schema.{sepaMessageStatusColumnType, sepaMessageTypeColumnType}
import model.enums.SepaMessageStatus
import model.enums.SepaMessageStatus.SepaMessageStatus
import model.enums.SepaMessageType.SepaMessageType
import model.types.Bic
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SepaMessage(
                        id: UUID,
                        creationDateTime: LocalDateTime,
                        messageType: SepaMessageType,
                        status: SepaMessageStatus,
                        sepaFileId: Option[UUID],
                        messageIdInSepaFile: String,
                        numberOfTransactions: Int,
                        totalAmount: BigDecimal,
                        settlementDate: Option[LocalDate],
                        instigatingAgent: Option[Bic],
                        instigatedAgent: Option[Bic],
                        customFields: Option[Json]
                      ) {
  def insert(): Future[Unit] = Schema.db.run(DBIOAction.seq(Schema.sepaMessages += this))

  def update(): Future[Unit] = Schema.db.run(DBIOAction.seq(Schema.sepaMessages.filter(_.id === this.id).update(this)))
}

object SepaMessage {
  def getById(id: UUID): Future[SepaMessage] =
    Schema.db.run(Schema.sepaMessages.filter(_.id === id).result.headOption).flatMap {
      case Some(sepaMessage) => Future.successful(sepaMessage)
      case None => Future.failed(new Exception(s"None SepaMessage found with the id $id"))
    }

  def getByMessageIdInSepaFile(messageIdInSepaFile: String): Future[SepaMessage] =
    Schema.db.run(Schema.sepaMessages.filter(_.messageIdInSepaFile === messageIdInSepaFile).result.headOption).flatMap {
      case Some(sepaMessage) => Future.successful(sepaMessage)
      case None => Future.failed(new Exception(s"None SepaMessage found with the messageIdInSepaFile $messageIdInSepaFile"))
    }

  def getUnprocessed: Future[Seq[SepaMessage]] = Schema.db.run(Schema.sepaMessages.filter(_.status === SepaMessageStatus.UNPROCESSED).result)

  def getUnprocessedByType(messageType: SepaMessageType): Future[Seq[SepaMessage]] =
    Schema.db.run(
      Schema.sepaMessages.filter(message =>
        message.status === SepaMessageStatus.UNPROCESSED && message.messageType === messageType
      ).result
    )

  def getBySepaCreditTransferTransactionId(transactionId: UUID): Future[Seq[SepaMessage]] =
    Schema.db.run(
      Schema.sepaTransactionMessages
        .filter(_.sepaCreditTransferTransactionId === transactionId)
        .join(Schema.sepaMessages)
        .on((transactionMessage, message) => transactionMessage.sepaMessageId === message.id)
        .map(_._2)
        .result
    )

}
