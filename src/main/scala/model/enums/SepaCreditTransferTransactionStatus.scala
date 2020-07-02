package model.enums

object SepaCreditTransferTransactionStatus extends Enumeration {
  type SepaCreditTransferTransactionStatus = Value

  val PROCESSED: SepaCreditTransferTransactionStatus.Value = Value
  val UNPROCESSED: SepaCreditTransferTransactionStatus.Value = Value
  val PROCESSING_ERROR: SepaCreditTransferTransactionStatus.Value = Value
}
