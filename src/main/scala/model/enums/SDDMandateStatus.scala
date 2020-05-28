package model.enums

object SDDMandateStatus extends Enumeration {
  type SDDMandateStatus = Value

  val ACTIVE: SDDMandateStatus.Value = Value
  val SUSPENDED: SDDMandateStatus.Value = Value
  val DEACTIVATED: SDDMandateStatus.Value = Value
  val CLOSED: SDDMandateStatus.Value = Value
}
