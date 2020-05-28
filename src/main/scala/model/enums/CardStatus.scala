package model.enums

object CardStatus extends Enumeration {
  type CardStatus = Value

  val ACTIVE: CardStatus.Value = Value
  val DEACTIVATED: CardStatus.Value = Value
  val UNACTIVATED: CardStatus.Value = Value
  val EXPIRED: CardStatus.Value = Value
  val OPPOSED: CardStatus.Value = Value
}
