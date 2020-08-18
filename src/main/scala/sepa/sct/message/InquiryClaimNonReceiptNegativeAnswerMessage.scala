package sepa.sct.message

import java.time.{LocalDateTime, ZoneId}
import java.util.{GregorianCalendar, UUID}

import io.circe.{Json, JsonObject}
import javax.xml.datatype.DatatypeFactory
import model.enums._
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import scalaxb.DataRecord
import sepa.SepaUtil
import sepa.sct.generated.inquiryClaimNonReceiptNegativeAnswer._

import scala.util.Try
import scala.xml.{Elem, NodeSeq}

case class InquiryClaimNonReceiptNegativeAnswerMessage(
                                                        message: SepaMessage,
                                                        creditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]
                                                      ) extends SctMessage[Document] {
  def toXML: NodeSeq = {
    val document = Document(
      ResolutionOfInvestigationV08(
        Assgnmt = CaseAssignment4(
          Id = message.messageIdInSepaFile,
          Assgnr = message.instigatingAgent.map(_.bic).getOrElse("") match {
            case assigner if SepaUtil.isBic(assigner) => Party35Choice(DataRecord(<Agt></Agt>, BranchAndFinancialInstitutionIdentification5(FinancialInstitutionIdentification8(Some(assigner)))))
            case assigner => Party35Choice(DataRecord(<Pty></Pty>, PartyIdentification125(Nm = Some(assigner))))
          },
          Assgne = message.instigatedAgent.map(_.bic).getOrElse("") match {
            case assigner if SepaUtil.isBic(assigner) => Party35Choice(DataRecord(<Agt></Agt>, BranchAndFinancialInstitutionIdentification5(FinancialInstitutionIdentification8(Some(assigner)))))
            case assigner => Party35Choice(DataRecord(<Pty></Pty>, PartyIdentification125(Nm = Some(assigner))))
          },
          CreDtTm = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(message.creationDateTime.atZone(ZoneId.of("Europe/Paris"))))
        ),
        RslvdCase = Some(Case4(
          Id = creditTransferTransactions.head._2,
          Cretr = Party35Choice(DataRecord(<Agt></Agt>, BranchAndFinancialInstitutionIdentification5(FinancialInstitutionIdentification8(
            creditTransferTransactions.head._1.customFields.flatMap(json =>
              (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CASE_CREATOR.toString).headOption.flatMap(_.asString))
              .orElse(message.customFields.flatMap(json =>
                (json \\ SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CASE_CREATOR.toString).headOption.flatMap(_.asString)))
          ))))
        )),
        Sts = InvestigationStatus4Choice(
          DataRecord(<Conf></Conf>,
            creditTransferTransactions.head._1.customFields.flatMap(json =>
              (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_STATUS_CODE.toString).headOption.flatMap(_.asString))
              .orElse(message.customFields.flatMap(json =>
                (json \\ SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_STATUS_CODE.toString).headOption.flatMap(_.asString))).getOrElse("")
          )
        ),
        ModDtls = Some(PaymentTransaction90(
          ModStsId = creditTransferTransactions.head._1.customFields.flatMap(json =>
            (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_MODIFICATION_STATUS_ID.toString).headOption.flatMap(_.asString))
            .orElse(message.customFields.flatMap(json =>
              (json \\ SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_MODIFICATION_STATUS_ID.toString).headOption.flatMap(_.asString))),
          OrgnlGrpInf = OriginalGroupInformation29(
            OrgnlMsgId = creditTransferTransactions.head._1.customFields.flatMap(json =>
              (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).headOption.flatMap(_.asString))
              .orElse(message.customFields.flatMap(json =>
                (json \\ SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).headOption.flatMap(_.asString)))
              .getOrElse(""),
            OrgnlMsgNmId = creditTransferTransactions.head._1.customFields.flatMap(json =>
              (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_MESSAGE_TYPE.toString).headOption.flatMap(_.asString))
              .orElse(message.customFields.flatMap(json =>
                (json \\ SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_MESSAGE_TYPE.toString).headOption.flatMap(_.asString)))
              .getOrElse("")
          ),
          OrgnlTxId = Some(creditTransferTransactions.head._1.transactionIdInSepaFile),
          OrgnlTxRef = Some(OriginalTransactionReference27(
            DbtrAgt = creditTransferTransactions.head._1.debtorAgent.map(debtorAgent => BranchAndFinancialInstitutionIdentification5(FinInstnId = FinancialInstitutionIdentification8(BICFI = Some(debtorAgent.bic)))),
            CdtrAgt = creditTransferTransactions.head._1.creditorAgent.map(creditorAgent => BranchAndFinancialInstitutionIdentification5(FinInstnId = FinancialInstitutionIdentification8(BICFI = Some(creditorAgent.bic)))),
          ))

        )),
        ClmNonRctDtls = Some(ClaimNonReceipt1Choice(
          DataRecord(<Rjctd></Rjctd>, ClaimNonReceiptRejectReason1Choice(
            DataRecord(<Cd></Cd>, creditTransferTransactions.head._1.customFields.flatMap(json =>
              (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_REJECTED_REASON_CODE.toString).headOption.flatMap(_.asString))
              .orElse(message.customFields.flatMap(json =>
                (json \\ SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_REJECTED_REASON_CODE.toString).headOption.flatMap(_.asString)))
              .getOrElse(""))
          ))
        ))
      )
    )
    scalaxb.toXML[Document](document, "Document", defaultScope)
  }
}


object InquiryClaimNonReceiptNegativeAnswerMessage {
  def fromXML(xmlFile: Elem, sepaFileId: UUID): Try[InquiryClaimNonReceiptNegativeAnswerMessage] = {
    Try(scalaxb.fromXML[Document](xmlFile)).map(document =>
      InquiryClaimNonReceiptNegativeAnswerMessage(
        SepaMessage(
          id = UUID.randomUUID(),
          creationDateTime = document.RsltnOfInvstgtn.Assgnmt.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime,
          messageType = SepaMessageType.B2B_INQUIRY_CLAIM_NON_RECEIP_NEGATIVE_RESPONSE,
          status = SepaMessageStatus.PROCESSING_IN_PROGRESS,
          sepaFileId = Some(sepaFileId),
          messageIdInSepaFile = document.RsltnOfInvstgtn.Assgnmt.Id,
          numberOfTransactions = 1,
          totalAmount = 0,
          settlementDate = None,
          instigatingAgent = document.RsltnOfInvstgtn.Assgnmt.Assgnr.party35choicableoption.value match {
            case assigner: BranchAndFinancialInstitutionIdentification5 => assigner.FinInstnId.BICFI.map(Bic)
            case assigner: PartyIdentification125 => assigner.Nm.map(Bic)
          },
          instigatedAgent = document.RsltnOfInvstgtn.Assgnmt.Assgne.party35choicableoption.value match {
            case assignee: BranchAndFinancialInstitutionIdentification5 => assignee.FinInstnId.BICFI.map(Bic)
            case assignee: PartyIdentification125 => assignee.Nm.map(Bic)
          },
          customFields = Some(Json.fromJsonObject(JsonObject.empty
            .add(SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CASE_ID.toString,
              Json.fromString(document.RsltnOfInvstgtn.RslvdCase.map(_.Id).getOrElse("")))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CASE_CREATOR.toString,
              Json.fromString(document.RsltnOfInvstgtn.RslvdCase.map(_.Cretr.party35choicableoption.value).flatMap {
                case creator: BranchAndFinancialInstitutionIdentification5able => creator.FinInstnId.BICFI
              }.getOrElse("")))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_STATUS_CODE.toString,
              Json.fromString(document.RsltnOfInvstgtn.Sts.investigationstatus4choicableoption.value))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_MODIFICATION_STATUS_ID.toString,
              Json.fromString(document.RsltnOfInvstgtn.ModDtls.flatMap(_.ModStsId).getOrElse("")))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString,
              Json.fromString(document.RsltnOfInvstgtn.ModDtls.map(_.OrgnlGrpInf.OrgnlMsgId).getOrElse("")))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_MESSAGE_TYPE.toString,
              Json.fromString(document.RsltnOfInvstgtn.ModDtls.map(_.OrgnlGrpInf.OrgnlMsgNmId).getOrElse("")))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_REJECTED_REASON_CODE.toString,
              Json.fromString(document.RsltnOfInvstgtn.ModDtls.map(_.OrgnlGrpInf.OrgnlMsgNmId).getOrElse("")))
          ))
        ),
        creditTransferTransactions = Seq(
          (SepaCreditTransferTransaction(
            id = UUID.randomUUID(),
            amount = 0,
            debtor = None,
            debtorAccount = None,
            debtorAgent = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxRef.flatMap(
              _.DbtrAgt.flatMap(_.FinInstnId.BICFI.map(Bic)))),
            ultimateDebtor = None,
            creditor = None,
            creditorAccount = None,
            creditorAgent = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxRef.flatMap(
              _.CdtrAgt.flatMap(_.FinInstnId.BICFI.map(Bic)))),
            ultimateCreditor = None,
            purposeCode = None,
            description = None,
            creationDateTime = LocalDateTime.now(),
            settlementDate = None,
            transactionIdInSepaFile = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxId).getOrElse(""),
            instructionId = None,
            endToEndId = "",
            settlementInformation = None,
            paymentTypeInformation = None,
            status = SepaCreditTransferTransactionStatus.CLAIM_NON_RECEIPT_REJECTED,
            customFields = Some(Json.fromJsonObject(JsonObject.empty
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CASE_ID.toString,
                Json.fromString(document.RsltnOfInvstgtn.RslvdCase.map(_.Id).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CASE_CREATOR.toString,
                Json.fromString(document.RsltnOfInvstgtn.RslvdCase.map(_.Cretr.party35choicableoption.value).flatMap {
                  case creator: BranchAndFinancialInstitutionIdentification5able => creator.FinInstnId.BICFI
                }.getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_STATUS_CODE.toString,
                Json.fromString(document.RsltnOfInvstgtn.Sts.investigationstatus4choicableoption.value))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_MODIFICATION_STATUS_ID.toString,
                Json.fromString(document.RsltnOfInvstgtn.ModDtls.flatMap(_.ModStsId).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString,
                Json.fromString(document.RsltnOfInvstgtn.ModDtls.map(_.OrgnlGrpInf.OrgnlMsgId).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_MESSAGE_TYPE.toString,
                Json.fromString(document.RsltnOfInvstgtn.ModDtls.map(_.OrgnlGrpInf.OrgnlMsgNmId).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_REJECTED_REASON_CODE.toString,
                Json.fromString(document.RsltnOfInvstgtn.ModDtls.map(_.OrgnlGrpInf.OrgnlMsgNmId).getOrElse("")))
            ))
          ), document.RsltnOfInvstgtn.RslvdCase.map(_.Id).getOrElse("")))
      )
    )
  }
}

