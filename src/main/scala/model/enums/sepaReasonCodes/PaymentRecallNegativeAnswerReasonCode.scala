package model.enums.sepaReasonCodes

object PaymentRecallNegativeAnswerReasonCode extends Enumeration {
  type PaymentRecallNegativeAnswerReasonCode = Value

  val CUSTOMER_DECISION = Value("CUST") // Beneficiary’s Refusal
  val LEGAL_DECISION = Value("LEGL") // Legal reasons
  val ALREADY_RETURNED_TRANSACTION = Value("ARDT") // The transaction has already been returned
  val CLOSED_ACCOUNT_NUMBER = Value("AC04") // Account closed
  val INSUFFICIENT_FUNDS = Value("AM04") // Insufficient funds on the account
  val NO_ANSWER_FROM_CUSTOMER = Value("NOAS") // No response from Beneficiary
  val NO_ORIGINAL_TRANSACTION_RECEIVED = Value("NOOR") // Initial SEPA Credit Transfer Transaction never received

}