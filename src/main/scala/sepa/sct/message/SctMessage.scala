package sepa.sct.message

import model.{SepaCreditTransferTransaction, SepaMessage}

import scala.xml.NodeSeq

abstract class SctMessage {

  val message: SepaMessage
  val creditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]

  def toXML: NodeSeq

}