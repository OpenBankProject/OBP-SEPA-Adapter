package model.enums

object SepaCreditTransferTransactionCustomField extends Enumeration {
  type SepaCreditTransferTransactionCustomField = Value

  val PAYMENT_RETURN_ORIGINATOR = Value("paymentReturnOriginator")
  val PAYMENT_RETURN_REASON_CODE = Value("paymentReturnReasonCode")
  val PAYMENT_RETURN_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE = Value("paymentReturnOriginalMessageIdInSepaFile")
  val PAYMENT_RETURN_ORIGINAL_MESSAGE_TYPE = Value("paymentReturnOriginalMessageType")

  val PAYMENT_REJECT_ORIGINATOR = Value("paymentRejectOriginator")
  val PAYMENT_REJECT_REASON_CODE = Value("paymentRejectReasonCode")
  val PAYMENT_REJECT_TRANSACTION_STATUS = Value("paymentRejectTransactionStatus")

  val PAYMENT_RECALL_ORIGINATOR = Value("paymentRecallOriginator")
  val PAYMENT_RECALL_REASON_CODE = Value("paymentRecallReasonCode")
  val PAYMENT_RECALL_ADDITIONAL_INFORMATION = Value("paymentRecallAdditionalInformation")
  val PAYMENT_RECALL_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE = Value("paymentRecallOriginalMessageIdInSepaFile")
  val PAYMENT_RECALL_ORIGINAL_MESSAGE_TYPE = Value("paymentRecallOriginalMessageType")
}
