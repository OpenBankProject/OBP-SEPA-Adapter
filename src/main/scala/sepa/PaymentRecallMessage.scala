package sepa

import java.time.{LocalDateTime, ZoneId}
import java.util.{GregorianCalendar, UUID}

import adapter.Adapter
import com.openbankproject.commons.model.{Iban, TransactionRequestId}
import io.circe.{Json, JsonObject}
import javax.xml.datatype.DatatypeFactory
import model.enums._
import model.enums.sepaReasonCodes.PaymentRecallReasonCode
import model.enums.sepaReasonCodes.PaymentRecallReasonCode._
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import scalaxb.DataRecord
import sepa.sct.generated.paymentRecall._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scala.xml.{Elem, NodeSeq}

case class PaymentRecallMessage(
                                 message: SepaMessage,
                                 creditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]
                               ) extends SctMessage {
  def toXML: NodeSeq = {
    val document = Document(
      FIToFIPaymentCancellationRequestV01(
        Assgnmt = CaseAssignment2(
          Id = message.messageIdInSepaFile,
          Assgnr = message.instigatingAgent.map(_.bic).getOrElse("") match {
            case assigner if SepaUtil.isBic(assigner) => Party7Choice(DataRecord(<Agt></Agt>, BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some(assigner)))))
            case assigner => Party7Choice(DataRecord(<Pty></Pty>, PartyIdentification32(Nm = Some(assigner))))
          },
          Assgne = message.instigatedAgent.map(_.bic).getOrElse("") match {
            case assigner if SepaUtil.isBic(assigner) => Party7Choice(DataRecord(<Agt></Agt>, BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some(assigner)))))
            case assigner => Party7Choice(DataRecord(<Pty></Pty>, PartyIdentification32(Nm = Some(assigner))))
          },
          CreDtTm = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(message.creationDateTime.atZone(ZoneId.of("Europe/Paris"))))
        ),
        CtrlData = Some(ControlData1(message.numberOfTransactions.toString)),
        Undrlyg = Seq(UnderlyingTransaction2( // WARNING : Not sure about the multiplicity of Undrlyg/TxInf
          TxInf = creditTransferTransactions.map(transaction =>
            PaymentTransactionInformation31(
              CxlId = Some(transaction._2),
              OrgnlGrpInf = transaction._1.customFields.flatMap(json =>
                for {
                  originalMessageIdInSepaFile <- (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).headOption.flatMap(_.asString)
                  originalMessageType <- (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ORIGINAL_MESSAGE_TYPE.toString).headOption.flatMap(_.asString)
                } yield OriginalGroupInformation3(originalMessageIdInSepaFile, originalMessageType)
              ),
              OrgnlInstrId = transaction._1.instructionId,
              OrgnlEndToEndId = Some(transaction._1.endToEndId),
              OrgnlTxId = Some(transaction._1.transactionIdInSepaFile),
              OrgnlIntrBkSttlmAmt = Some(ActiveOrHistoricCurrencyAndAmount(value = transaction._1.amount, Map(("@Ccy", DataRecord("EUR"))))),
              OrgnlIntrBkSttlmDt = {
                transaction._1.settlementDate.map(date => {
                  val settlementDate = DatatypeFactory.newInstance().newXMLGregorianCalendar
                  settlementDate.setYear(date.getYear)
                  settlementDate.setMonth(date.getMonthValue)
                  settlementDate.setDay(date.getDayOfMonth)
                  settlementDate
                })
              },
              CxlRsnInf = Seq(CancellationReasonInformation3(
                Orgtr = transaction._1.customFields.flatMap(json => (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ORIGINATOR.toString).headOption.flatMap(_.asString))
                  .map {
                    case originator if SepaUtil.isBic(originator) =>
                      PartyIdentification32(Id = Some(Party6Choice(DataRecord(<OrgId></OrgId>, OrganisationIdentification4(BICOrBEI = Some(originator))))))
                    case originator => PartyIdentification32(Nm = Some(originator))
                  },
                Rsn = PaymentRecallReasonCode.withName(transaction._1.customFields.flatMap(json =>
                  (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_REASON_CODE.toString).headOption.flatMap(_.asString)).getOrElse("")) match {
                  case DUPLICATE_PAYMENT => Some(CancellationReason2Choice(DataRecord(<Cd></Cd>, DUPLICATE_PAYMENT.toString)))
                  case TECHNICAL_PROBLEM => Some(CancellationReason2Choice(DataRecord(<Prtry></Prtry>, TECHNICAL_PROBLEM.toString)))
                  case FRAUDULENT_ORIGIN => Some(CancellationReason2Choice(DataRecord(<Prtry></Prtry>, FRAUDULENT_ORIGIN.toString)))
                  case REQUESTED_BY_CUSTOMER => Some(CancellationReason2Choice(DataRecord(<Cd></Cd>, REQUESTED_BY_CUSTOMER.toString)))
                  case WRONG_AMOUNT => Some(CancellationReason2Choice(DataRecord(<Prtry></Prtry>, WRONG_AMOUNT.toString)))
                  case INVALID_CREDITOR_ACCOUNT_NUMBER => Some(CancellationReason2Choice(DataRecord(<Prtry></Prtry>, INVALID_CREDITOR_ACCOUNT_NUMBER.toString)))
                },
                AddtlInf = transaction._1.customFields.flatMap(json =>
                  (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_REASON_CODE.toString).headOption.flatMap(_.asString).map(withName).flatMap {
                    case FRAUDULENT_ORIGIN | REQUESTED_BY_CUSTOMER | WRONG_AMOUNT | INVALID_CREDITOR_ACCOUNT_NUMBER =>
                      (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ADDITIONAL_INFORMATION.toString).headOption.flatMap(_.asString)
                  }
                ).toSeq
              )),
              OrgnlTxRef = Some(OriginalTransactionReference13(
                SttlmInf = None,
                PmtTpInf = None,
                RmtInf = transaction._1.description.map(description => RemittanceInformation5(Ustrd = Seq(description))),
                Dbtr = transaction._1.debtorName.map(name => PartyIdentification32(Nm = Some(name))),
                DbtrAcct = transaction._1.debtorAccount.map(account => CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
                DbtrAgt = transaction._1.debtorAgent.map(debtorAgent => BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = Some(debtorAgent.bic)))),
                Cdtr = transaction._1.creditorName.map(name => PartyIdentification32(Nm = Some(name))),
                CdtrAcct = transaction._1.creditorAccount.map(account => CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
                CdtrAgt = transaction._1.creditorAgent.map(creditorAgent => BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = Some(creditorAgent.bic)))),
              ))
            )
          )
        ))
      )
    )
    scalaxb.toXML[Document](document, "Document", defaultScope)
  }
}


object PaymentRecallMessage {
  def fromXML(xmlFile: Elem, sepaFileId: UUID): Try[PaymentRecallMessage] = {
    Try(scalaxb.fromXML[Document](xmlFile)).map(document =>
      PaymentRecallMessage(
        SepaMessage(
          id = UUID.randomUUID(),
          creationDateTime = document.FIToFIPmtCxlReq.Assgnmt.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime,
          messageType = SepaMessageType.B2B_PAYMENT_RECALL,
          status = SepaMessageStatus.PROCESSING_IN_PROGRESS,
          sepaFileId = Some(sepaFileId),
          messageIdInSepaFile = document.FIToFIPmtCxlReq.Assgnmt.Id,
          numberOfTransactions = document.FIToFIPmtCxlReq.CtrlData.map(_.NbOfTxs.toInt).getOrElse(document.FIToFIPmtCxlReq.Undrlyg.map(_.TxInf.length).sum),
          totalAmount = document.FIToFIPmtCxlReq.Undrlyg.map(_.TxInf.map(_.OrgnlIntrBkSttlmAmt.map(_.value).getOrElse(BigDecimal(0))).sum).sum,
          settlementDate = None,
          instigatingAgent = document.FIToFIPmtCxlReq.Assgnmt.Assgnr.party7choicableoption.value match {
            case assigner: BranchAndFinancialInstitutionIdentification4 => assigner.FinInstnId.BIC.map(Bic)
            case assigner: PartyIdentification32able => assigner.Nm.map(Bic)
          },
          instigatedAgent = document.FIToFIPmtCxlReq.Assgnmt.Assgne.party7choicableoption.value match {
            case assigner: BranchAndFinancialInstitutionIdentification4 => assigner.FinInstnId.BIC.map(Bic)
            case assigner: PartyIdentification32able => assigner.Nm.map(Bic)
          },
          customFields = None
        ),
        creditTransferTransactions = document.FIToFIPmtCxlReq.Undrlyg.flatMap(_.TxInf.map(xmlTransaction => {
          val originalSepaCreditTransferTransaction = SepaCreditTransferTransaction(
            id = UUID.randomUUID(),
            amount = xmlTransaction.OrgnlIntrBkSttlmAmt.map(_.value).getOrElse(0),
            debtorName = xmlTransaction.OrgnlTxRef.flatMap(_.Dbtr.flatMap(_.Nm)),
            debtorAccount = xmlTransaction.OrgnlTxRef.flatMap(_.DbtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString))),
            debtorAgent = xmlTransaction.OrgnlTxRef.flatMap(_.DbtrAgt.flatMap(agent => agent.FinInstnId.BIC.map(Bic))),
            creditorName = xmlTransaction.OrgnlTxRef.flatMap(_.Cdtr.flatMap(_.Nm)),
            creditorAccount = xmlTransaction.OrgnlTxRef.flatMap(_.CdtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString))),
            creditorAgent = xmlTransaction.OrgnlTxRef.flatMap(_.CdtrAgt.flatMap(agent => agent.FinInstnId.BIC.map(Bic))),
            purposeCode = None,
            description = xmlTransaction.OrgnlTxRef.flatMap(_.RmtInf.flatMap(_.Ustrd.headOption)),
            creationDateTime = LocalDateTime.now(),
            settlementDate = xmlTransaction.OrgnlIntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate),
            transactionIdInSepaFile = xmlTransaction.OrgnlTxId.getOrElse(""),
            instructionId = xmlTransaction.OrgnlInstrId, xmlTransaction.OrgnlEndToEndId.getOrElse(""),
            status = SepaCreditTransferTransactionStatus.RECALLED,
            customFields = Some(Json.fromJsonObject(JsonObject.empty
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ORIGINATOR.toString,
                Json.fromString(xmlTransaction.CxlRsnInf.headOption.flatMap(_.Orgtr.flatMap(originator => originator.Nm.
                  orElse(originator.Id.flatMap(_.party6choicableoption.value match {
                    case ordId: OrganisationIdentification4 => ordId.BICOrBEI
                  })))).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_REASON_CODE.toString,
                Json.fromString(xmlTransaction.CxlRsnInf.headOption.flatMap(_.Rsn.map(_.cancellationreason2choicableoption.value.toString))
                  .getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ADDITIONAL_INFORMATION.toString,
                Json.fromString(xmlTransaction.CxlRsnInf.headOption.flatMap(_.AddtlInf.headOption).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString,
                Json.fromString(xmlTransaction.OrgnlGrpInf.map(_.OrgnlMsgId).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ORIGINAL_MESSAGE_TYPE.toString,
                Json.fromString(xmlTransaction.OrgnlGrpInf.map(_.OrgnlMsgNmId).getOrElse("")))
            ))
          )
          (originalSepaCreditTransferTransaction, xmlTransaction.CxlId.getOrElse(""))
        }))
      )
    )
  }


  def recallTransaction(transactionToRecall: SepaCreditTransferTransaction, originator: String, paymentRecallReasonCode: PaymentRecallReasonCode, additionalInformation: Option[String], obpTransactionRequestId: Option[TransactionRequestId] = None): Future[Unit] = {
    for {
      originalSepaMessage <- SepaMessage.getBySepaCreditTransferTransactionId(transactionToRecall.id).map(_.find(_.messageType == SepaMessageType.B2B_CREDIT_TRANSFER))
      recallSepaMessage <- {
        val sepaMessageId = UUID.randomUUID()
        val message = SepaMessage(
          sepaMessageId, LocalDateTime.now(), SepaMessageType.B2B_PAYMENT_RECALL,
          SepaMessageStatus.UNPROCESSED, sepaFileId = None, SepaUtil.removeDashesToUUID(sepaMessageId),
          numberOfTransactions = 1, totalAmount = transactionToRecall.amount, None,
          instigatingAgent = transactionToRecall.debtorAgent.orElse(Some(Adapter.BANK_BIC)), // Mandatory for a recall
          instigatedAgent = transactionToRecall.creditorAgent.orElse(Some(Adapter.CSM_BIC)), // Mandatory for a recall
          None
        )
        for {
          _ <- message.insert()
        } yield message
      }
      _ <- transactionToRecall.copy(
        status = SepaCreditTransferTransactionStatus.TO_RECALL,
        customFields = Some(transactionToRecall.customFields.getOrElse(Json.fromJsonObject(JsonObject.empty))
          .deepMerge(Json.fromJsonObject(JsonObject.empty
            .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ORIGINATOR.toString,
              Json.fromString(originator))
            .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_REASON_CODE.toString,
              Json.fromString(paymentRecallReasonCode.toString))
            .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ADDITIONAL_INFORMATION.toString,
              Json.fromString(additionalInformation.getOrElse("")))
            .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString,
              Json.fromString(originalSepaMessage.map(_.messageIdInSepaFile).getOrElse("")))
            .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_ORIGINAL_MESSAGE_TYPE.toString,
              Json.fromString(originalSepaMessage.map(_.messageType.toString).getOrElse(""))))))
      ).update()
      transactionStatusIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID())
      _ <- transactionToRecall.linkMessage(recallSepaMessage.id, transactionStatusIdInSepaFile, obpTransactionRequestId, None)
    } yield ()
  }
}
