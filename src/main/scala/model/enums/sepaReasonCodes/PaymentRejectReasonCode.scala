package model.enums.sepaReasonCodes

object PaymentRejectReasonCode extends Enumeration {
  type PaymentRejectReasonCode = Value

  val INCORRECT_ACCOUNT_NUMBER = Value("AC01") // Account identifier incorrect (i.e. invalid IBAN)
  val INVALID_BANK_OPERATION_CODE = Value("AG02") // Operation/transaction code incorrect, invalid file format Usage Rule: To be used for incorrect ‘operation/transaction’ code.
  val DUPLICATION = Value("AM05") // Duplicate payment
  val SETTLEMENT_FAILED = Value("ED05") // Settlement of the SEPA Credit Transfer has failed
  val ERI_OPTION_NOT_SUPPORTED = Value("ERIN") // The Extended Remittance Information (ERI) option is not supported
  val INVALID_FILE_FORMAT = Value("FF01") // Operation/transaction code incorrect, invalid file format
  val NOT_SPECIFIED_REASON_AGENT_GENERATED = Value("MS03") // Reason not specified
  val BANK_IDENTIFIER_INCORRECT = Value("RC01") // Bank identifier incorrect (i.e. invalid BIC)
  val MISSING_DEBTOR_ACCOUNT_OR_IDENTIFICATION = Value("RR01") // Regulatory Reason
  val MISSING_DEBTOR_NAME_OR_ADDRESS = Value("RR02") // Regulatory Reason
  val MISSING_CREDITOR_NAME_OR_ADDRESS = Value("RR03") // Regulatory Reason
  val REGULATORY_REASON = Value("RR04") // Regulatory Reason
  val CUT_OFF_TIME = Value("TM01") // File received after Cut-off Time
  val DEBTOR_BANK_NOT_REGISTERED = Value("DNOR") // Debtor bank is not registered under this BIC in the CSM
  val CREDITOR_BANK_NOT_REGISTERED = Value("CNOR") // Creditor bank is not registered under this BIC in the CSM

}