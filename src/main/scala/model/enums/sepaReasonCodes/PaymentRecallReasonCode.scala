package model.enums.sepaReasonCodes

object PaymentRecallReasonCode extends Enumeration {
  type PaymentRecallReasonCode = Value

  val DUPLICATE_PAYMENT = Value("DUPL") // Duplicate Sending
  val TECHNICAL_PROBLEM = Value("TECH") // Technical problems resulting in erroneous SCTs
  val FRAUDULENT_ORIGIN = Value("FRAD") // Fraudulent originated SEPA Credit Transfer

}