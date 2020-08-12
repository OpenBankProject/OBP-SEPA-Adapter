package model.enums

import model.enums


object SepaMessageType extends Enumeration {
  type SepaMessageType = Value

  /* CUSTOMER <==> BANK */
  val C2B_CREDIT_TRANSFER: SepaMessageType.Value = Value("pain.001.001.03")
  val B2C_PAYMENT_REJECT: SepaMessageType.Value = Value("pain.002.001.03")

  /* BANK <==> BANK */
  val B2B_CREDIT_TRANSFER: SepaMessageType.Value = Value("pacs.008.001.02")
  val B2B_PAYMENT_RETURN: SepaMessageType.Value = Value("pacs.004.001.02")
  val B2B_PAYMENT_REJECT: SepaMessageType.Value = Value("pacs.002.001.03")
  val B2B_PAYMENT_RECALL: SepaMessageType.Value = Value("camt.056.001.01")
  val B2B_PAYMENT_RECALL_NEGATIVE_ANSWER: SepaMessageType.Value = Value("camt.029.001.03") // Use a B2B_PAYMENT_RETURN for a positive answer

  val B2B_INQUIRY_CLAIM_NON_RECEIP: SepaMessageType.Value = Value("camt.027.001.06")
  val B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION: SepaMessageType.Value = Value("camt.087.001.05")

  val B2B_INQUIRY_RESPONSE: enums.SepaMessageType.Value = Value("camt.029.001.08") // This type must not be stored in database, must be used only to match on "camt.029.001.08"
  val B2B_INQUIRY_CLAIM_NON_RECEIP_POSITIVE_RESPONSE: SepaMessageType.Value = Value("camt.029.001.08:CLAIM_NON_RECEIP_POSITIVE_RESPONSE")
  val B2B_INQUIRY_CLAIM_NON_RECEIP_NEGATIVE_RESPONSE: SepaMessageType.Value = Value("camt.029.001.08:CLAIM_NON_RECEIP_NEGATIVE_RESPONSE")
  val B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION_POSITIVE_RESPONSE: SepaMessageType.Value = Value("camt.029.001.08:CLAIM_VALUE_DATE_CORRECTION_POSITIVE_RESPONSE")
  val B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION_NEGATIVE_RESPONSE: SepaMessageType.Value = Value("camt.029.001.08:CLAIM_VALUE_DATE_CORRECTION_NEGATIVE_RESPONSE")

  val B2B_REQUEST_STATUS_UPDATE: SepaMessageType.Value = Value("pacs.028.001.01")
}