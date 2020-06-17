package sepa

import generated.{Document, _}

import scala.xml.XML

object TestRead extends App {

  val xmlFile = XML.loadFile("src/main/scala/sepa/example.xml")
  val document = scalaxb.fromXML[Document](xmlFile)

  println(document.FIToFICstmrCdtTrf.CdtTrfTxInf.head.IntrBkSttlmAmt.value)
  println(document.FIToFICstmrCdtTrf.CdtTrfTxInf.head.CdtrAcct.head.Id.accountidentification4choicableoption.value)
  document.FIToFICstmrCdtTrf.CdtTrfTxInf.head.Cdtr.Nm.map(println)

}

