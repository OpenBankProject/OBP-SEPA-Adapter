package sepa

import model.SepaTransaction
import model.enums.CoreBankingFileType
import model.enums.CoreBankingFileType.CoreBankingFileType
import model.enums.SepaMessageType.SepaMessageType


import scala.xml._


case class SepaMessage(message: Node, messageType: SepaMessageType) {

  def addMessageRoot: Unit = {

    val document = <Document></Document>

  }

  def addPayment(document: Node, sepaTransactions: Seq[SepaTransaction]): Unit = {

  }

  def addTransaction(): Unit = {

  }

}

object SepaMessage {

  def load(message: Node): Unit = {

  }




}
