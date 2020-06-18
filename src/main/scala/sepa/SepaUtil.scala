package sepa

import java.util.UUID

import scala.util.Try

object SepaUtil {

  def removeDashesToUUID(uuid: UUID): String = uuid.toString.replace("-", "")

  def addDashesToUUID(uuid: String): Try[UUID] = {
    Try(
      UUID.fromString(
        uuid.substring(0, 8) + "-" +
          uuid.substring(8, 12) + "-" +
          uuid.substring(8, 12) + "-" +
          uuid.substring(12, 16) + "-" +
          uuid.substring(16, 20) + "-" +
          uuid.substring(20, 32)
      )
    )
  }

}
