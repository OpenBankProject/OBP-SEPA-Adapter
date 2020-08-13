package sepa.sct.message

import model.{SepaCreditTransferTransaction, SepaMessage}

import scala.xml.NodeSeq

trait SctMessage[DocumentType] {

  val message: SepaMessage
  val creditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]

  implicit val documentType: DocumentType = documentType

  def toXML: NodeSeq
}