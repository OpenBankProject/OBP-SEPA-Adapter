package model.enums

object SepaFileType extends Enumeration {
  type SepaFileType = Value

  val SCT_IN: SepaFileType.Value = Value
  val SCT_OUT: SepaFileType.Value = Value

  val SDD_OUT: SepaFileType.Value = Value
  val SDD_IN: SepaFileType.Value = Value

  val SCT_INST_IN: SepaFileType.Value = Value
  val SCT_INST_OUT: SepaFileType.Value = Value
}