package model.enums

object SepaCreditTransferTransactionCustomField extends Enumeration {
  type SepaCreditTransferTransactionCustomField = Value

  val PAYMENT_RETURN_ORIGINATOR = Value("paymentReturnOriginator")
  val PAYMENT_RETURN_REASON_CODE = Value("paymentReturnReasonCode")

  val PAYMENT_REJECT_ORIGINATOR = Value("paymentRejectOriginator")
  val PAYMENT_REJECT_REASON_CODE = Value("paymentRejectReasonCode")
  val PAYMENT_REJECT_TRANSACTION_STATUS = Value("paymentRejectTransactionStatus")
}
