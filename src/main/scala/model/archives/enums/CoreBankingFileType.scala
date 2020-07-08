package model.archives.enums

import model.enums

object CoreBankingFileType extends Enumeration {
  type CoreBankingFileType = Value

  val SCT_IN_FILE = Value
  val SCT_OUT_FILE = Value

  val SDD_OUT_FILE = Value
  val SDD_IN_FILE = Value


  val CARD_CLEARING_FILE = Value

}
