package model.enums

object SepaMessageCustomField extends Enumeration {
  type SepaMessageCustomField = Value

  val ORIGINAL_MESSAGE_ID_IN_SEPA_FILE = Value("originalMessageIdInSepaFile")
  val ORIGINAL_MESSAGE_TYPE = Value("originalMessageType")

  val ORIGINATOR = Value("originator")
  val REASON_CODE = Value("reasonCode")

  val PAYMENT_REJECT_GROUP_STATUS = Value("paymentRejectGroupStatus")
}