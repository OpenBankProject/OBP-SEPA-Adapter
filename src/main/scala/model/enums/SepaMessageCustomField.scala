package model.enums

object SepaMessageCustomField extends Enumeration {
  type SepaMessageCustomField = Value

  val ORIGINAL_MESSAGE_ID_IN_SEPA_FILE = Value("originalMessageIdInSepaFile")
  val ORIGINAL_MESSAGE_TYPE = Value("originalMessageType")

  val ORIGINATOR = Value("originator")
  val REASON_CODE = Value("reasonCode")

  val PAYMENT_REJECT_GROUP_STATUS = Value("paymentRejectGroupStatus")

  val INQUIRY_CLAIM_NON_RECEIPT_CASE_ID = Value("inquiryClaimNonReceiptCaseId")
  val INQUIRY_CLAIM_NON_RECEIPT_CASE_CREATOR = Value("inquiryClaimNonReceiptCaseCreator")
  val INQUIRY_CLAIM_NON_RECEIPT_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE = Value("inquiryClaimNonReceiptOriginalMessageIdInSepaFile")
  val INQUIRY_CLAIM_NON_RECEIPT_ORIGINAL_MESSAGE_TYPE = Value("inquiryClaimNonReceiptOriginalMessageType")
  val INQUIRY_CLAIM_NON_RECEIPT_ADDITIONAL_INFORMATION = Value("inquiryClaimNonReceiptAdditionalInformation")

  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_CASE_ID = Value("inquiryClaimValueDateCorrectionCaseId")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_CASE_CREATOR = Value("inquiryClaimValueDateCorrectionCaseCreator")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE = Value("inquiryClaimValueDateCorrectionOriginalMessageIdInSepaFile")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ORIGINAL_MESSAGE_TYPE = Value("inquiryClaimValueDateCorrectionOriginalMessageType")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_NEW_SETTLEMENT_DATE = Value("inquiryClaimValueDateCorrectionNewSettlementDate")
  val INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ADDITIONAL_INFORMATION = Value("inquiryClaimValueDateCorrectionAdditionalInformation")
}