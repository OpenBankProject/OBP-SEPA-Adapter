package sepa.sct.message

import java.time.LocalDateTime
import java.util.UUID

import com.openbankproject.commons.model.Iban
import io.circe.{Json, JsonObject}
import model.enums._
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import sepa.sct.generated.paymentReject._

import scala.util.Try
import scala.xml.{Elem, NodeSeq}

case class PaymentRejectMessage(
                                 message: SepaMessage,
                                 creditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]
                               ) extends SctMessage {
  override def toXML: NodeSeq = ???
}

object PaymentRejectMessage {
  def fromXML(xmlFile: Elem, sepaFileId: UUID): Try[PaymentRejectMessage] = {
    Try(scalaxb.fromXML[Document](xmlFile)).map(document =>
      PaymentRejectMessage(
        SepaMessage(
          id = UUID.randomUUID(),
          creationDateTime = document.FIToFIPmtStsRpt.GrpHdr.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime,
          messageType = SepaMessageType.B2B_PAYMENT_REJECT,
          status = SepaMessageStatus.PROCESSING_IN_PROGRESS,
          sepaFileId = Some(sepaFileId),
          messageIdInSepaFile = document.FIToFIPmtStsRpt.GrpHdr.MsgId,
          numberOfTransactions = document.FIToFIPmtStsRpt.TxInfAndSts.length,
          totalAmount = document.FIToFIPmtStsRpt.TxInfAndSts.flatMap(_.OrgnlTxRef.flatMap(_.IntrBkSttlmAmt.map(_.value))).sum,
          settlementDate = None,
          instigatingAgent = document.FIToFIPmtStsRpt.GrpHdr.InstgAgt.flatMap(_.FinInstnId.BIC.map(Bic)),
          instigatedAgent = document.FIToFIPmtStsRpt.GrpHdr.InstdAgt.flatMap(_.FinInstnId.BIC.map(Bic)),
          customFields = Some(Json.fromJsonObject(JsonObject.empty
            .add(SepaMessageCustomField.ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString, Json.fromString(document.FIToFIPmtStsRpt.OrgnlGrpInfAndSts.OrgnlMsgId))
            .add(SepaMessageCustomField.ORIGINAL_MESSAGE_TYPE.toString, Json.fromString(document.FIToFIPmtStsRpt.OrgnlGrpInfAndSts.OrgnlMsgNmId))
            .add(SepaMessageCustomField.PAYMENT_REJECT_GROUP_STATUS.toString, Json.fromString(document.FIToFIPmtStsRpt.OrgnlGrpInfAndSts.GrpSts.map(_.toString).getOrElse("")))
            .add(SepaMessageCustomField.ORIGINATOR.toString, Json.fromString(document.FIToFIPmtStsRpt.OrgnlGrpInfAndSts.StsRsnInf.headOption.flatMap(_.Orgtr.flatMap(originator => originator.Nm.
              orElse(originator.Id.flatMap(_.party6choicableoption.value match {
                case ordId: OrganisationIdentification4 => ordId.BICOrBEI
              })))).getOrElse("")))
            .add(SepaMessageCustomField.REASON_CODE.toString,
              Json.fromString(document.FIToFIPmtStsRpt.OrgnlGrpInfAndSts.StsRsnInf.headOption.flatMap(_.Rsn.map(_.statusreason6choicableoption.value)).getOrElse("")))
          ))
        ),
        creditTransferTransactions = document.FIToFIPmtStsRpt.TxInfAndSts.map(xmlTransaction => {
          val originalSepaCreditTransferTransaction = SepaCreditTransferTransaction(
            UUID.randomUUID(), xmlTransaction.OrgnlTxRef.flatMap(_.IntrBkSttlmAmt.map(_.value)).getOrElse(0),
            xmlTransaction.OrgnlTxRef.flatMap(_.Dbtr.flatMap(_.Nm)),
            xmlTransaction.OrgnlTxRef.flatMap(_.DbtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString))),
            xmlTransaction.OrgnlTxRef.flatMap(_.DbtrAgt.flatMap(agent => agent.FinInstnId.BIC.map(Bic))),
            xmlTransaction.OrgnlTxRef.flatMap(_.Cdtr.flatMap(_.Nm)),
            xmlTransaction.OrgnlTxRef.flatMap(_.CdtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString))),
            xmlTransaction.OrgnlTxRef.flatMap(_.CdtrAgt.flatMap(agent => agent.FinInstnId.BIC.map(Bic))),
            None,
            xmlTransaction.OrgnlTxRef.flatMap(_.RmtInf.flatMap(_.Ustrd.headOption)),
            creationDateTime = LocalDateTime.now(),
            settlementDate = xmlTransaction.OrgnlTxRef.flatMap(_.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate)),
            transactionIdInSepaFile = xmlTransaction.OrgnlTxId.getOrElse(""),
            xmlTransaction.OrgnlInstrId, xmlTransaction.OrgnlEndToEndId.getOrElse(""),
            status = SepaCreditTransferTransactionStatus.REJECTED,
            customFields = Some(Json.fromJsonObject(JsonObject.empty
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_REJECT_ORIGINATOR.toString,
                Json.fromString(xmlTransaction.StsRsnInf.headOption.flatMap(_.Orgtr.flatMap(originator => originator.Nm.
                  orElse(originator.Id.flatMap(_.party6choicableoption.value match {
                    case ordId: OrganisationIdentification4 => ordId.BICOrBEI
                  })))).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_REJECT_REASON_CODE.toString,
                Json.fromString(xmlTransaction.StsRsnInf.headOption.flatMap(_.Rsn.map(_.statusreason6choicableoption.value)).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_REJECT_TRANSACTION_STATUS.toString,
                Json.fromString(xmlTransaction.TxSts.map(_.toString).getOrElse("")))
            ))
          )
          (originalSepaCreditTransferTransaction, xmlTransaction.StsId.getOrElse(""))
        })
      )
    )
  }
}









