package sepa

import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.util.{GregorianCalendar, UUID}

import com.openbankproject.commons.model.Iban
import io.circe.{Json, JsonObject}
import javax.xml.datatype.DatatypeFactory
import model.enums._
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import scalaxb.DataRecord
import sepa.sct.generated.inquiryClaimNonReceiptPositiveAnswer._

import scala.util.Try
import scala.xml.{Elem, NodeSeq}

// TODO : For all Messages, include custom fields in the class
case class InquiryClaimNonReceiptPositiveAnswerMessage(
                                                        message: SepaMessage,
                                                        creditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]
                                                      ) extends SctMessage {
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
          OrgnlInstrId = creditTransferTransactions.head._1.instructionId,
          OrgnlEndToEndId = Some(creditTransferTransactions.head._1.endToEndId),
          OrgnlTxId = Some(creditTransferTransactions.head._1.transactionIdInSepaFile),
          OrgnlTxRef = Some(OriginalTransactionReference27(
            IntrBkSttlmAmt = Some(ActiveOrHistoricCurrencyAndAmount(value = creditTransferTransactions.head._1.amount, Map(("@Ccy", DataRecord("EUR"))))),
            IntrBkSttlmDt = {
              creditTransferTransactions.headOption.flatMap(_._1.settlementDate.map(date => {
                val settlementDate = DatatypeFactory.newInstance().newXMLGregorianCalendar
                settlementDate.setYear(date.getYear)
                settlementDate.setMonth(date.getMonthValue)
                settlementDate.setDay(date.getDayOfMonth)
                settlementDate
              }))
            },
            SttlmInf = None,
            PmtTpInf = None,
            RmtInf = creditTransferTransactions.head._1.description.map(description => RemittanceInformation15(Ustrd = Seq(description))),
            Dbtr = creditTransferTransactions.head._1.debtorName.map(name => Party35Choice(DataRecord(<Pty></Pty>, PartyIdentification125(Nm = Some(name))))),
            DbtrAcct = creditTransferTransactions.head._1.debtorAccount.map(account => CashAccount24(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
            DbtrAgt = creditTransferTransactions.head._1.debtorAgent.map(debtorAgent => BranchAndFinancialInstitutionIdentification5(FinInstnId = FinancialInstitutionIdentification8(BICFI = Some(debtorAgent.bic)))),
            Cdtr = creditTransferTransactions.head._1.creditorName.map(name => Party35Choice(DataRecord(<Pty></Pty>, PartyIdentification125(Nm = Some(name))))),
            CdtrAcct = creditTransferTransactions.head._1.creditorAccount.map(account => CashAccount24(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
            CdtrAgt = creditTransferTransactions.head._1.creditorAgent.map(creditorAgent => BranchAndFinancialInstitutionIdentification5(FinInstnId = FinancialInstitutionIdentification8(BICFI = Some(creditorAgent.bic)))),
            Purp = creditTransferTransactions.head._1.purposeCode.map(purposeCode => Purpose2Choice(DataRecord(<Cd></Cd>, purposeCode)))
          ))

        )),
        ClmNonRctDtls = Some(ClaimNonReceipt1Choice(
          DataRecord(<Accptd></Accptd>, ClaimNonReceipt1(
            DtPrcd = creditTransferTransactions.head._1.customFields.flatMap(json =>
              (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_DATE_PROCESSED.toString).headOption.flatMap(_.asString)
                .flatMap(dateString => Try {
                  val date = LocalDate.parse(dateString)
                  val newSettlementDate = DatatypeFactory.newInstance().newXMLGregorianCalendar
                  newSettlementDate.setYear(date.getYear)
                  newSettlementDate.setMonth(date.getMonthValue)
                  newSettlementDate.setDay(date.getDayOfMonth)
                  newSettlementDate
                }.toOption))
              .orElse(message.customFields.flatMap(json =>
                (json \\ SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_DATE_PROCESSED.toString).headOption.flatMap(_.asString).flatMap(dateString => Try {
                  val date = LocalDate.parse(dateString)
                  val newSettlementDate = DatatypeFactory.newInstance().newXMLGregorianCalendar
                  newSettlementDate.setYear(date.getYear)
                  newSettlementDate.setMonth(date.getMonthValue)
                  newSettlementDate.setDay(date.getDayOfMonth)
                  newSettlementDate
                }.toOption))
              ).get,
            OrgnlNxtAgt = BranchAndFinancialInstitutionIdentification5(FinancialInstitutionIdentification8(BICFI =
              creditTransferTransactions.head._1.customFields.flatMap(json =>
                (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_NEXT_AGENT.toString).headOption.flatMap(_.asString))
                .orElse(message.customFields.flatMap(json =>
                  (json \\ SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_NEXT_AGENT.toString).headOption.flatMap(_.asString)))
            ))
          ))
        )),
        RsltnRltdInf = creditTransferTransactions.head._1.customFields.flatMap(json =>
          (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CHARGES_AMOUNT.toString).headOption.flatMap(_.asString))
          .orElse(message.customFields.flatMap(json =>
            (json \\ SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CHARGES_AMOUNT.toString).headOption.flatMap(_.asString)))
          .map(chargesAmountString => BigDecimal(chargesAmountString))
          .map(chargesAmount => ResolutionInformation2(Chrgs = Seq(Charges2(
            Amt = ActiveOrHistoricCurrencyAndAmount(value = chargesAmount, Map(("@Ccy", DataRecord("EUR")))),
            Agt = BranchAndFinancialInstitutionIdentification5(FinancialInstitutionIdentification8(BICFI =
              creditTransferTransactions.head._1.customFields.flatMap(json =>
                (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CHARGES_AGENT.toString).headOption.flatMap(_.asString))
                .orElse(message.customFields.flatMap(json =>
                  (json \\ SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CHARGES_AGENT.toString).headOption.flatMap(_.asString)))
            )))
          )))
      )
    )
    scalaxb.toXML[Document](document, "Document", defaultScope)
  }
}


object InquiryClaimNonReceiptPositiveAnswerMessage {
  def fromXML(xmlFile: Elem, sepaFileId: UUID): Try[InquiryClaimNonReceiptPositiveAnswerMessage] = {
    Try(scalaxb.fromXML[Document](xmlFile)).map(document =>
      InquiryClaimNonReceiptPositiveAnswerMessage(
        SepaMessage(
          id = UUID.randomUUID(),
          creationDateTime = document.RsltnOfInvstgtn.Assgnmt.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime,
          messageType = SepaMessageType.B2B_INQUIRY_CLAIM_NON_RECEIP_POSITIVE_RESPONSE,
          status = SepaMessageStatus.PROCESSING_IN_PROGRESS,
          sepaFileId = Some(sepaFileId),
          messageIdInSepaFile = document.RsltnOfInvstgtn.Assgnmt.Id,
          numberOfTransactions = 1,
          totalAmount = document.RsltnOfInvstgtn.ModDtls.head.OrgnlTxRef.flatMap(_.IntrBkSttlmAmt.map(_.value)).getOrElse(0),
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
            .add(SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_DATE_PROCESSED.toString,
              Json.fromString(document.RsltnOfInvstgtn.ClmNonRctDtls
                .map(_.claimnonreceipt1choicableoption.value.DtPrcd.toGregorianCalendar.toZonedDateTime.toLocalDate.toString).getOrElse("")))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_NEXT_AGENT.toString,
              Json.fromString(document.RsltnOfInvstgtn.ClmNonRctDtls.flatMap(_.claimnonreceipt1choicableoption.value.OrgnlNxtAgt.FinInstnId.BICFI).getOrElse("")))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CHARGES_AMOUNT.toString,
              Json.fromString(document.RsltnOfInvstgtn.RsltnRltdInf.flatMap(_.Chrgs.headOption.map(_.Amt.value.toString)).getOrElse("")))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CHARGES_AGENT.toString,
              Json.fromString(document.RsltnOfInvstgtn.ModDtls.map(_.OrgnlGrpInf.OrgnlMsgNmId).getOrElse("")))
          ))
        ),
        creditTransferTransactions = Seq(
          (SepaCreditTransferTransaction(
            id = UUID.randomUUID(),
            amount = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxRef.flatMap(_.IntrBkSttlmAmt.map(_.value))).getOrElse(0),
            debtorName = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxRef.flatMap(_.Dbtr.flatMap(_.party35choicableoption.value match {
              case debtor: PartyIdentification125able => debtor.Nm
            }))),
            debtorAccount = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxRef.flatMap(
              _.DbtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString)))),
            debtorAgent = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxRef.flatMap(
              _.DbtrAgt.flatMap(_.FinInstnId.BICFI.map(Bic)))),
            creditorName = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxRef.flatMap(_.Cdtr.flatMap(_.party35choicableoption.value match {
              case creditor: PartyIdentification125able => creditor.Nm
            }))),
            creditorAccount = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxRef.flatMap(
              _.CdtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString)))),
            creditorAgent = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxRef.flatMap(
              _.CdtrAgt.flatMap(_.FinInstnId.BICFI.map(Bic)))),
            purposeCode = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxRef.flatMap(_.Purp.map(_.purpose2choiceoption.value))),
            description = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxRef.flatMap(_.RmtInf.map(_.Ustrd.mkString))),
            creationDateTime = LocalDateTime.now(),
            settlementDate = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxRef.flatMap(
              _.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate))),
            transactionIdInSepaFile = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlTxId).getOrElse(""),
            instructionId = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlInstrId),
            endToEndId = document.RsltnOfInvstgtn.ModDtls.flatMap(_.OrgnlEndToEndId).getOrElse(""),
            status = SepaCreditTransferTransactionStatus.CLAIM_NON_RECEIPT_ACCEPTED,
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
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_DATE_PROCESSED.toString,
                Json.fromString(document.RsltnOfInvstgtn.ClmNonRctDtls
                  .map(_.claimnonreceipt1choicableoption.value.DtPrcd.toGregorianCalendar.toZonedDateTime.toLocalDate.toString).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_ORIGINAL_NEXT_AGENT.toString,
                Json.fromString(document.RsltnOfInvstgtn.ClmNonRctDtls.flatMap(_.claimnonreceipt1choicableoption.value.OrgnlNxtAgt.FinInstnId.BICFI).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CHARGES_AMOUNT.toString,
                Json.fromString(document.RsltnOfInvstgtn.RsltnRltdInf.flatMap(_.Chrgs.headOption.map(_.Amt.value.toString)).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_NON_RECEIPT_RESPONSE_CHARGES_AGENT.toString,
                Json.fromString(document.RsltnOfInvstgtn.ModDtls.map(_.OrgnlGrpInf.OrgnlMsgNmId).getOrElse("")))
            ))
          ), document.RsltnOfInvstgtn.RslvdCase.map(_.Id).getOrElse("")))
      )
    )
  }
}

