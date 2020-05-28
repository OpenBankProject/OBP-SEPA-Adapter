package model.enums

object CoreBankingFileStatus extends Enumeration {
  type CoreBankingFileStatus = Value

  val PROCESSED: CoreBankingFileStatus.Value = Value
  val UNPROCESSED: CoreBankingFileStatus.Value = Value
  val PROCESSING_IN_PROGRESS: CoreBankingFileStatus.Value = Value
  val PROCESSING_ERROR: CoreBankingFileStatus.Value = Value
}
