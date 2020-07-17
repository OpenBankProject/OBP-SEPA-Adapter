package sepa

import java.time.ZoneId
import java.util.{GregorianCalendar, UUID}

import javax.xml.datatype.DatatypeFactory
import model.enums.{SepaMessageStatus, SepaMessageType}
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import scalaxb.DataRecord
import sepa.sct.generated.creditTransfer._

import scala.util.Try
import scala.xml.{Elem, NodeSeq}

case class CreditTransferMessage(
                                  message: SepaMessage,
                                  creditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]
                                ) extends SctMessage {
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
        CdtTrfTxInf = creditTransferTransactions.map(transaction => scalaxb.fromXML[CreditTransferTransactionInformation11](transaction._1.toXML))
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
          (SepaCreditTransferTransaction.fromXML(transaction, document.FIToFICstmrCdtTrf.GrpHdr.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate)),
            transaction.PmtId.TxId
            )
        )
      )
    )
  }
}

