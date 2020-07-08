package model.archives.enums

object TransactionStatus extends Enumeration {
  type TransactionStatus = Value

  /* SEPA */
  val PROCESSED: TransactionStatus.Value = Value
  val NOT_PROCESSED: TransactionStatus.Value = Value
  val PENDING: TransactionStatus.Value = Value

  /* Card */
  val CLEARED: TransactionStatus.Value = Value
  val NOT_CLEARED: TransactionStatus.Value = Value
  // ...
}
