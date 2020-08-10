package model.enums.sepaReasonCodes

object ClaimNonReceiptRejectReasonCode extends Enumeration {
  type ClaimNonReceiptRejectReasonCode = Value

  val NO_ORIGINAL_TRANSACTION_RECEIVED = Value("NOOR")
  val ORIGINAL_TRANSACTION_RECEIVED_BUT_NOT_PROCESSABLE = Value("RNPR")
  val ALREADY_REJECTED_TRANSACTION = Value("ARJT")
  val ALREADY_RETURNED_TRANSACTION = Value("ARDT")
  val REGULATORY_REASON = Value("RR04")

}