package sepa

import java.util.UUID

import generated.`package`.defaultScope
import generated.{AccountIdentification4Choice, ActiveCurrencyAndAmount, BranchAndFinancialInstitutionIdentification4, CLRG, CashAccount16, CreditTransferTransactionInformation11, Document, FIToFICustomerCreditTransferV02, FinancialInstitutionIdentification7, GroupHeader33, PartyIdentification32, PaymentIdentification3, PaymentTypeInformation21, Purpose2Choice, RemittanceInformation5, SLEV, ServiceLevel8Choice, SettlementInformation13, _}
import javax.xml.datatype.DatatypeFactory
import scalaxb.DataRecord

import scala.xml.XML

object TestRead extends App {

  val xmlFile = XML.loadFile("src/main/scala/sepa/example.xml")
  val document = scalaxb.fromXML[Document](xmlFile)

  println(document.FIToFICstmrCdtTrf.CdtTrfTxInf.head.IntrBkSttlmAmt.value)
  println(document.FIToFICstmrCdtTrf.CdtTrfTxInf.head.CdtrAcct.head.Id.accountidentification4choicableoption.value)
  document.FIToFICstmrCdtTrf.CdtTrfTxInf.head.Cdtr.Nm.map(println)

}
