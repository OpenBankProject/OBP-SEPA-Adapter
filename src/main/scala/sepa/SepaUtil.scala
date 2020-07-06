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

  def isIban(iban: String): Boolean = iban.matches("[A-Z]{2,2}[0-9]{2,2}[a-zA-Z0-9]{1,30}")

  def isBic(bic: String): Boolean = bic.matches("[A-Z]{6,6}[A-Z2-9][A-NP-Z0-9]([A-Z0-9]{3,3}){0,1}")
}
