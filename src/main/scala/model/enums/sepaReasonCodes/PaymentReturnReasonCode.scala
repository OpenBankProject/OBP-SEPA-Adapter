package model.enums.sepaReasonCodes

object PaymentReturnReasonCode extends Enumeration {
  type PaymentReturnReasonCode = Value

  val INCORRECT_ACCOUNT_NUMBER = Value("AC01") // Account identifier invalid (i.e. invalid IBAN or account number does not exist)
  val CLOSED_ACCOUNT_NUMBER = Value("AC04") // Account closed
  val BLOCKED_ACCOUNT = Value("AC06") // Account blocked, reason not specified
  val TRANSACTION_FORBIDDEN = Value("AG01") // Credit transfer forbidden on this type of account (e.g. savings account)
  val INVALID_BANK_OPERATION_CODE = Value("AG02") // Operation/transaction code incorrect, invalid file format Usage Rule: To be used for incorrect ‘operation/transaction’ code or invalid file format.
  val DUPLICATION = Value("AM05") // Duplicate payment
  val MISSING_CREDITOR_ADDRESS = Value("BE04") // Account address invalid
  val CREDITOR_BANK_NOT_REGISTERED = Value("CNOR") // Creditor bank is not registered under this BIC in the CSM
  val ERI_OPTION_NOT_SUPPORTED = Value("ERIN") // The Extended Remittance Information (ERI) option is not supported
  val END_CUSTOMER_DECEASED = Value("MD07") // Beneficiary deceased
  val NOT_SPECIFIED_REASON_CUSTOMER_GENERATED = Value("MS02") // By order of the Beneficiary
  val NOT_SPECIFIED_REASON_AGENT_GENERATED = Value("MS03") // Reason not specified
  val BANK_IDENTIFIER_INCORRECT = Value("RC01") // Bank identifier incorrect, eg, invalid BIC
  val MISSING_DEBTOR_ACCOUNT_OR_IDENTIFICATION = Value("RR01") // Regulatory Reason
  val MISSING_DEBTOR_NAME_OR_ADDRESS = Value("RR02") // Regulatory Reason
  val MISSING_CREDITOR_NAME_OR_ADDRESS = Value("RR03") // Regulatory Reason
  val REGULATORY_REASON = Value("RR04") // Regulatory Reason

}