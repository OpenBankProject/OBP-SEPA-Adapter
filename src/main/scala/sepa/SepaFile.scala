package sepa

import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID

import model.enums.SepaFileStatus.SepaFileStatus
import model.enums.SepaFileType.SepaFileType

case class SepaFile(
                     id: UUID,
                     name: String,
                     path: Path,
                     fileType: SepaFileType,
                     status: SepaFileStatus,
                     receiptDate: Option[LocalDateTime],
                     processedDate: Option[LocalDateTime]
                   )
