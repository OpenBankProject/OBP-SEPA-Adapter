package sepa.sct.message

import java.time.{LocalDateTime, ZoneId}
import java.util.{GregorianCalendar, UUID}

import com.openbankproject.commons.model.Iban
import io.circe.{Json, JsonObject}
import javax.xml.datatype.DatatypeFactory
import model.enums._
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import scalaxb.DataRecord
import sepa.sct.generated.requestStatusUpdate._

import scala.util.Try
import scala.xml.{Elem, NodeSeq}

case class RequestStatusUpdateMessage(
                                       message: SepaMessage,
                                       creditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]
                                     ) extends SctMessage {
  def toXML: NodeSeq = {
    val document = Document(
      FIToFIPaymentStatusRequestV01(
        GrpHdr = GroupHeader53(
          MsgId = message.messageIdInSepaFile,
          CreDtTm = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(message.creationDateTime.atZone(ZoneId.of("Europe/Paris")))),
          InstgAgt = message.instigatingAgent.map(agent => BranchAndFinancialInstitutionIdentification5(FinancialInstitutionIdentification8(BICFI = Some(agent.bic)))),
          InstdAgt = message.instigatingAgent.map(agent => BranchAndFinancialInstitutionIdentification5(FinancialInstitutionIdentification8(BICFI = Some(agent.bic))))
        ),
        OrgnlGrpInf = message.customFields.flatMap(json =>
          for {
            originalSepaMessageId <- (json \\ SepaMessageCustomField.ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).headOption.flatMap(_.asString)
            originalSepaMessageNameId <- (json \\ SepaMessageCustomField.ORIGINAL_MESSAGE_TYPE.toString).headOption.flatMap(_.asString)
          } yield OriginalGroupInformation27(originalSepaMessageId, originalSepaMessageNameId)
        ).toSeq,
        TxInf = creditTransferTransactions.map(transaction =>
          PaymentTransaction73(
            StsReqId = Some(transaction._2),
            OrgnlInstrId = transaction._1.instructionId,
            OrgnlEndToEndId = Some(transaction._1.endToEndId),
            OrgnlTxId = Some(transaction._1.transactionIdInSepaFile),
            OrgnlTxRef = Some(OriginalTransactionReference24(
              IntrBkSttlmAmt = Some(ActiveOrHistoricCurrencyAndAmount(transaction._1.amount, Map(("@Ccy", DataRecord("EUR"))))),
              IntrBkSttlmDt = {
                transaction._1.settlementDate.map(date => {
                  val IntrBkSttlmDt = DatatypeFactory.newInstance().newXMLGregorianCalendar
                  IntrBkSttlmDt.setYear(date.getYear)
                  IntrBkSttlmDt.setMonth(date.getMonthValue)
                  IntrBkSttlmDt.setDay(date.getDayOfMonth)
                  IntrBkSttlmDt
                })
              },
              SttlmInf = None,
              PmtTpInf = None,
              RmtInf = transaction._1.description.map(description =>
                RemittanceInformation11(Ustrd = Seq(description))),
              Dbtr = transaction._1.debtorName.map(name =>
                PartyIdentification43(Nm = Some(name))),
              DbtrAcct = transaction._1.debtorAccount.map(account =>
                CashAccount24(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
              DbtrAgt = transaction._1.debtorAgent.map(debtorAgent =>
                BranchAndFinancialInstitutionIdentification5(FinInstnId = FinancialInstitutionIdentification8(BICFI = Some(debtorAgent.bic)))),
              Cdtr = transaction._1.creditorName.map(name =>
                PartyIdentification43(Nm = Some(name))),
              CdtrAcct = transaction._1.creditorAccount.map(account =>
                CashAccount24(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
              CdtrAgt = transaction._1.creditorAgent.map(creditorAgent =>
                BranchAndFinancialInstitutionIdentification5(FinInstnId = FinancialInstitutionIdentification8(BICFI = Some(creditorAgent.bic))))
            ))
          )
        )
      )
    )
    scalaxb.toXML[Document](document, "Document", defaultScope)
  }
}

object RequestStatusUpdateMessage {
  def fromXML(xmlFile: Elem, sepaFileId: UUID): Try[PaymentReturnMessage] = {
    Try(scalaxb.fromXML[Document](xmlFile)).map(document =>
      PaymentReturnMessage(
        SepaMessage(
          id = UUID.randomUUID(),
          creationDateTime = document.FIToFIPmtStsReq.GrpHdr.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime,
          messageType = SepaMessageType.B2B_REQUEST_STATUS_UPDATE,
          status = SepaMessageStatus.PROCESSING_IN_PROGRESS,
          sepaFileId = Some(sepaFileId),
          messageIdInSepaFile = document.FIToFIPmtStsReq.GrpHdr.MsgId,
          numberOfTransactions = 1,
          totalAmount = document.FIToFIPmtStsReq.TxInf.map(_.OrgnlTxRef.flatMap(_.IntrBkSttlmAmt.map(_.value)).getOrElse(BigDecimal(0))).sum,
          settlementDate = None,
          instigatingAgent = document.FIToFIPmtStsReq.GrpHdr.InstgAgt.flatMap(_.FinInstnId.BICFI.map(Bic)),
          instigatedAgent = document.FIToFIPmtStsReq.GrpHdr.InstdAgt.flatMap(_.FinInstnId.BICFI.map(Bic)),
          customFields = document.FIToFIPmtStsReq.OrgnlGrpInf.headOption.map(originalGroupInfo => Json.fromJsonObject(JsonObject.empty
            .add(SepaMessageCustomField.ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString, Json.fromString(originalGroupInfo.OrgnlMsgId))
            .add(SepaMessageCustomField.ORIGINAL_MESSAGE_TYPE.toString, Json.fromString(originalGroupInfo.OrgnlMsgNmId))))
        ),
        creditTransferTransactions = document.FIToFIPmtStsReq.TxInf.map(xmlTransaction => {
          val originalSepaCreditTransferTransaction = SepaCreditTransferTransaction(
            id = UUID.randomUUID(),
            amount = xmlTransaction.OrgnlTxRef.flatMap(_.IntrBkSttlmAmt.map(_.value)).getOrElse(0),
            debtorName = xmlTransaction.OrgnlTxRef.flatMap(_.Dbtr.flatMap(_.Nm)),
            debtorAccount = xmlTransaction.OrgnlTxRef.flatMap(_.DbtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString))),
            debtorAgent = xmlTransaction.OrgnlTxRef.flatMap(_.DbtrAgt.flatMap(agent => agent.FinInstnId.BICFI.map(Bic))),
            creditorName = xmlTransaction.OrgnlTxRef.flatMap(_.Cdtr.flatMap(_.Nm)),
            creditorAccount = xmlTransaction.OrgnlTxRef.flatMap(_.CdtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString))),
            creditorAgent = xmlTransaction.OrgnlTxRef.flatMap(_.CdtrAgt.flatMap(agent => agent.FinInstnId.BICFI.map(Bic))),
            purposeCode = None,
            description = xmlTransaction.OrgnlTxRef.flatMap(_.RmtInf.flatMap(_.Ustrd.headOption)),
            creationDateTime = LocalDateTime.now(),
            settlementDate = xmlTransaction.OrgnlTxRef.flatMap(_.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate)),
            transactionIdInSepaFile = xmlTransaction.OrgnlTxId.get,
            instructionId = xmlTransaction.OrgnlInstrId,
            endToEndId = xmlTransaction.OrgnlEndToEndId.get,
            status = SepaCreditTransferTransactionStatus.REQUESTED_STATUS_UPDATE,
            customFields = Some(Json.fromJsonObject(JsonObject.empty
              .add(SepaCreditTransferTransactionCustomField.REQUEST_STATUS_UPDATE_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString,
                Json.fromString(document.FIToFIPmtStsReq.OrgnlGrpInf.head.OrgnlMsgId))
              .add(SepaCreditTransferTransactionCustomField.REQUEST_STATUS_UPDATE_ORIGINAL_MESSAGE_TYPE.toString,
                Json.fromString(document.FIToFIPmtStsReq.OrgnlGrpInf.head.OrgnlMsgNmId))
            ))
          )
          (originalSepaCreditTransferTransaction, xmlTransaction.StsReqId.get)
        })
      )
    )
  }
}

