package model.enums.sepaReasonCodes

object PaymentRecallReasonCode extends Enumeration {
  type PaymentRecallReasonCode = Value

  val DUPLICATE_PAYMENT = Value("DUPL") // Duplicate Sending
  val TECHNICAL_PROBLEM = Value("TECH") // Technical problems resulting in erroneous SCTs
  val FRAUDULENT_ORIGIN = Value("FRAD") // Fraudulent originated SEPA Credit Transfer

  val REQUESTED_BY_CUSTOMER = Value("CUST") // By request of the Originator without anyreason specified
  val WRONG_AMOUNT = Value("AM09") // Wrong Amount
  val INVALID_CREDITOR_ACCOUNT_NUMBER = Value("AC03") // Account Number Wrong unique identifier of the Beneficiary account

}