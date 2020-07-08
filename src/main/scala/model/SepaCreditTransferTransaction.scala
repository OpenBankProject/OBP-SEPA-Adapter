package model

import java.time.LocalDateTime
import java.util.UUID

import com.openbankproject.commons.model.Iban
import io.circe.Json
import model.Schema.sepaCreditTransferTransactionStatusColumnType
import model.enums.SepaCreditTransferTransactionStatus
import model.enums.SepaCreditTransferTransactionStatus.SepaCreditTransferTransactionStatus
import model.types.Bic
import scalaxb._
import sepa.sct.generated.creditTransfer._
import sepa.sct.generated.creditTransfer.`package`.defaultScope
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.xml.NodeSeq

case class SepaCreditTransferTransaction(
                                          id: UUID,
                                          amount: BigDecimal,
                                          debtorName: Option[String],
                                          debtorAccount: Option[Iban],
                                          debtorAgent: Option[Bic],
                                          creditorName: Option[String],
                                          creditorAccount: Option[Iban],
                                          creditorAgent: Option[Bic],
                                          purposeCode: Option[String],
                                          description: Option[String],
                                          creationDateTime: LocalDateTime,
                                          transactionIdInSepaFile: String,
                                          instructionId: Option[String],
                                          endToEndId: String,
                                          status: SepaCreditTransferTransactionStatus,
                                          customFields: Option[Json]
                                          // TODO : Add fields settlementDate, settlementInformation, paymentInformation, ...
                                        ) {
  def insert(): Future[Unit] = Schema.db.run(DBIOAction.seq(Schema.sepaCreditTransferTransactions += this))

  def update(): Future[Unit] = Schema.db.run(DBIOAction.seq(Schema.sepaCreditTransferTransactions.filter(_.id === this.id).update(this)))

  def linkMessage(sepaMessageId: UUID, transactionStatusIdInSepaFile: String, obpTransactionRequestId: Option[UUID], obpTransactionId: Option[UUID]): Future[Unit] = Schema.db.run(
    DBIOAction.seq(Schema.sepaTransactionMessages += SepaTransactionMessage(this.id, sepaMessageId, transactionStatusIdInSepaFile, obpTransactionRequestId, obpTransactionId))
  )

  def updateMessageLink(sepaMessageId: UUID, transactionStatusIdInSepaFile: String, obpTransactionRequestId: Option[UUID], obpTransactionId: Option[UUID]): Future[Unit] = Schema.db.run(
    DBIOAction.seq(
      Schema.sepaTransactionMessages
        .filter(transactionMessage => transactionMessage.sepaCreditTransferTransactionId === this.id && transactionMessage.sepaMessageId === sepaMessageId)
        .map(transactionMessage => (transactionMessage.transactionStatusIdInSepaFile, transactionMessage.obpTransactionRequestId, transactionMessage.obpTransactionId))
        .update((transactionStatusIdInSepaFile, obpTransactionRequestId, obpTransactionId))
    )
  )

  def toXML: NodeSeq = {
    val xmlTransaction = CreditTransferTransactionInformation11(
      PmtId = PaymentIdentification3(
        EndToEndId = endToEndId,
        TxId = transactionIdInSepaFile,
        InstrId = instructionId
      ),
      IntrBkSttlmAmt = ActiveCurrencyAndAmount(value = amount, Map(("@Ccy", DataRecord("EUR")))),
      ChrgBr = SLEV,
      Dbtr = PartyIdentification32(
        Nm = debtorName
      ),
      DbtrAcct = debtorAccount.map(account => CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
      DbtrAgt = BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = debtorAgent.map(_.bic))),
      Cdtr = PartyIdentification32(
        Nm = creditorName,
      ),
      CdtrAcct = creditorAccount.map(account => CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, account.iban)))),
      CdtrAgt = BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = creditorAgent.map(_.bic))),
      Purp = purposeCode.map(purposeCode => Purpose2Choice(DataRecord(<Cd></Cd>, purposeCode))),
      RmtInf = description.map(description => RemittanceInformation5(Ustrd = Seq(description)))
    )
    scalaxb.toXML[CreditTransferTransactionInformation11](xmlTransaction, "", defaultScope)
  }
}

object SepaCreditTransferTransaction {
  def fromXML(transaction: CreditTransferTransactionInformation11able, creationDateTime: LocalDateTime): SepaCreditTransferTransaction = {
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
      description = transaction.RmtInf.flatMap(_.Ustrd.headOption),
      creationDateTime = LocalDateTime.now(),
      transactionIdInSepaFile = transaction.PmtId.TxId,
      instructionId = transaction.PmtId.InstrId,
      endToEndId = transaction.PmtId.EndToEndId,
      status = SepaCreditTransferTransactionStatus.UNPROCESSED,
      customFields = None
    )
  }

  def getById(id: UUID): Future[Option[SepaCreditTransferTransaction]] = Schema.db.run(Schema.sepaCreditTransferTransactions.filter(_.id === id).result.headOption)

  def getByTransactionIdInSepaFile(transactionIdInSepaFile: String): Future[Option[SepaCreditTransferTransaction]] = Schema.db.run(Schema.sepaCreditTransferTransactions.filter(_.transactionIdInSepaFile === transactionIdInSepaFile).result.headOption)

  def getByTransactionStatusIdInSepaFile(transactionStatusIdInSepaFile: String): Future[Option[SepaCreditTransferTransaction]] =
    Schema.db.run(
      Schema.sepaTransactionMessages
        .filter(_.transactionStatusIdInSepaFile === transactionStatusIdInSepaFile)
        .join(Schema.sepaCreditTransferTransactions)
        .on((transactionMessage, transaction) => transactionMessage.sepaCreditTransferTransactionId === transaction.id)
        .map(_._2)
        .result.headOption
    )

  def getUnprocessed: Future[Seq[SepaCreditTransferTransaction]] = Schema.db.run(Schema.sepaCreditTransferTransactions.filter(_.status === SepaCreditTransferTransactionStatus.UNPROCESSED).result)

  def getBySepaMessageId(messageId: UUID): Future[Seq[(SepaCreditTransferTransaction, String)]] =
    Schema.db.run(
      Schema.sepaTransactionMessages
        .filter(_.sepaMessageId === messageId)
        .join(Schema.sepaCreditTransferTransactions)
        .on((transactionMessage, transaction) => transactionMessage.sepaCreditTransferTransactionId === transaction.id)
        .map(a => (a._2, a._1.transactionStatusIdInSepaFile))
        .result
    )
}
