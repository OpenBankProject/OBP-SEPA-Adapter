package model.enums

import model.enums

object SepaCreditTransferTransactionStatus extends Enumeration {
  type SepaCreditTransferTransactionStatus = Value

  val PROCESSED: SepaCreditTransferTransactionStatus.Value = Value
  val UNPROCESSED: SepaCreditTransferTransactionStatus.Value = Value
  val PROCESSING_ERROR: SepaCreditTransferTransactionStatus.Value = Value

  val TO_RETURN: enums.SepaCreditTransferTransactionStatus.Value = Value
  val RETURNED: enums.SepaCreditTransferTransactionStatus.Value = Value
}
