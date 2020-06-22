package model

import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID

import model.enums.SepaFileStatus.SepaFileStatus
import model.enums.SepaFileType.SepaFileType
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

case class SepaFile(
                     id: UUID,
                     name: String,
                     path: Path,
                     fileType: SepaFileType,
                     status: SepaFileStatus,
                     receiptDate: Option[LocalDateTime],
                     processedDate: Option[LocalDateTime]
                   ) {
  def insert(): Future[Unit] = Schema.db.run(DBIOAction.seq(Schema.sepaFiles += this))
}

object SepaFile {
  def getById(id: UUID): Future[Seq[SepaFile]] = Schema.db.run(Schema.sepaFiles.filter(_.id === id).result)
}
