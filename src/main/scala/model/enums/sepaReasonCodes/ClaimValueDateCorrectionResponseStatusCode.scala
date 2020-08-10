package model.enums.sepaReasonCodes

object ClaimValueDateCorrectionResponseStatusCode extends Enumeration {
  type ClaimValueDateCorrectionResponseStatusCode = Value

  val ACCEPTED_VALUE_DATE_ADJUSTMENT = Value("ACVA") // The claim for value date correction is accepted.
  val MODIFIED_AS_PER_REQUEST = Value("MODI") // The accepted claim for value date correction is confirmed.

  val CORRECT_VALUE_DATE_ALREADY_APPLIED = Value("CVAA") // The original value date was correct.
  val REJECTED_VALUE_DATE_ADJUSTMENT = Value("RJVA") // The claim for value date correction is rejected.
}