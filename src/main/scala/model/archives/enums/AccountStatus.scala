package model.archives.enums

object AccountStatus extends Enumeration {
  type AccountStatus = Value

  val ACTIVE: AccountStatus.Value = Value
  val CLOSED: AccountStatus.Value = Value
  val LOCKED: AccountStatus.Value = Value
}
