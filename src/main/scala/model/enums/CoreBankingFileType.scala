package model.enums

import model.enums

object CoreBankingFileType extends Enumeration {
  type CoreBankingFileType = Value

  val SCT_IN_FILE: enums.CoreBankingFileType.Value = Value
  val SCT_OUT_FILE: enums.CoreBankingFileType.Value = Value

  val SDD_OUT_FILE: enums.CoreBankingFileType.Value = Value
  val SDD_IN_FILE: enums.CoreBankingFileType.Value = Value


  val CARD_CLEARING_FILE: enums.CoreBankingFileType.Value = Value

}