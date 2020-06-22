package model

import java.util.UUID

import com.openbankproject.commons.model.Iban
import generated._
import generated.`package`.defaultScope
import model.types.Bic
import scalaxb._
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
                                          descripton: Option[String],
                                          sepaMessageId: Option[UUID],
                                          idInSepaFile: String,
                                          instructionId: Option[String],
                                          endToEndId: String
                                        ) {
  def insert(): Future[Unit] = Schema.db.run(DBIOAction.seq(Schema.sepaCreditTransferTransactions += this))

  def update(): Future[Unit] = Schema.db.run(DBIOAction.seq(Schema.sepaCreditTransferTransactions.filter(_.id === this.id).update(this)))

  def toXML: NodeSeq = {
    val xmlTransaction = CreditTransferTransactionInformation11(
      PmtId = PaymentIdentification3(
        EndToEndId = endToEndId,
        TxId = idInSepaFile,
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
      RmtInf = descripton.map(description => RemittanceInformation5(Ustrd = Seq(description)))
    )
    scalaxb.toXML[CreditTransferTransactionInformation11](xmlTransaction, "", defaultScope)
  }
}

object SepaCreditTransferTransaction {
  def fromXML(transaction: CreditTransferTransactionInformation11able, sepaMessageId: UUID): SepaCreditTransferTransaction = {
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
      sepaMessageId = Some(sepaMessageId),
      idInSepaFile = transaction.PmtId.TxId,
      instructionId = transaction.PmtId.InstrId,
      endToEndId = transaction.PmtId.EndToEndId
    )
  }

  def getById(id: UUID): Future[Seq[SepaCreditTransferTransaction]] = Schema.db.run(Schema.sepaCreditTransferTransactions.filter(_.id === id).result)

  def getBySepaMessageId(sepaMessageId: UUID): Future[Seq[SepaCreditTransferTransaction]] = Schema.db.run(Schema.sepaCreditTransferTransactions.filter(_.sepaMessageId === sepaMessageId).result)

  def getUnprocessed: Future[Seq[SepaCreditTransferTransaction]] = Schema.db.run(Schema.sepaCreditTransferTransactions.filter(_.sepaMessageId.isEmpty).result)

}
