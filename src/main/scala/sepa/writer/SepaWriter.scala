package sepa.writer

import model.Schema.SepaTransactions
import model.SepaTransaction
import model.enums.CoreBankingFileType.CoreBankingFileType

import scala.concurrent.Future

import scala.xml._


object SepaWriter {

  def write(outputFileName: String, sepaFileType: CoreBankingFileType, sepaTransactions: Seq[SepaTransaction]): Unit = {

    val document = <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.03" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    </Document>

  }

  def createDocument() {}

  def addPayment(document: Node, sepaTransactions: Seq[SepaTransaction]): Unit = {

  }



}
