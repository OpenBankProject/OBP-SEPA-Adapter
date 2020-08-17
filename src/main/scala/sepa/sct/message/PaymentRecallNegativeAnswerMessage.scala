package sepa.sct.message

import java.time.{LocalDateTime, ZoneId}
import java.util.{GregorianCalendar, UUID}

import adapter.Adapter
import com.openbankproject.commons.model.{Iban, TransactionRequestId}
import io.circe.{Json, JsonObject}
import javax.xml.datatype.DatatypeFactory
import model.enums._
import model.enums.sepaReasonCodes.PaymentRecallNegativeAnswerReasonCode
import model.enums.sepaReasonCodes.PaymentRecallNegativeAnswerReasonCode.{CLOSED_ACCOUNT_NUMBER, _}
import model.jsonClasses.Party
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import scalaxb.DataRecord
import sepa.SepaUtil
import sepa.sct.generated.paymentRecallNegativeAnswer._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scala.xml.{Elem, NodeSeq}

case class PaymentRecallNegativeAnswerMessage(
                                               message: SepaMessage,
                                               creditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]
                                             ) extends SctMessage[Document] {
  def toXML: NodeSeq = {
    val document = Document(
      ResolutionOfInvestigationV03(
        Assgnmt = CaseAssignment2(
          Id = message.messageIdInSepaFile,
          Assgnr = message.instigatingAgent.map(_.bic).getOrElse("") match {
            case assigner if SepaUtil.isBic(assigner) => Party7Choice(DataRecord(<Agt></Agt>, BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some(assigner)))))
            case assigner => Party7Choice(DataRecord(<Pty></Pty>, PartyIdentification32(Nm = Some(assigner))))
          },
          Assgne = message.instigatedAgent.map(_.bic).getOrElse("") match {
            case assignee if SepaUtil.isBic(assignee) => Party7Choice(DataRecord(<Agt></Agt>, BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some(assignee)))))
            case assignee => Party7Choice(DataRecord(<Pty></Pty>, PartyIdentification32(Nm = Some(assignee))))
          },
          CreDtTm = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(message.creationDateTime.atZone(ZoneId.of("Europe/Paris"))))
        ),
        Sts = InvestigationStatus2Choice(
          investigationstatus2choicableoption = DataRecord(<Conf></Conf>, InvestigationExecutionConfirmation3Code.fromString("RJCR", defaultScope))
        ),
        CxlDtls = Seq(UnderlyingTransaction3(
          TxInfAndSts = creditTransferTransactions.map(transaction =>
            PaymentTransactionInformation33(
              CxlStsId = Some(transaction._2),
              OrgnlGrpInf = Some(OriginalGroupInformation3(
                OrgnlMsgId = transaction._1.customFields.flatMap(json => (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString)
                  .headOption.flatMap(_.asString)).getOrElse(""),
                OrgnlMsgNmId = transaction._1.customFields.flatMap(json => (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_ORIGINAL_MESSAGE_TYPE.toString)
                  .headOption.flatMap(_.asString)).getOrElse("")
              )),
              OrgnlInstrId = transaction._1.instructionId,
              OrgnlEndToEndId = Some(transaction._1.endToEndId),
              OrgnlTxId = Some(transaction._1.transactionIdInSepaFile),
              TxCxlSts = Some(RJCRValue),
              CxlStsRsnInf = transaction._1.customFields.flatMap(json =>
                (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION.toString).headOption.flatMap(_.asArray).map(_.map(reasonInformationJson =>
                  CancellationStatusReasonInformation1(
                    Orgtr = (reasonInformationJson \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION_ORIGINATOR.toString)
                      .headOption.flatMap(_.asString).map {
                      case originator if SepaUtil.isBic(originator) =>
                        PartyIdentification32(Id = Some(Party6Choice(DataRecord(<OrgId></OrgId>, OrganisationIdentification4(BICOrBEI = Some(originator))))))
                      case originator => PartyIdentification32(Nm = Some(originator))
                    },
                    Rsn = PaymentRecallNegativeAnswerReasonCode.withName(
                      (reasonInformationJson \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION_REASON_CODE.toString).headOption.flatMap(_.asString).getOrElse("")) match {
                      case CUSTOMER_DECISION => Some(CancellationStatusReason1Choice(DataRecord(<Cd></Cd>, CUSTOMER_DECISION.toString)))
                      case LEGAL_DECISION => Some(CancellationStatusReason1Choice(DataRecord(<Cd></Cd>, LEGAL_DECISION.toString)))
                      case ALREADY_RETURNED_TRANSACTION => Some(CancellationStatusReason1Choice(DataRecord(<Prtry></Prtry>, ALREADY_RETURNED_TRANSACTION.toString)))
                      case CLOSED_ACCOUNT_NUMBER => Some(CancellationStatusReason1Choice(DataRecord(<Prtry></Prtry>, CLOSED_ACCOUNT_NUMBER.toString)))
                      case INSUFFICIENT_FUNDS => Some(CancellationStatusReason1Choice(DataRecord(<Prtry></Prtry>, INSUFFICIENT_FUNDS.toString)))
                      case NO_ANSWER_FROM_CUSTOMER => Some(CancellationStatusReason1Choice(DataRecord(<Prtry></Prtry>, NO_ANSWER_FROM_CUSTOMER.toString)))
                      case NO_ORIGINAL_TRANSACTION_RECEIVED => Some(CancellationStatusReason1Choice(DataRecord(<Prtry></Prtry>, NO_ORIGINAL_TRANSACTION_RECEIVED.toString)))
                    },
                    AddtlInf = {
                      val additionalInformation = (reasonInformationJson \\ SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION_ADDITIONAL_INFORMATION.toString).headOption.flatMap(_.asString).getOrElse("")
                      additionalInformation match {
                        case _ if additionalInformation.isEmpty => Nil
                        case _ if additionalInformation.splitAt(105)._2.isEmpty => Seq(additionalInformation)
                        case _ => Seq(additionalInformation.splitAt(105)._1, additionalInformation.splitAt(105)._2.take(105))
                      }
                    }
                  ),
                ).toSeq)
              ).getOrElse(Nil),
              OrgnlTxRef = Some(OriginalTransactionReference13(
                IntrBkSttlmAmt = Some(ActiveOrHistoricCurrencyAndAmount(value = transaction._1.amount, Map(("@Ccy", DataRecord("EUR"))))),
                IntrBkSttlmDt = {
                  transaction._1.settlementDate.map(date => {
                    val settlementDate = DatatypeFactory.newInstance().newXMLGregorianCalendar
                    settlementDate.setYear(date.getYear)
                    settlementDate.setMonth(date.getMonthValue)
                    settlementDate.setDay(date.getDayOfMonth)
                    settlementDate
                  })
                },
                SttlmInf = None,
                PmtTpInf = None,
                RmtInf = transaction._1.description.map(description => RemittanceInformation5(Ustrd = Seq(description))),
                Dbtr = transaction._1.debtor.map(_.toPartyIdentification32),
                DbtrAcct = transaction._1.debtorAccount.map(account => CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
                DbtrAgt = transaction._1.debtorAgent.map(debtorAgent => BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = Some(debtorAgent.bic)))),
                UltmtDbtr = transaction._1.ultimateDebtor.map(_.toPartyIdentification32),
                Cdtr = transaction._1.creditor.map(_.toPartyIdentification32),
                CdtrAcct = transaction._1.creditorAccount.map(account => CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
                CdtrAgt = transaction._1.creditorAgent.map(creditorAgent => BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = Some(creditorAgent.bic)))),
                UltmtCdtr = transaction._1.ultimateCreditor.map(_.toPartyIdentification32),
              ))
            ),
          )
        ))
      )
    )
    scalaxb.toXML[Document](document, "Document", defaultScope)
  }
}


object PaymentRecallNegativeAnswerMessage {
  def fromXML(xmlFile: Elem, sepaFileId: UUID): Try[PaymentRecallNegativeAnswerMessage] = {
    Try(scalaxb.fromXML[Document](xmlFile)).map(document =>
      PaymentRecallNegativeAnswerMessage(
        SepaMessage(
          id = UUID.randomUUID(),
          creationDateTime = document.RsltnOfInvstgtn.Assgnmt.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime,
          messageType = SepaMessageType.B2B_PAYMENT_RECALL_NEGATIVE_ANSWER,
          status = SepaMessageStatus.PROCESSING_IN_PROGRESS,
          sepaFileId = Some(sepaFileId),
          messageIdInSepaFile = document.RsltnOfInvstgtn.Assgnmt.Id,
          numberOfTransactions = document.RsltnOfInvstgtn.CxlDtls.map(_.TxInfAndSts.length).sum,
          totalAmount = document.RsltnOfInvstgtn.CxlDtls.map(_.TxInfAndSts.map(_.OrgnlTxRef.flatMap(_.IntrBkSttlmAmt.map(_.value)).getOrElse(BigDecimal(0))).sum).sum,
          settlementDate = None,
          instigatingAgent = document.RsltnOfInvstgtn.Assgnmt.Assgnr.party7choicableoption.value match {
            case assigner: BranchAndFinancialInstitutionIdentification4 => assigner.FinInstnId.BIC.map(Bic)
            case assigner: PartyIdentification32able => assigner.Nm.map(Bic)
          },
          instigatedAgent = document.RsltnOfInvstgtn.Assgnmt.Assgne.party7choicableoption.value match {
            case assignee: BranchAndFinancialInstitutionIdentification4 => assignee.FinInstnId.BIC.map(Bic)
            case assignee: PartyIdentification32able => assignee.Nm.map(Bic)
          },
          customFields = None
        ),
        creditTransferTransactions = document.RsltnOfInvstgtn.CxlDtls.flatMap(_.TxInfAndSts.map(xmlTransaction => {
          val originalSepaCreditTransferTransaction = SepaCreditTransferTransaction(
            id = UUID.randomUUID(),
            amount = xmlTransaction.OrgnlTxRef.flatMap(_.IntrBkSttlmAmt.map(_.value)).getOrElse(0),
            debtor = xmlTransaction.OrgnlTxRef.flatMap(_.Dbtr).map(Party.fromPartyIdentification32),
            debtorAccount = xmlTransaction.OrgnlTxRef.flatMap(_.DbtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString))),
            debtorAgent = xmlTransaction.OrgnlTxRef.flatMap(_.DbtrAgt.flatMap(agent => agent.FinInstnId.BIC.map(Bic))),
            ultimateDebtor = xmlTransaction.OrgnlTxRef.flatMap(_.UltmtDbtr).map(Party.fromPartyIdentification32),
            creditor = xmlTransaction.OrgnlTxRef.flatMap(_.Cdtr).map(Party.fromPartyIdentification32),
            creditorAccount = xmlTransaction.OrgnlTxRef.flatMap(_.CdtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString))),
            creditorAgent = xmlTransaction.OrgnlTxRef.flatMap(_.CdtrAgt.flatMap(agent => agent.FinInstnId.BIC.map(Bic))),
            ultimateCreditor = xmlTransaction.OrgnlTxRef.flatMap(_.UltmtCdtr).map(Party.fromPartyIdentification32),
            purposeCode = None,
            description = xmlTransaction.OrgnlTxRef.flatMap(_.RmtInf.flatMap(_.Ustrd.headOption)),
            creationDateTime = LocalDateTime.now(),
            settlementDate = xmlTransaction.OrgnlTxRef.flatMap(_.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate)),
            transactionIdInSepaFile = xmlTransaction.OrgnlTxId.getOrElse(""),
            instructionId = xmlTransaction.OrgnlInstrId,
            endToEndId = xmlTransaction.OrgnlEndToEndId.getOrElse(""),
            settlementInformation = None, // TODO
            paymentTypeInformation = None, // TODO
            status = SepaCreditTransferTransactionStatus.RECALL_REJECTED,
            customFields = Some(Json.fromJsonObject(JsonObject.empty
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION.toString,
                Json.fromValues(xmlTransaction.CxlStsRsnInf.map(reasonInformation =>
                  Json.fromJsonObject(JsonObject.empty
                    .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION_ORIGINATOR.toString,
                      Json.fromString(reasonInformation.Orgtr.flatMap(originator => originator.Nm.
                        orElse(originator.Id.flatMap(_.party6choicableoption.value match {
                          case ordId: OrganisationIdentification4 => ordId.BICOrBEI
                        }))).getOrElse("")))
                    .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION_REASON_CODE.toString,
                      Json.fromString(reasonInformation.Rsn.map(_.cancellationstatusreason1choicableoption.value.toString)
                        .getOrElse("")))
                    .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION_ADDITIONAL_INFORMATION.toString,
                      Json.fromString(reasonInformation.AddtlInf.mkString))
                  )
                ))
              )
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString,
                Json.fromString(xmlTransaction.OrgnlGrpInf.map(_.OrgnlMsgId).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_ORIGINAL_MESSAGE_TYPE.toString,
                Json.fromString(xmlTransaction.OrgnlGrpInf.map(_.OrgnlMsgNmId).getOrElse("")))
            ))
          )
          (originalSepaCreditTransferTransaction, xmlTransaction.CxlStsId.getOrElse(""))
        }))
      )
    )
  }

  def sendRecallNegativeAnswer(sepaCreditTransferTransaction: SepaCreditTransferTransaction, reasonsInformation: Seq[ReasonInformation], obpTransactionRequestId: Option[TransactionRequestId] = None): Future[Unit] = {
    for {
      originalSepaMessage <- SepaMessage.getBySepaCreditTransferTransactionId(sepaCreditTransferTransaction.id).map(_.find(_.messageType == SepaMessageType.B2B_CREDIT_TRANSFER))
      recallNegativeAnswerSepaMessage <- {
        val sepaMessageId = UUID.randomUUID()
        val message = SepaMessage(
          sepaMessageId, LocalDateTime.now(), SepaMessageType.B2B_PAYMENT_RECALL_NEGATIVE_ANSWER,
          SepaMessageStatus.UNPROCESSED, sepaFileId = None, SepaUtil.removeDashesToUUID(sepaMessageId),
          numberOfTransactions = 1, totalAmount = sepaCreditTransferTransaction.amount, None,
          instigatingAgent = sepaCreditTransferTransaction.creditorAgent.orElse(Some(Adapter.BANK_BIC)), // Mandatory for a recall negative answer
          instigatedAgent = sepaCreditTransferTransaction.debtorAgent.orElse(Some(Adapter.CSM_BIC)), // Mandatory for a recall negative answer
          None
        )
        for {
          _ <- message.insert()
        } yield message
      }
      _ <- sepaCreditTransferTransaction.copy(
        status = SepaCreditTransferTransactionStatus.TO_RECALL_REJECT,
        customFields = Some(sepaCreditTransferTransaction.customFields.getOrElse(Json.fromJsonObject(JsonObject.empty))
          .deepMerge(Json.fromJsonObject(JsonObject.empty
            .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION.toString,
              Json.fromValues(reasonsInformation.map(reasonInformation =>
                Json.fromJsonObject(JsonObject.empty
                  .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION_ORIGINATOR.toString,
                    Json.fromString(reasonInformation.originator))
                  .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION_REASON_CODE.toString,
                    Json.fromString(reasonInformation.reasonCode.toString))
                  .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_REASON_INFORMATION_ADDITIONAL_INFORMATION.toString,
                    Json.fromString(reasonInformation.additionalInformation.getOrElse("")))
                )
              ))
            )
            .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString,
              Json.fromString(originalSepaMessage.map(_.messageIdInSepaFile).getOrElse("")))
            .add(SepaCreditTransferTransactionCustomField.PAYMENT_RECALL_NEGATIVE_ANSWER_ORIGINAL_MESSAGE_TYPE.toString,
              Json.fromString(originalSepaMessage.map(_.messageType.toString).getOrElse(""))))))
      ).update()
      transactionStatusIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID())
      _ <- sepaCreditTransferTransaction.linkMessage(recallNegativeAnswerSepaMessage.id, transactionStatusIdInSepaFile, obpTransactionRequestId, None)
    } yield ()
  }

  case class ReasonInformation(originator: String, reasonCode: PaymentRecallNegativeAnswerReasonCode, additionalInformation: Option[String])

}
