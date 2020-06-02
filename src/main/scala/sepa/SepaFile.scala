package sepa

import model.enums.CoreBankingFileType
import model.enums.CoreBankingFileType.CoreBankingFileType

import scala.xml._


class SepaFile(coreBankingFileType: CoreBankingFileType) {


  coreBankingFileType match {
    case CoreBankingFileType.SCT_OUT_FILE =>
  }

  def addMessage(sepaMessage: SepaMessage): Unit = {

  }

}
