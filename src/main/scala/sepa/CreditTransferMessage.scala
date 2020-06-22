package sepa

import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.util.{GregorianCalendar, UUID}

import com.openbankproject.commons.model.Iban
import generated._
import javax.xml.datatype.DatatypeFactory
import model.enums.SepaMessageType
import model.enums.SepaMessageType.SepaMessageType
import model.types.Bic
import scalaxb.DataRecord

import scala.util.Try
import scala.xml.{Elem, NodeSeq}

case class CreditTransferMessage(
                                  override val id: UUID,
                                  override val creationDateTime: LocalDateTime = LocalDateTime.now(),
                                  override val messageType: SepaMessageType = SepaMessageType.B2B_CREDIT_TRANSFER,
                                  override val content: Option[String],
                                  override val sepaFileId: Option[UUID],
                                  override val idInSepaFile: String,
                                  interbankSettlementDate: Option[LocalDate] = None,
                                  instigatingAgent: Option[Bic] = None,
                                  instigatedAgent: Option[Bic] = None,
                                  creditTransferTransactions: Seq[SepaCreditTransferTransaction]
                                ) extends SepaMessage(id, creationDateTime, messageType, content, sepaFileId, idInSepaFile) {
  val numberOfTransactions: Int = creditTransferTransactions.length
  val totalAmount: BigDecimal = creditTransferTransactions.map(_.amount).sum

  def toXML: NodeSeq = {
    val document = Document(
      FIToFICustomerCreditTransferV02(
        GrpHdr = GroupHeader33(
          MsgId = idInSepaFile,
          CreDtTm = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(creationDateTime.atZone(ZoneId.of("Europe/Paris")))),
          NbOfTxs = numberOfTransactions.toString,
          TtlIntrBkSttlmAmt = Some(ActiveCurrencyAndAmount(value = totalAmount, Map(("@Ccy", DataRecord("EUR"))))),
          IntrBkSttlmDt = {
            interbankSettlementDate.map(date => {
              val IntrBkSttlmDt = DatatypeFactory.newInstance().newXMLGregorianCalendar
              IntrBkSttlmDt.setYear(date.getYear)
              IntrBkSttlmDt.setMonth(date.getMonthValue)
              IntrBkSttlmDt.setDay(date.getDayOfMonth)
              IntrBkSttlmDt
            })
          },
          SttlmInf = SettlementInformation13(SttlmMtd = CLRG),
          PmtTpInf = Some(PaymentTypeInformation21(SvcLvl = Some(ServiceLevel8Choice(DataRecord(<Cd></Cd>, "SEPA"))))),
          InstgAgt = instigatingAgent.map(agent => BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some(agent.bic)))),
          InstdAgt = instigatingAgent.map(agent => BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some(agent.bic)))),
        ),
        CdtTrfTxInf = creditTransferTransactions.map(transaction =>
          CreditTransferTransactionInformation11(
            PmtId = PaymentIdentification3(
              EndToEndId = transaction.endToEndId,
              TxId = transaction.idInSepaFile,
              InstrId = transaction.instructionId
            ),
            IntrBkSttlmAmt = ActiveCurrencyAndAmount(value = transaction.amount, Map(("@Ccy", DataRecord("EUR")))),
            ChrgBr = SLEV,
            Dbtr = PartyIdentification32(
              Nm = transaction.debtorName
            ),
            DbtrAcct = transaction.debtorAccount.map(account => CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
            DbtrAgt = BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = transaction.debtorAgent.map(_.bic))),
            Cdtr = PartyIdentification32(
              Nm = transaction.creditorName,
            ),
            CdtrAcct = transaction.creditorAccount.map(account => CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
            CdtrAgt = BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = transaction.creditorAgent.map(_.bic))),
            Purp = transaction.purposeCode.map(purposeCode => Purpose2Choice(DataRecord(<Cd></Cd>, purposeCode))),
            RmtInf = transaction.descripton.map(description => RemittanceInformation5(Ustrd = Seq(description)))
          )
        )
      )
    )
    scalaxb.toXML[Document](document, "Document", defaultScope)
  }
}

object CreditTransferMessage {
  def fromXML(id: UUID = UUID.randomUUID(), sepaFileId: UUID, xmlFile: Elem): Try[CreditTransferMessage] = {
    Try(scalaxb.fromXML[Document](xmlFile)).map(document =>
      CreditTransferMessage(
        id = id,
        creationDateTime = document.FIToFICstmrCdtTrf.GrpHdr.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime,
        content = Some(document.toString),
        sepaFileId = Some(sepaFileId),
        idInSepaFile = document.FIToFICstmrCdtTrf.GrpHdr.MsgId,
        interbankSettlementDate = document.FIToFICstmrCdtTrf.GrpHdr.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate),
        instigatingAgent = document.FIToFICstmrCdtTrf.GrpHdr.InstgAgt.flatMap(_.FinInstnId.BIC.map(Bic)),
        instigatedAgent = document.FIToFICstmrCdtTrf.GrpHdr.InstdAgt.flatMap(_.FinInstnId.BIC.map(Bic)),
        creditTransferTransactions = document.FIToFICstmrCdtTrf.CdtTrfTxInf.map(transaction =>
          SepaCreditTransferTransaction(
            id = UUID.randomUUID(),
            amount = transaction.IntrBkSttlmAmt.value,
            debtorName = transaction.Dbtr.Nm,
            debtorAccount = transaction.DbtrAcct.map(a => Iban(a.Id.accountidentification4choicableoption.value.toString)),
            debtorAgent = transaction.DbtrAgt.FinInstnId.BIC.map(Bic),
            creditorName = transaction.Cdtr.Nm,
            creditorAccount = transaction.CdtrAcct.map(a => Iban(a.Id.accountidentification4choicableoption.value.toString)),
            creditorAgent = transaction.CdtrAgt.FinInstnId.BIC.map(Bic),
            purposeCode = transaction.Purp.map(_.purpose2choicableoption.value),
            descripton = transaction.RmtInf.flatMap(_.Ustrd.headOption),
            sepaMessageId = id,
            idInSepaFile = transaction.PmtId.TxId,
            instructionId = transaction.PmtId.InstrId,
            endToEndId = transaction.PmtId.EndToEndId
          )
        )
      )
    )
  }
}

