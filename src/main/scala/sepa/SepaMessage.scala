package sepa

import java.time.LocalDateTime
import java.util.UUID

import model.enums.SepaMessageType.SepaMessageType

class SepaMessage(
                   val id: UUID,
                   val creationDateTime: LocalDateTime,
                   val messageType: SepaMessageType,
                   val content: Option[String],
                   val sepaFileId: Option[UUID],
                   val idInSepaFile: String
                 )
