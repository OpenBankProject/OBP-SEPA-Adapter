package model.archives.enums

object CardType extends Enumeration {
  type CardType = Value

  val DEBIT: CardType.Value = Value
  val CREDIT: CardType.Value = Value
  val PREPAID: CardType.Value = Value
}
