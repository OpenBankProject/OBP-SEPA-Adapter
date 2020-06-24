package sepa

import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.util.{GregorianCalendar, UUID}

import io.circe.Json
import javax.xml.datatype.DatatypeFactory
import model.enums.SepaMessageType
import model.enums.SepaMessageType.SepaMessageType
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import scalaxb.DataRecord
import sepa.sct.generated.creditTransfer._

import scala.util.Try
import scala.xml.{Elem, NodeSeq}

case class CreditTransferMessage(
                                  override val id: UUID,
                                  override val creationDateTime: LocalDateTime = LocalDateTime.now(),
                                  override val messageType: SepaMessageType = SepaMessageType.B2B_CREDIT_TRANSFER,
                                  override val sepaFileId: Option[UUID],
                                  override val messageIdInSepaFile: String,
                                  override val numberOfTransactions: Int,
                                  override val totalAmount: BigDecimal,
                                  override val settlementDate: Option[LocalDate],
                                  override val instigatingAgent: Option[Bic],
                                  override val instigatedAgent: Option[Bic],
                                  override val customFields: Option[Json],
                                  creditTransferTransactions: Seq[SepaCreditTransferTransaction]
                                ) extends SepaMessage(id, creationDateTime, messageType, sepaFileId, messageIdInSepaFile, numberOfTransactions, totalAmount, settlementDate, instigatingAgent, instigatedAgent, customFields) {
  def toXML: NodeSeq = {
    val document = Document(
      FIToFICustomerCreditTransferV02(
        GrpHdr = GroupHeader33(
          MsgId = messageIdInSepaFile,
          CreDtTm = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(creationDateTime.atZone(ZoneId.of("Europe/Paris")))),
          NbOfTxs = numberOfTransactions.toString,
          TtlIntrBkSttlmAmt = Some(ActiveCurrencyAndAmount(value = totalAmount, Map(("@Ccy", DataRecord("EUR"))))),
          IntrBkSttlmDt = {
            settlementDate.map(date => {
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
      creationDateTime = sepaMessage.creationDateTime,
      messageType = sepaMessage.messageType,
      sepaFileId = sepaMessage.sepaFileId,
      messageIdInSepaFile = sepaMessage.messageIdInSepaFile,
      numberOfTransactions = sepaMessage.numberOfTransactions,
      totalAmount = sepaMessage.totalAmount,
      settlementDate = sepaMessage.settlementDate,
      instigatingAgent = sepaMessage.instigatingAgent,
      instigatedAgent = sepaMessage.instigatedAgent,
      customFields = sepaMessage.customFields,
      creditTransferTransactions = creditTransferTransactions
    )
  }

  def fromXML(messageId: UUID = UUID.randomUUID(), sepaFileId: UUID, xmlFile: Elem): Try[CreditTransferMessage] = {
    Try(scalaxb.fromXML[Document](xmlFile)).map(document =>
      CreditTransferMessage(
        id = messageId,
        creationDateTime = document.FIToFICstmrCdtTrf.GrpHdr.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime,
        messageType = SepaMessageType.B2B_CREDIT_TRANSFER,
        sepaFileId = Some(sepaFileId),
        messageIdInSepaFile = document.FIToFICstmrCdtTrf.GrpHdr.MsgId,
        numberOfTransactions = document.FIToFICstmrCdtTrf.GrpHdr.NbOfTxs.toInt,
        totalAmount = document.FIToFICstmrCdtTrf.CdtTrfTxInf.map(_.IntrBkSttlmAmt.value).sum,
        settlementDate = document.FIToFICstmrCdtTrf.GrpHdr.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate),
        instigatingAgent = document.FIToFICstmrCdtTrf.GrpHdr.InstgAgt.flatMap(_.FinInstnId.BIC.map(Bic)),
        instigatedAgent = document.FIToFICstmrCdtTrf.GrpHdr.InstdAgt.flatMap(_.FinInstnId.BIC.map(Bic)),
        customFields = None,
        creditTransferTransactions = document.FIToFICstmrCdtTrf.CdtTrfTxInf.map(transaction =>
          SepaCreditTransferTransaction.fromXML(transaction, document.FIToFICstmrCdtTrf.GrpHdr.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime)
        )
      )
    )
  }
}

