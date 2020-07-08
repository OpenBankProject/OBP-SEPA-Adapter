package model.archives.enums

object CardPaymentStatus extends Enumeration {
  type CardPaymentStatus = Value

  val ACTIVE: CardPaymentStatus.Value = Value
  val DEACTIVATED: CardPaymentStatus.Value = Value
}
