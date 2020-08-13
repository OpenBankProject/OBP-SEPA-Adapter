package sepa.sct.message

import java.time.{LocalDateTime, ZoneId}
import java.util.{GregorianCalendar, UUID}

import com.openbankproject.commons.model.Iban
import javax.xml.datatype.DatatypeFactory
import model.enums.{SepaCreditTransferTransactionStatus, SepaMessageStatus, SepaMessageType}
import model.jsonClasses.Party
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import scalaxb.DataRecord
import sepa.sct.generated.creditTransfer._

import scala.util.Try
import scala.xml.{Elem, NodeSeq}

case class CreditTransferMessage(
                                  message: SepaMessage,
                                  creditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]
                                ) extends SctMessage[Document] {
  def toXML: NodeSeq = {
    val document = Document(
      FIToFICustomerCreditTransferV02(
        GrpHdr = GroupHeader33(
          MsgId = message.messageIdInSepaFile,
          CreDtTm = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(message.creationDateTime.atZone(ZoneId.of("Europe/Paris")))),
          NbOfTxs = message.numberOfTransactions.toString,
          TtlIntrBkSttlmAmt = Some(ActiveCurrencyAndAmount(value = message.totalAmount, Map(("@Ccy", DataRecord("EUR"))))),
          IntrBkSttlmDt = {
            message.settlementDate.map(date => {
              val IntrBkSttlmDt = DatatypeFactory.newInstance().newXMLGregorianCalendar
              IntrBkSttlmDt.setYear(date.getYear)
              IntrBkSttlmDt.setMonth(date.getMonthValue)
              IntrBkSttlmDt.setDay(date.getDayOfMonth)
              IntrBkSttlmDt
            })
          },
          SttlmInf = SettlementInformation13(SttlmMtd = CLRG),
          PmtTpInf = Some(PaymentTypeInformation21(SvcLvl = Some(ServiceLevel8Choice(DataRecord(<Cd></Cd>, "SEPA"))))),
          InstgAgt = message.instigatingAgent.map(agent => BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some(agent.bic)))),
          InstdAgt = message.instigatingAgent.map(agent => BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some(agent.bic)))),
        ),
        CdtTrfTxInf = creditTransferTransactions.map(transaction => CreditTransferTransactionInformation11(
          PmtId = PaymentIdentification3(
            EndToEndId = transaction._1.endToEndId,
            TxId = transaction._1.transactionIdInSepaFile,
            InstrId = transaction._1.instructionId
          ),
          IntrBkSttlmAmt = ActiveCurrencyAndAmount(value = transaction._1.amount, Map(("@Ccy", DataRecord("EUR")))),
          ChrgBr = SLEV,
          Dbtr = transaction._1.debtor.map(_.toPartyIdentification32CreditTransfer).get,
          DbtrAcct = transaction._1.debtorAccount.map(account => CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
          DbtrAgt = BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = transaction._1.debtorAgent.map(_.bic))),
          UltmtDbtr = transaction._1.ultimateDebtor.map(_.toPartyIdentification32CreditTransfer),
          Cdtr = transaction._1.creditor.map(_.toPartyIdentification32CreditTransfer).get,
          CdtrAcct = transaction._1.creditorAccount.map(account => CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
          CdtrAgt = BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = transaction._1.creditorAgent.map(_.bic))),
          UltmtCdtr = transaction._1.ultimateCreditor.map(_.toPartyIdentification32CreditTransfer),
          Purp = transaction._1.purposeCode.map(purposeCode => Purpose2Choice(DataRecord(<Cd></Cd>, purposeCode))),
          RmtInf = transaction._1.description.map(description => RemittanceInformation5(Ustrd = Seq(description)))
        ))
      )
    )
    scalaxb.toXML[Document](document, "Document", defaultScope)
  }
}

object CreditTransferMessage {

  def fromXML(xmlFile: Elem, sepaFileId: UUID): Try[CreditTransferMessage] = {
    Try(scalaxb.fromXML[Document](xmlFile)).map(document =>
      CreditTransferMessage(
        SepaMessage(
          id = UUID.randomUUID(),
          creationDateTime = document.FIToFICstmrCdtTrf.GrpHdr.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime,
          messageType = SepaMessageType.B2B_CREDIT_TRANSFER,
          status = SepaMessageStatus.PROCESSING_IN_PROGRESS,
          sepaFileId = Some(sepaFileId),
          messageIdInSepaFile = document.FIToFICstmrCdtTrf.GrpHdr.MsgId,
          numberOfTransactions = document.FIToFICstmrCdtTrf.GrpHdr.NbOfTxs.toInt,
          totalAmount = document.FIToFICstmrCdtTrf.GrpHdr.TtlIntrBkSttlmAmt.map(_.value).getOrElse(document.FIToFICstmrCdtTrf.CdtTrfTxInf.map(_.IntrBkSttlmAmt.value).sum),
          settlementDate = document.FIToFICstmrCdtTrf.GrpHdr.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate),
          instigatingAgent = document.FIToFICstmrCdtTrf.GrpHdr.InstgAgt.flatMap(_.FinInstnId.BIC.map(Bic)),
          instigatedAgent = document.FIToFICstmrCdtTrf.GrpHdr.InstdAgt.flatMap(_.FinInstnId.BIC.map(Bic)),
          customFields = None
        ),
        creditTransferTransactions = document.FIToFICstmrCdtTrf.CdtTrfTxInf.map(transaction =>
          (SepaCreditTransferTransaction(
            id = UUID.randomUUID(),
            amount = transaction.IntrBkSttlmAmt.value,
            debtor = Some(Party.fromPartyIdentification32(transaction.Dbtr)),
            debtorAccount = transaction.DbtrAcct.map(a => Iban(a.Id.accountidentification4choicableoption.value.toString)),
            debtorAgent = transaction.DbtrAgt.FinInstnId.BIC.map(Bic),
            ultimateDebtor = transaction.UltmtDbtr.map(Party.fromPartyIdentification32),
            creditor = Some(Party.fromPartyIdentification32(transaction.Cdtr)),
            creditorAccount = transaction.CdtrAcct.map(a => Iban(a.Id.accountidentification4choicableoption.value.toString)),
            creditorAgent = transaction.CdtrAgt.FinInstnId.BIC.map(Bic),
            ultimateCreditor = transaction.UltmtCdtr.map(Party.fromPartyIdentification32),
            purposeCode = transaction.Purp.map(_.purpose2choicableoption.value),
            description = transaction.RmtInf.flatMap(_.Ustrd.headOption),
            creationDateTime = LocalDateTime.now(),
            settlementDate = document.FIToFICstmrCdtTrf.GrpHdr.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate),
            transactionIdInSepaFile = transaction.PmtId.TxId,
            instructionId = transaction.PmtId.InstrId,
            endToEndId = transaction.PmtId.EndToEndId,
            status = SepaCreditTransferTransactionStatus.UNPROCESSED,
            customFields = None
          ), transaction.PmtId.TxId)
        )
      )
    )
  }
}

