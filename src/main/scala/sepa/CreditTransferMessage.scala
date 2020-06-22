package sepa

import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.util.{GregorianCalendar, UUID}

import generated._
import javax.xml.datatype.DatatypeFactory
import model.enums.SepaMessageType
import model.enums.SepaMessageType.SepaMessageType
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
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
        CdtTrfTxInf = creditTransferTransactions.map(transaction => scalaxb.fromXML[CreditTransferTransactionInformation11](transaction.toXML))
      )
    )
    scalaxb.toXML[Document](document, "Document", defaultScope)
  }
}

object CreditTransferMessage {
  def fromSepaMessage(sepaMessage: SepaMessage, creditTransferTransactions: Seq[SepaCreditTransferTransaction]): CreditTransferMessage = {
    CreditTransferMessage(
      id = sepaMessage.id,
      creationDateTime = LocalDateTime.now(),
      messageType = sepaMessage.messageType,
      content = sepaMessage.content,
      sepaFileId = sepaMessage.sepaFileId,
      idInSepaFile = sepaMessage.idInSepaFile,
      interbankSettlementDate = Some(sepaMessage.creationDateTime.toLocalDate.plusDays(1)),
      instigatingAgent = None,
      instigatedAgent = None,
      creditTransferTransactions = creditTransferTransactions
    )
  }

  def fromXML(messageId: UUID = UUID.randomUUID(), sepaFileId: UUID, xmlFile: Elem): Try[CreditTransferMessage] = {
    Try(scalaxb.fromXML[Document](xmlFile)).map(document =>
      CreditTransferMessage(
        id = messageId,
        creationDateTime = document.FIToFICstmrCdtTrf.GrpHdr.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime,
        content = Some(document.toString),
        sepaFileId = Some(sepaFileId),
        idInSepaFile = document.FIToFICstmrCdtTrf.GrpHdr.MsgId,
        interbankSettlementDate = document.FIToFICstmrCdtTrf.GrpHdr.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate),
        instigatingAgent = document.FIToFICstmrCdtTrf.GrpHdr.InstgAgt.flatMap(_.FinInstnId.BIC.map(Bic)),
        instigatedAgent = document.FIToFICstmrCdtTrf.GrpHdr.InstdAgt.flatMap(_.FinInstnId.BIC.map(Bic)),
        creditTransferTransactions = document.FIToFICstmrCdtTrf.CdtTrfTxInf.map(transaction =>
          SepaCreditTransferTransaction.fromXML(transaction, messageId)
        )
      )
    )
  }
}

