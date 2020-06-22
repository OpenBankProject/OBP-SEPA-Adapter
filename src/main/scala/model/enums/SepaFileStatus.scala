package model.enums

object SepaFileStatus extends Enumeration {
  type SepaFileStatus = Value

  val PROCESSED: SepaFileStatus.Value = Value
  val UNPROCESSED: SepaFileStatus.Value = Value
  val PROCESSING_IN_PROGRESS: SepaFileStatus.Value = Value
  val PROCESSING_ERROR: SepaFileStatus.Value = Value
}
