package model.enums.sepaReasonCodes

object ClaimValueDateCorrectionResponseStatusCode extends Enumeration {
  type ClaimValueDateCorrectionResponseStatusCode = Value

  val ACCEPTED_VALUE_DATE_ADJUSTMENT = Value("ACVA")
  val MODIFIED_AS_PER_REQUEST = Value("MODI")
}