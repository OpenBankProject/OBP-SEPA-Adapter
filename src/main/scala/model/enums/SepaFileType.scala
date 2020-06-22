package model.enums

object SepaFileType extends Enumeration {
  type SepaFileType = Value

  val SCT_IN_FILE: SepaFileType.Value = Value
  val SCT_OUT_FILE: SepaFileType.Value = Value

  val SDD_OUT_FILE: SepaFileType.Value = Value
  val SDD_IN_FILE: SepaFileType.Value = Value

  val SCT_INST_IN_FILE: SepaFileType.Value = Value
  val SCT_INST_OUT_FILE: SepaFileType.Value = Value
}