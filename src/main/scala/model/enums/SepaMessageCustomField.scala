package model.enums

object SepaMessageCustomField extends Enumeration {
  type SepaMessageCustomField = Value

  val ORIGINAL_SEPA_MESSAGE_ID = Value("originalSepaMessageId")
  val ORIGINAL_SEPA_MESSAGE_NAME_ID = Value("originalSepaMessageNameId")
}