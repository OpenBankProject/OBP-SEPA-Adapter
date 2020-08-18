package model.enums

object SepaMessageCustomField extends Enumeration {
  type SepaMessageCustomField = Value

  val ORIGINAL_MESSAGE_ID_IN_SEPA_FILE = Value("originalMessageIdInSepaFile")
  val ORIGINAL_MESSAGE_TYPE = Value("originalMessageType")

  val ORIGINATOR = Value("originator")
  val REASON_CODE = Value("reasonCode")

  val CREDIT_TRANFER_SETTLEMENT_INFORMATION = Value("creditTransferSettlementInformation")
  val CREDIT_TRANFER_PAYMENT_TYPE_INFORMATION = Value("creditTransferPaymentTypeInformation")

  val PAYMENT_RETURN_SETTLEMENT_INFORMATION = Value("paymentReturnSettlementInformation")

  // TODO : remove this field : useless ?
  val PAYMENT_REJECT_GROUP_STATUS = Value("paymentRejectGroupStatus")

  val INQUIRY_CLAIM_NON_RECEIPT_CASE_ID = Value("inquiryClaimNonReceiptCaseId")
  val INQUIRY_CLAIM_NON_RECEIPT_CASE_CREATOR = Value("inquiryClaimNonReceiptCaseCreator")
  val INQUIRY_CLAIM_NON_RECEIPT_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE = Value("inquiryClaimNonReceiptOriginalMessageIdInSepaFile")
  val INQUIRY_CLAIM_NON_RECEIPT_ORIGINAL_MESSAGE_TYPE = Value("inquiryClaimNonReceiptOriginalMessageType")
  val INQUIRY_CLAIM_NON_RECEIPT_ADDITIONAL_INFORMATION = Value("inquiryClaimNonReceiptAdditionalInformation")

  val INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CASE_ID = Value("inquiryClaimNonReceiptResponseCaseId")
  val INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CASE_CREATOR = Value("inquiryClaimNonReceiptResponseCaseCreator")
  val INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_STATUS_CODE = Value("inquiryClaimNonReceiptResponseStatusCode")
  val INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_MODIFICATION_STATUS_ID = Value("inquiryClaimNonReceiptResponseModificationStatusId")
  val INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE = Value("inquiryClaimNonReceiptResponseOriginalMessageIdInSepaFile")
  val INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_MESSAGE_TYPE = Value("inquiryClaimNonReceiptResponseOriginalMessageType")
  val INQUIRY_CLAIM_NON_RECEIPT_ACCEPTED_DATE_PROCESSED = Value("inquiryClaimNonReceiptAcceptedDateProcessed")
  val INQUIRY_CLAIM_NON_RECEIPT_ACCEPTED_ORIGINAL_NEXT_AGENT = Value("inquiryClaimNonReceiptAcceptedOriginalNextAgent")
  val INQUIRY_CLAIM_NON_RECEIPT_ACCEPTED_CHARGES_AMOUNT = Value("inquiryClaimNonReceiptAcceptedChargesAmount")
  val INQUIRY_CLAIM_NON_RECEIPT_ACCEPTED_CHARGES_AGENT = Value("inquiryClaimNonReceiptAcceptedChargesAgent")
  val INQUIRY_CLAIM_NON_RECEIPT_REJECTED_REASON_CODE = Value("inquiryClaimNonReceiptRejectedReasonCode")

  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_CASE_ID = Value("inquiryClaimValueDateCorrectionCaseId")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_CASE_CREATOR = Value("inquiryClaimValueDateCorrectionCaseCreator")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE = Value("inquiryClaimValueDateCorrectionOriginalMessageIdInSepaFile")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ORIGINAL_MESSAGE_TYPE = Value("inquiryClaimValueDateCorrectionOriginalMessageType")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_NEW_SETTLEMENT_DATE = Value("inquiryClaimValueDateCorrectionNewSettlementDate")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ADDITIONAL_INFORMATION = Value("inquiryClaimValueDateCorrectionAdditionalInformation")

  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_RESPONSE_CASE_ID = Value("inquiryClaimValueDateCorrectionResponseCaseId")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_RESPONSE_CASE_CREATOR = Value("inquiryClaimValueDateCorrectionResponseCaseCreator")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_RESPONSE_STATUS_CODE = Value("inquiryClaimValueDateCorrectionResponseStatusCode")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_RESPONSE_MODIFICATION_STATUS_ID = Value("inquiryClaimValueDateCorrectionResponseModificationStatusId")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_RESPONSE_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE = Value("inquiryClaimValueDateCorrectionResponseOriginalMessageIdInSepaFile")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_RESPONSE_ORIGINAL_MESSAGE_TYPE = Value("inquiryClaimValueDateCorrectionResponseOriginalMessageType")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ACCEPTED_NEW_SETTLEMENT_DATE = Value("inquiryClaimValueDateCorrectionAcceptedNewSettlementDate")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ACCEPTED_COMPENSATION_AMOUNT = Value("inquiryClaimValueDateCorrectionAcceptedCompensationAmount")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ACCEPTED_COMPENSATION_DEBTOR_AGENT = Value("inquiryClaimValueDateCorrectionAcceptedCompensationDebtorAgent")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ACCEPTED_COMPENSATION_CREDITOR_AGENT = Value("inquiryClaimValueDateCorrectionAcceptedCompensationCreditorAgent")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ACCEPTED_COMPENSATION_REASON_CODE = Value("inquiryClaimValueDateCorrectionAcceptedCompensationReasonCode")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ACCEPTED_CHARGES_AMOUNT = Value("inquiryClaimValueDateCorrectionAcceptedChargesAmount")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ACCEPTED_CHARGES_AGENT = Value("inquiryClaimValueDateCorrectionAcceptedChargesAgent")

}