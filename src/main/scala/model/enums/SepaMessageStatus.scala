package model.enums

import model.enums

object SepaMessageStatus extends Enumeration {
  type SepaMessageStatus = Value

  val PROCESSED: enums.SepaMessageStatus.Value = Value
  val UNPROCESSED: enums.SepaMessageStatus.Value = Value
  val PROCESSING_IN_PROGRESS: enums.SepaMessageStatus.Value = Value
  val PROCESSING_ERROR: enums.SepaMessageStatus.Value = Value
}