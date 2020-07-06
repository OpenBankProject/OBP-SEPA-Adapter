package model.enums

object SepaCreditTransferTransactionCustomField extends Enumeration {
  type SepaCreditTransferTransactionCustomField = Value

  val PAYMENT_RETURN_ORIGINATOR = Value("paymentReturnOriginator")
  val PAYMENT_RETURN_REASON_CODE = Value("paymentReturnReasonCode")
}
