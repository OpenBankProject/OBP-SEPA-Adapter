package model

import java.time.LocalDateTime
import java.util.UUID

import model.enums.SepaMessageType.SepaMessageType
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class SepaMessage(
                   val id: UUID,
                   val creationDateTime: LocalDateTime,
                   val messageType: SepaMessageType,
                   val content: Option[String],
                   val sepaFileId: Option[UUID],
                   val idInSepaFile: String
                 ) {
  def insert(): Future[Unit] = Schema.db.run(DBIOAction.seq(Schema.sepaMessages += this))
}

object SepaMessage {
  def unapply(sepaMessage: SepaMessage): Some[(UUID, LocalDateTime, SepaMessageType, Option[String], Option[UUID], String)] =
    Some(sepaMessage.id, sepaMessage.creationDateTime, sepaMessage.messageType, sepaMessage.content, sepaMessage.sepaFileId, sepaMessage.idInSepaFile)

  def tupled: ((UUID, LocalDateTime, SepaMessageType, Option[String], Option[UUID], String)) => SepaMessage = {
    case (id, creationDateTime, messageType, content, sepaFileId, idInSepaFile) => new SepaMessage(id, creationDateTime, messageType, content, sepaFileId, idInSepaFile)
  }

  def getById(id: UUID): Future[Seq[SepaMessage]] = Schema.db.run(Schema.sepaMessages.filter(_.id === id).result)

  def getUnprocessed: Future[Seq[SepaMessage]] = Schema.db.run(Schema.sepaMessages.filter(message => message.sepaFileId.isEmpty).result)

}
