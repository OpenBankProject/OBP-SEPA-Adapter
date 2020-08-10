package model.enums.sepaReasonCodes

object ClaimNonReceiptResponseStatusCode extends Enumeration {
  type ClaimNonReceiptResponseStatusCode = Value

  val ACCEPTED_CLAIM_NON_RECEIPT = Value("ACNR") // The claim for non-receipt of a payment instruction is accepted
  val REJECTED_CLAIM_NON_RECEIPT = Value("RJNR") // The claim for non-receipt of a payment instruction is rejected.
}