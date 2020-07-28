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
import sepa.sct.generated.inquiryClaimValueDateCorrection._

import scala.util.Try
import scala.xml.{Elem, NodeSeq}

case class InquiryClaimValueDateCorrection(
                                            message: SepaMessage,
                                            creditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]
                                          ) extends SctMessage {
  def toXML: NodeSeq = {
    val document = Document(
      RequestToModifyPaymentV05(
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
        Case = Some(Case4(
          Id = creditTransferTransactions.head._2,
          Cretr = Party35Choice(DataRecord(<Agt></Agt>, BranchAndFinancialInstitutionIdentification5(FinancialInstitutionIdentification8(
            creditTransferTransactions.head._1.customFields.flatMap(json =>
              (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_CASE_CREATOR.toString).headOption.flatMap(_.asString))
              .orElse(message.customFields.flatMap(json =>
                (json \\ SepaMessageCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_CASE_CREATOR.toString).headOption.flatMap(_.asString)))
          ))))
        )),
        Undrlyg = UnderlyingTransaction4Choice(
          underlyingtransaction4choicableoption = DataRecord(<IntrBk></IntrBk>,
            UnderlyingPaymentTransaction3(
              OrgnlGrpInf = Some(UnderlyingGroupInformation1(
                OrgnlMsgId = creditTransferTransactions.head._1.customFields.flatMap(json =>
                  (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).headOption.flatMap(_.asString))
                  .orElse(message.customFields.flatMap(json =>
                    (json \\ SepaMessageCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).headOption.flatMap(_.asString)))
                  .getOrElse(""),
                OrgnlMsgNmId = creditTransferTransactions.head._1.customFields.flatMap(json =>
                  (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ORIGINAL_MESSAGE_TYPE.toString).headOption.flatMap(_.asString))
                  .orElse(message.customFields.flatMap(json =>
                    (json \\ SepaMessageCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ORIGINAL_MESSAGE_TYPE.toString).headOption.flatMap(_.asString)))
                  .getOrElse("")
              )),
              OrgnlInstrId = creditTransferTransactions.head._1.instructionId,
              OrgnlEndToEndId = Some(creditTransferTransactions.head._1.endToEndId),
              OrgnlTxId = Some(creditTransferTransactions.head._1.transactionIdInSepaFile),
              OrgnlIntrBkSttlmAmt = ActiveOrHistoricCurrencyAndAmount(value = creditTransferTransactions.head._1.amount, Map(("@Ccy", DataRecord("EUR")))),
              OrgnlIntrBkSttlmDt = {
                creditTransferTransactions.head._1.settlementDate.map(date => {
                  val settlementDate = DatatypeFactory.newInstance().newXMLGregorianCalendar
                  settlementDate.setYear(date.getYear)
                  settlementDate.setMonth(date.getMonthValue)
                  settlementDate.setDay(date.getDayOfMonth)
                  settlementDate
                }).get
              },
              OrgnlTxRef = Some(OriginalTransactionReference27(
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
            )
          )
        ),
        Mod = RequestedModification7(
          IntrBkSttlmDt = creditTransferTransactions.head._1.customFields.flatMap(json =>
            (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_NEW_SETTLEMENT_DATE.toString).headOption.flatMap(_.asString)
              .flatMap(dateString => Try {
                val date = LocalDate.parse(dateString)
                val newSettlementDate = DatatypeFactory.newInstance().newXMLGregorianCalendar
                newSettlementDate.setYear(date.getYear)
                newSettlementDate.setMonth(date.getMonthValue)
                newSettlementDate.setDay(date.getDayOfMonth)
                newSettlementDate
              }.toOption))
            .orElse(message.customFields.flatMap(json =>
              (json \\ SepaMessageCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_NEW_SETTLEMENT_DATE.toString).headOption.flatMap(_.asString).flatMap(dateString => Try {
                val date = LocalDate.parse(dateString)
                val newSettlementDate = DatatypeFactory.newInstance().newXMLGregorianCalendar
                newSettlementDate.setYear(date.getYear)
                newSettlementDate.setMonth(date.getMonthValue)
                newSettlementDate.setDay(date.getDayOfMonth)
                newSettlementDate
              }.toOption))
            )
        ),
        InstrForAssgne = creditTransferTransactions.head._1.customFields.flatMap(json =>
          (json \\ SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ADDITIONAL_INFORMATION.toString).headOption.flatMap(_.asString))
          .map(additionalInformation => InstructionForAssignee1(Cd = Some("INQR"), InstrInf = Some(additionalInformation)))
          .orElse(message.customFields.flatMap(json =>
            (json \\ SepaMessageCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ADDITIONAL_INFORMATION.toString).headOption.flatMap(_.asString))
            .map(additionalInformation => InstructionForAssignee1(Cd = Some("INQR"), InstrInf = Some(additionalInformation))))
      )
    )
    scalaxb.toXML[Document](document, "Document", defaultScope)
  }
}

object InquiryClaimValueDateCorrection {
  def fromXML(xmlFile: Elem, sepaFileId: UUID): Try[InquiryClaimNonReceiptMessage] = {
    Try(scalaxb.fromXML[Document](xmlFile)).map(document =>
      InquiryClaimNonReceiptMessage(
        SepaMessage(
          id = UUID.randomUUID(),
          creationDateTime = document.ReqToModfyPmt.Assgnmt.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime,
          messageType = SepaMessageType.B2B_INQUIRY_CLAIM_VALUE_DATE_CORRECTION,
          status = SepaMessageStatus.PROCESSING_IN_PROGRESS,
          sepaFileId = Some(sepaFileId),
          messageIdInSepaFile = document.ReqToModfyPmt.Assgnmt.Id,
          numberOfTransactions = 1,
          totalAmount = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlIntrBkSttlmAmt.value,
          settlementDate = document.ReqToModfyPmt.Mod.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate),
          instigatingAgent = document.ReqToModfyPmt.Assgnmt.Assgnr.party35choicableoption.value match {
            case assigner: BranchAndFinancialInstitutionIdentification5 => assigner.FinInstnId.BICFI.map(Bic)
            case assigner: PartyIdentification125 => assigner.Nm.map(Bic)
          },
          instigatedAgent = document.ReqToModfyPmt.Assgnmt.Assgne.party35choicableoption.value match {
            case assignee: BranchAndFinancialInstitutionIdentification5 => assignee.FinInstnId.BICFI.map(Bic)
            case assignee: PartyIdentification125 => assignee.Nm.map(Bic)
          },
          customFields = Some(Json.fromJsonObject(JsonObject.empty
            .add(SepaMessageCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_CASE_ID.toString,
              Json.fromString(document.ReqToModfyPmt.Case.map(_.Id).getOrElse("")))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_CASE_CREATOR.toString,
              Json.fromString(document.ReqToModfyPmt.Case.map(_.Cretr.party35choicableoption.value).flatMap {
                case creator: BranchAndFinancialInstitutionIdentification5able => creator.FinInstnId.BICFI
              }.getOrElse("")))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ADDITIONAL_INFORMATION.toString,
              Json.fromString(document.ReqToModfyPmt.InstrForAssgne.flatMap(_.InstrInf).getOrElse("")))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString,
              Json.fromString(document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlGrpInf.map(_.OrgnlMsgId).getOrElse("")))
            .add(SepaMessageCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ORIGINAL_MESSAGE_TYPE.toString,
              Json.fromString(document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlGrpInf.map(_.OrgnlMsgNmId).getOrElse("")))
          ))
        ),
        creditTransferTransactions = Seq(
          (SepaCreditTransferTransaction(
            id = UUID.randomUUID(),
            amount = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlIntrBkSttlmAmt.value,
            debtorName = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlTxRef.flatMap(_.Dbtr.flatMap(_.party35choicableoption.value match {
              case debtor: PartyIdentification125able => debtor.Nm
            })),
            debtorAccount = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlTxRef
              .flatMap(_.DbtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString))),
            debtorAgent = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlTxRef
              .flatMap(_.DbtrAgt.flatMap(_.FinInstnId.BICFI.map(Bic))),
            creditorName = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlTxRef.flatMap(_.Cdtr.flatMap(_.party35choicableoption.value match {
              case creditor: PartyIdentification125able => creditor.Nm
            })),
            creditorAccount = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlTxRef
              .flatMap(_.CdtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString))),
            creditorAgent = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlTxRef
              .flatMap(_.CdtrAgt.flatMap(_.FinInstnId.BICFI.map(Bic))),
            purposeCode = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlTxRef
              .flatMap(_.Purp.map(_.purpose2choiceoption.value)),
            description = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlTxRef
              .flatMap(_.RmtInf.map(_.Ustrd.mkString)),
            creationDateTime = LocalDateTime.now(),
            settlementDate = Some(document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlIntrBkSttlmDt.toGregorianCalendar.toZonedDateTime.toLocalDate),
            transactionIdInSepaFile = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlTxId.getOrElse(""),
            instructionId = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlInstrId,
            endToEndId = document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlEndToEndId.getOrElse(""),
            status = SepaCreditTransferTransactionStatus.CLAIMED_VALUE_DATE_CORRECTION,
            customFields = Some(Json.fromJsonObject(JsonObject.empty
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_CASE_ID.toString,
                Json.fromString(document.ReqToModfyPmt.Case.map(_.Id).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_CASE_CREATOR.toString,
                Json.fromString(document.ReqToModfyPmt.Case.map(_.Cretr.party35choicableoption.value).flatMap {
                  case creator: BranchAndFinancialInstitutionIdentification5able => creator.FinInstnId.BICFI
                }.getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ADDITIONAL_INFORMATION.toString,
                Json.fromString(document.ReqToModfyPmt.InstrForAssgne.flatMap(_.InstrInf).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString,
                Json.fromString(document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlGrpInf.map(_.OrgnlMsgId).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.INQUIRY_CLAIM_VALUE_DATE_CORRECTION_ORIGINAL_MESSAGE_TYPE.toString,
                Json.fromString(document.ReqToModfyPmt.Undrlyg.underlyingtransaction4choicableoption.value.OrgnlGrpInf.map(_.OrgnlMsgNmId).getOrElse("")))
            ))
          ), document.ReqToModfyPmt.Case.map(_.Id).getOrElse("")))
      )
    )
  }
}


