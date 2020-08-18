package model.archives.enums

object TransactionType extends Enumeration {
  type TransactionType = Value

  /* SEPA Credit Transfer */
  val SCT_IN_TRANSFER: TransactionType.Value = Value
  val SCT_IN_REJECT: TransactionType.Value = Value
  val SCT_IN_RETURN: TransactionType.Value = Value
  val SCT_IN_RECALL: TransactionType.Value = Value

  val SCT_OUT_TRANSFER: TransactionType.Value = Value
  val SCT_OUT_REJECT: TransactionType.Value = Value
  val SCT_OUT_RETURN: TransactionType.Value = Value
  val SCT_OUT_RECALL: TransactionType.Value = Value

  /* Card Payment */
  val CARD_CHIP_PAYMENT: TransactionType.Value = Value
  val CARD_NFC_PAYMENT: TransactionType.Value = Value
  val CARD_INTERNET_PAYMENT: TransactionType.Value = Value
  val CARD_CLEARING: TransactionType.Value = Value
  val CARD_REFUND: TransactionType.Value = Value
  // ...
}
