package model

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import io.circe.Json
import model.enums.SepaMessageType.SepaMessageType
import model.types.Bic
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class SepaMessage(
                   val id: UUID,
                   val creationDateTime: LocalDateTime,
                   val messageType: SepaMessageType,
                   val sepaFileId: Option[UUID],
                   val messageIdInSepaFile: String,
                   val numberOfTransactions: Int,
                   val totalAmount: BigDecimal,
                   val settlementDate: Option[LocalDate],
                   val instigatingAgent: Option[Bic],
                   val instigatedAgent: Option[Bic],
                   val customFields: Option[Json]
                 ) {
  def insert(): Future[Unit] = Schema.db.run(DBIOAction.seq(Schema.sepaMessages += this))
}

object SepaMessage {
  def unapply(sepaMessage: SepaMessage): Some[(UUID, LocalDateTime, SepaMessageType, Option[UUID], String, Int, BigDecimal, Option[LocalDate], Option[Bic], Option[Bic], Option[Json])] =
    Some(sepaMessage.id, sepaMessage.creationDateTime, sepaMessage.messageType, sepaMessage.sepaFileId, sepaMessage.messageIdInSepaFile, sepaMessage.numberOfTransactions, sepaMessage.totalAmount, sepaMessage.settlementDate, sepaMessage.instigatingAgent, sepaMessage.instigatedAgent, sepaMessage.customFields)

  def tupled: ((UUID, LocalDateTime, SepaMessageType, Option[UUID], String, Int, BigDecimal, Option[LocalDate], Option[Bic], Option[Bic], Option[Json])) => SepaMessage = {
    case (id, creationDateTime, messageType, sepaFileId, messageIdInSepaFile, numberOfTransactions, totalAmount, settlementDate, instigatingAgent, instigatedAgent, customFields) =>
      new SepaMessage(id, creationDateTime, messageType, sepaFileId, messageIdInSepaFile, numberOfTransactions, totalAmount, settlementDate, instigatingAgent, instigatedAgent, customFields)
  }

  def getById(id: UUID): Future[Option[SepaMessage]] = Schema.db.run(Schema.sepaMessages.filter(_.id === id).result.headOption)

  def getUnprocessed: Future[Seq[SepaMessage]] = Schema.db.run(Schema.sepaMessages.filter(message => message.sepaFileId.isEmpty).result)

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
