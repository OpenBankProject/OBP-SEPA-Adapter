package sepa.sct.message

import java.time.{LocalDateTime, ZoneId}
import java.util.{GregorianCalendar, UUID}

import com.openbankproject.commons.model.{Iban, TransactionId, TransactionRequestId}
import io.circe.{Json, JsonObject}
import javax.xml.datatype.DatatypeFactory
import model.enums._
import model.enums.sepaReasonCodes.PaymentReturnReasonCode.PaymentReturnReasonCode
import model.jsonClasses.{Party, PaymentTypeInformation, SettlementInformation, SettlementMethod}
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage}
import scalaxb.DataRecord
import sepa.SepaUtil
import sepa.sct.generated.paymentReturn
import sepa.sct.generated.paymentReturn._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scala.xml.{Elem, NodeSeq}

case class PaymentReturnMessage(
                                 message: SepaMessage,
                                 creditTransferTransactions: Seq[(SepaCreditTransferTransaction, String)]
                               ) extends SctMessage[Document] {
  def toXML: NodeSeq = {
    val document = Document(
      PaymentReturnV02(
        GrpHdr = GroupHeader38(
          MsgId = message.messageIdInSepaFile,
          CreDtTm = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(message.creationDateTime.atZone(ZoneId.of("Europe/Paris")))),
          NbOfTxs = message.numberOfTransactions.toString,
          TtlRtrdIntrBkSttlmAmt = Some(paymentReturn.ActiveCurrencyAndAmount(value = message.totalAmount, Map(("@Ccy", DataRecord("EUR"))))),
          IntrBkSttlmDt = {
            message.settlementDate.map(date => {
              val IntrBkSttlmDt = DatatypeFactory.newInstance().newXMLGregorianCalendar
              IntrBkSttlmDt.setYear(date.getYear)
              IntrBkSttlmDt.setMonth(date.getMonthValue)
              IntrBkSttlmDt.setDay(date.getDayOfMonth)
              IntrBkSttlmDt
            })
          },
          SttlmInf = message.customFields.flatMap(json =>
            (json \\ SepaMessageCustomField.PAYMENT_RETURN_SETTLEMENT_INFORMATION.toString).headOption)
            .flatMap(json => SettlementInformation.fromJson(json.toString)).map(_.toSettlementInformation13PR).get,
          InstgAgt = message.instigatingAgent.map(agent => BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some(agent.bic)))),
          InstdAgt = message.instigatingAgent.map(agent => BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some(agent.bic)))),
        ),
        OrgnlGrpInf = message.customFields.flatMap(json =>
          for {
            originalSepaMessageId <- (json \\ SepaMessageCustomField.ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).headOption.flatMap(_.asString)
            originalSepaMessageNameId <- (json \\ SepaMessageCustomField.ORIGINAL_MESSAGE_TYPE.toString).headOption.flatMap(_.asString)
          } yield OriginalGroupInformation21(originalSepaMessageId, originalSepaMessageNameId)
        ),
        TxInf = creditTransferTransactions.map(transaction =>
          PaymentTransactionInformation27(
            RtrId = Some(transaction._2),
            OrgnlGrpInf = transaction._1.customFields.flatMap(json =>
              for {
                originalSepaMessageId <- (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).headOption.flatMap(_.asString)
                  .orElse((json \\ SepaMessageCustomField.ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString).headOption.flatMap(_.asString))
                originalSepaMessageNameId <- (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINAL_MESSAGE_TYPE.toString).headOption.flatMap(_.asString)
                  .orElse((json \\ SepaMessageCustomField.ORIGINAL_MESSAGE_TYPE.toString).headOption.flatMap(_.asString))
              } yield OriginalGroupInformation3(originalSepaMessageId, originalSepaMessageNameId)
            ),
            OrgnlInstrId = transaction._1.instructionId,
            OrgnlEndToEndId = Some(transaction._1.endToEndId),
            OrgnlTxId = Some(transaction._1.transactionIdInSepaFile),
            OrgnlIntrBkSttlmAmt = Some(ActiveOrHistoricCurrencyAndAmount(transaction._1.amount, Map(("@Ccy", DataRecord("EUR"))))),
            RtrdIntrBkSttlmAmt = ActiveCurrencyAndAmount(transaction._1.amount, Map(("@Ccy", DataRecord("EUR")))),
            RtrdInstdAmt = None,
            ChrgBr = Some(SLEV),
            ChrgsInf = transaction._1.customFields.flatMap(json =>
              (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_CHARGES_AMOUNT.toString).headOption.flatMap(_.asString))
              .flatMap(chargesAmountString => Try(BigDecimal(chargesAmountString)).toOption)
              .map(chargesAmount => ChargesInformation5(
                Amt = ActiveOrHistoricCurrencyAndAmount(value = chargesAmount, Map(("@Ccy", DataRecord("EUR")))),
                Pty = BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(BIC =
                  transaction._1.customFields.flatMap(json =>
                    (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_CHARGES_PARTY.toString).headOption.flatMap(_.asString))
                ))
              )).toSeq,
            InstgAgt = message.instigatingAgent.map(agent => BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some(agent.bic)))),
            InstdAgt = message.instigatingAgent.map(agent => BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some(agent.bic)))),
            RtrRsnInf = Seq(ReturnReasonInformation9(
              Orgtr = transaction._1.customFields.flatMap(json => (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINATOR.toString).headOption.flatMap(_.asString))
                .map {
                  case originator if SepaUtil.isBic(originator) =>
                    PartyIdentification32(Id = Some(Party6Choice(DataRecord(<OrgId></OrgId>, OrganisationIdentification4(BICOrBEI = Some(originator))))))
                  case originator => PartyIdentification32(Nm = Some(originator))
                },
              Rsn = transaction._1.customFields.flatMap(json => (json \\ SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_REASON_CODE.toString).headOption)
                .map(reasonCode => ReturnReason5Choice(DataRecord(<Cd></Cd>, reasonCode.asString.getOrElse(""))))
            )),
            Some(OriginalTransactionReference13(
              IntrBkSttlmDt = {
                transaction._1.settlementDate.map(date => {
                  val IntrBkSttlmDt = DatatypeFactory.newInstance().newXMLGregorianCalendar
                  IntrBkSttlmDt.setYear(date.getYear)
                  IntrBkSttlmDt.setMonth(date.getMonthValue)
                  IntrBkSttlmDt.setDay(date.getDayOfMonth)
                  IntrBkSttlmDt
                })
              },
              SttlmInf = transaction._1.settlementInformation.map(_.toSettlementInformation13PR),
              PmtTpInf = transaction._1.paymentTypeInformation.map(_.toPaymentTypeInformation22),
              RmtInf = transaction._1.description.map(description => RemittanceInformation5(Ustrd = Seq(description))),
              Dbtr = transaction._1.debtor.map(_.toPartyIdentification32),
              DbtrAcct = transaction._1.debtorAccount.map(account => CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
              DbtrAgt = transaction._1.debtorAgent.map(debtorAgent => BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = Some(debtorAgent.bic)))),
              UltmtDbtr = transaction._1.ultimateDebtor.map(_.toPartyIdentification32),
              Cdtr = transaction._1.creditor.map(_.toPartyIdentification32),
              CdtrAcct = transaction._1.creditorAccount.map(account => CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
              CdtrAgt = transaction._1.creditorAgent.map(creditorAgent => BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = Some(creditorAgent.bic)))),
              UltmtCdtr = transaction._1.ultimateCreditor.map(_.toPartyIdentification32)
            ))
          )
        )
      )
    )
    scalaxb.toXML[Document](document, "Document", defaultScope)
  }
}

object PaymentReturnMessage {
  def fromXML(xmlFile: Elem, sepaFileId: UUID): Try[PaymentReturnMessage] = {
    Try(scalaxb.fromXML[Document](xmlFile)).map(document =>
      PaymentReturnMessage(
        SepaMessage(
          id = UUID.randomUUID(),
          creationDateTime = document.PmtRtr.GrpHdr.CreDtTm.toGregorianCalendar.toZonedDateTime.toLocalDateTime,
          messageType = SepaMessageType.B2B_PAYMENT_RETURN,
          status = SepaMessageStatus.PROCESSING_IN_PROGRESS,
          sepaFileId = Some(sepaFileId),
          messageIdInSepaFile = document.PmtRtr.GrpHdr.MsgId,
          numberOfTransactions = document.PmtRtr.GrpHdr.NbOfTxs.toInt,
          totalAmount = document.PmtRtr.GrpHdr.TtlRtrdIntrBkSttlmAmt.map(_.value).getOrElse(document.PmtRtr.TxInf.map(_.RtrdIntrBkSttlmAmt.value).sum),
          settlementDate = document.PmtRtr.GrpHdr.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate),
          instigatingAgent = document.PmtRtr.GrpHdr.InstgAgt.flatMap(_.FinInstnId.BIC.map(Bic)),
          instigatedAgent = document.PmtRtr.GrpHdr.InstdAgt.flatMap(_.FinInstnId.BIC.map(Bic)),
          customFields = document.PmtRtr.OrgnlGrpInf.map(originalGroupInfo => Json.fromJsonObject(JsonObject.empty
            .add(SepaMessageCustomField.PAYMENT_RETURN_SETTLEMENT_INFORMATION.toString,
              SettlementInformation.fromSettlementInformation13(document.PmtRtr.GrpHdr.SttlmInf).toJson)
            .add(SepaMessageCustomField.ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString, Json.fromString(originalGroupInfo.OrgnlMsgId))
            .add(SepaMessageCustomField.ORIGINAL_MESSAGE_TYPE.toString, Json.fromString(originalGroupInfo.OrgnlMsgNmId))))
        ),
        creditTransferTransactions = document.PmtRtr.TxInf.map(xmlTransaction => {
          val originalSepaCreditTransferTransaction = SepaCreditTransferTransaction(
            UUID.randomUUID(), xmlTransaction.OrgnlIntrBkSttlmAmt.map(_.value).getOrElse(xmlTransaction.RtrdIntrBkSttlmAmt.value),
            debtor = xmlTransaction.OrgnlTxRef.flatMap(_.Dbtr).map(Party.fromPartyIdentification32),
            xmlTransaction.OrgnlTxRef.flatMap(_.DbtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString))),
            xmlTransaction.OrgnlTxRef.flatMap(_.DbtrAgt.flatMap(agent => agent.FinInstnId.BIC.map(Bic))),
            ultimateDebtor = xmlTransaction.OrgnlTxRef.flatMap(_.UltmtDbtr).map(Party.fromPartyIdentification32),
            creditor = xmlTransaction.OrgnlTxRef.flatMap(_.Cdtr).map(Party.fromPartyIdentification32),
            xmlTransaction.OrgnlTxRef.flatMap(_.CdtrAcct.map(account => Iban(account.Id.accountidentification4choicableoption.value.toString))),
            xmlTransaction.OrgnlTxRef.flatMap(_.CdtrAgt.flatMap(agent => agent.FinInstnId.BIC.map(Bic))),
            ultimateCreditor = xmlTransaction.OrgnlTxRef.flatMap(_.UltmtCdtr).map(Party.fromPartyIdentification32),
            None,
            xmlTransaction.OrgnlTxRef.flatMap(_.RmtInf.flatMap(_.Ustrd.headOption)),
            creationDateTime = LocalDateTime.now(),
            settlementDate = xmlTransaction.OrgnlTxRef.flatMap(_.IntrBkSttlmDt.map(_.toGregorianCalendar.toZonedDateTime.toLocalDate)),
            transactionIdInSepaFile = xmlTransaction.OrgnlTxId.getOrElse(""),
            xmlTransaction.OrgnlInstrId, xmlTransaction.OrgnlEndToEndId.getOrElse(""),
            settlementInformation = xmlTransaction.OrgnlTxRef.flatMap(_.SttlmInf).map(SettlementInformation.fromSettlementInformation13),
            paymentTypeInformation = xmlTransaction.OrgnlTxRef.flatMap(_.PmtTpInf).map(PaymentTypeInformation.fromPaymentTypeInformation22),
            status = SepaCreditTransferTransactionStatus.RETURNED,
            customFields = Some(Json.fromJsonObject(JsonObject.empty
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString,
                Json.fromString(xmlTransaction.OrgnlGrpInf.map(_.OrgnlMsgId).orElse(document.PmtRtr.OrgnlGrpInf.map(_.OrgnlMsgId)).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINAL_MESSAGE_TYPE.toString,
                Json.fromString(xmlTransaction.OrgnlGrpInf.map(_.OrgnlMsgNmId).orElse(document.PmtRtr.OrgnlGrpInf.map(_.OrgnlMsgNmId)).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINATOR.toString,
                Json.fromString(xmlTransaction.RtrRsnInf.headOption.flatMap(_.Orgtr.flatMap(originator => originator.Nm.
                  orElse(originator.Id.flatMap(_.party6choicableoption.value match {
                    case ordId: OrganisationIdentification4 => ordId.BICOrBEI
                  })))).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_REASON_CODE.toString,
                Json.fromString(xmlTransaction.RtrRsnInf.headOption.flatMap(_.Rsn.map(_.returnreason5choicableoption.value)).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_CHARGES_AMOUNT.toString,
                Json.fromString(xmlTransaction.ChrgsInf.headOption.map(_.Amt.value.toString).getOrElse("")))
              .add(SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_CHARGES_PARTY.toString,
                Json.fromString(xmlTransaction.ChrgsInf.headOption.flatMap(_.Pty.FinInstnId.BIC).getOrElse("")))
            ))
          )
          (originalSepaCreditTransferTransaction, xmlTransaction.RtrId.getOrElse(""))
        })
      )
    )
  }


  def returnTransaction(transactionToReturn: SepaCreditTransferTransaction, originator: String, paymentReturnReasonCode: PaymentReturnReasonCode, obpTransactionRequestId: Option[TransactionRequestId] = None, obpTransactionId: Option[TransactionId] = None): Future[Unit] = {
    for {
      originalSepaMessage <- SepaMessage.getBySepaCreditTransferTransactionId(transactionToReturn.id).map(_.find(_.messageType == SepaMessageType.B2B_CREDIT_TRANSFER))
      returnSepaMessage <- SepaMessage.getUnprocessedByType(SepaMessageType.B2B_PAYMENT_RETURN).map(_.headOption).flatMap {
        case Some(returnMessage) => Future.successful(returnMessage)
        case None =>
          val sepaMessageId = UUID.randomUUID()
          val message = SepaMessage(
            sepaMessageId, LocalDateTime.now(), SepaMessageType.B2B_PAYMENT_RETURN,
            SepaMessageStatus.UNPROCESSED, sepaFileId = None, SepaUtil.removeDashesToUUID(sepaMessageId),
            numberOfTransactions = 0, totalAmount = 0, None, None, None,
            customFields = Some(Json.fromJsonObject(JsonObject.empty
              .add(SepaMessageCustomField.PAYMENT_RETURN_SETTLEMENT_INFORMATION.toString,
                SettlementInformation(settlementMethod = SettlementMethod.CLEARING_SYSTEM).toJson)
            ))
          )
          for {
            _ <- message.insert()
          } yield message
      }
      _ <- transactionToReturn.copy(
        status = SepaCreditTransferTransactionStatus.TO_RETURN,
        customFields = Some(transactionToReturn.customFields.getOrElse(Json.fromJsonObject(JsonObject.empty))
          .deepMerge(Json.fromJsonObject(JsonObject.empty
            .add(SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINAL_MESSAGE_ID_IN_SEPA_FILE.toString,
              Json.fromString(originalSepaMessage.map(_.messageIdInSepaFile).getOrElse("")))
            .add(SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINAL_MESSAGE_TYPE.toString,
              Json.fromString(originalSepaMessage.map(_.messageType.toString).getOrElse("")))
            .add(SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_ORIGINATOR.toString,
              Json.fromString(originator))
            .add(SepaCreditTransferTransactionCustomField.PAYMENT_RETURN_REASON_CODE.toString,
              Json.fromString(paymentReturnReasonCode.toString)))))
      ).update()
      transactionStatusIdInSepaFile = SepaUtil.removeDashesToUUID(UUID.randomUUID())
      _ <- transactionToReturn.linkMessage(returnSepaMessage.id, transactionStatusIdInSepaFile, obpTransactionRequestId, obpTransactionId)
      _ <- returnSepaMessage.copy(
        numberOfTransactions = returnSepaMessage.numberOfTransactions + 1,
        totalAmount = returnSepaMessage.totalAmount + transactionToReturn.amount
      ).update()
    } yield ()
  }
}



