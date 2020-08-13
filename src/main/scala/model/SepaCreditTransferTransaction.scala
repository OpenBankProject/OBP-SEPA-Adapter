package model

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import com.openbankproject.commons.model.{Iban, TransactionId, TransactionRequestId}
import io.circe.Json
import model.Schema.{obpTransactionIdColumnType, obpTransactionRequestIdColumnType}
import model.enums.SepaCreditTransferTransactionStatus.SepaCreditTransferTransactionStatus
import model.jsonClasses.Party
import model.types.Bic
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SepaCreditTransferTransaction(
                                          id: UUID,
                                          amount: BigDecimal,
                                          debtor: Option[Party],
                                          debtorAccount: Option[Iban],
                                          debtorAgent: Option[Bic],
                                          ultimateDebtor: Option[Party],
                                          creditor: Option[Party],
                                          creditorAccount: Option[Iban],
                                          creditorAgent: Option[Bic],
                                          ultimateCreditor: Option[Party],
                                          purposeCode: Option[String],
                                          description: Option[String],
                                          creationDateTime: LocalDateTime,
                                          settlementDate: Option[LocalDate],
                                          transactionIdInSepaFile: String,
                                          instructionId: Option[String],
                                          endToEndId: String,
                                          status: SepaCreditTransferTransactionStatus,
                                          customFields: Option[Json]
                                          // TODO : Add missing fields : Settlement information, payment information
                                        ) {
  def insert(): Future[Unit] = Schema.db.run(DBIOAction.seq(Schema.sepaCreditTransferTransactions += this))

  def update(): Future[Unit] = Schema.db.run(DBIOAction.seq(Schema.sepaCreditTransferTransactions.filter(_.id === this.id).update(this)))

  def linkMessage(sepaMessageId: UUID, transactionStatusIdInSepaFile: String, obpTransactionRequestId: Option[TransactionRequestId], obpTransactionId: Option[TransactionId]): Future[Unit] = Schema.db.run(
    DBIOAction.seq(Schema.sepaTransactionMessages += SepaTransactionMessage(this.id, sepaMessageId, transactionStatusIdInSepaFile, obpTransactionRequestId, obpTransactionId))
  )

  def updateMessageLink(sepaMessageId: UUID, transactionStatusIdInSepaFile: String, obpTransactionRequestId: Option[TransactionRequestId], obpTransactionId: Option[TransactionId]): Future[Unit] = Schema.db.run(
    DBIOAction.seq(
      Schema.sepaTransactionMessages
        .filter(transactionMessage => transactionMessage.sepaCreditTransferTransactionId === this.id && transactionMessage.sepaMessageId === sepaMessageId)
        .map(transactionMessage => (transactionMessage.transactionStatusIdInSepaFile, transactionMessage.obpTransactionRequestId, transactionMessage.obpTransactionId))
        .update((transactionStatusIdInSepaFile, obpTransactionRequestId, obpTransactionId))
    )
  )
}

object SepaCreditTransferTransaction {
  def getById(id: UUID): Future[SepaCreditTransferTransaction] =
    Schema.db.run(Schema.sepaCreditTransferTransactions.filter(_.id === id).result.headOption).flatMap {
      case Some(sepaCreditTransferTransaction) => Future.successful(sepaCreditTransferTransaction)
      case None => Future.failed(new Exception(s"None SepaCreditTransferTransaction found with the id $id"))
    }

  def getByTransactionIdInSepaFile(transactionIdInSepaFile: String): Future[SepaCreditTransferTransaction] =
    Schema.db.run(Schema.sepaCreditTransferTransactions.filter(_.transactionIdInSepaFile === transactionIdInSepaFile).result.headOption).flatMap {
      case Some(sepaCreditTransferTransaction) => Future.successful(sepaCreditTransferTransaction)
      case None => Future.failed(new Exception(s"None SepaCreditTransferTransaction found with the transactionIdInSepaFile $transactionIdInSepaFile"))
    }

  def getByTransactionStatusIdInSepaFile(transactionStatusIdInSepaFile: String): Future[SepaCreditTransferTransaction] =
    Schema.db.run(
      Schema.sepaTransactionMessages
        .filter(_.transactionStatusIdInSepaFile === transactionStatusIdInSepaFile)
        .join(Schema.sepaCreditTransferTransactions)
        .on((transactionMessage, transaction) => transactionMessage.sepaCreditTransferTransactionId === transaction.id)
        .map(_._2)
        .result.headOption
    ).flatMap {
      case Some(sepaCreditTransferTransaction) => Future.successful(sepaCreditTransferTransaction)
      case None => Future.failed(new Exception(s"None SepaCreditTransferTransaction found with the transactionStatusIdInSepaFile $transactionStatusIdInSepaFile"))
    }

  def getByObpTransactionId(obpTransactionId: TransactionId): Future[SepaCreditTransferTransaction] =
    Schema.db.run(
      Schema.sepaTransactionMessages
        .filter(_.obpTransactionId === obpTransactionId)
        .join(Schema.sepaCreditTransferTransactions)
        .on((transactionMessage, transaction) => transactionMessage.sepaCreditTransferTransactionId === transaction.id)
        .map(_._2)
        .result.headOption
    ).flatMap {
      case Some(sepaCreditTransferTransaction) => Future.successful(sepaCreditTransferTransaction)
      case None => Future.failed(new Exception(s"None SepaCreditTransferTransaction found with the obpTransactionId $obpTransactionId"))
    }

  def getByObpTransactionRequestId(obpTransactionRequestId: TransactionRequestId): Future[SepaCreditTransferTransaction] =
    Schema.db.run(
      Schema.sepaTransactionMessages
        .filter(_.obpTransactionRequestId === obpTransactionRequestId)
        .join(Schema.sepaCreditTransferTransactions)
        .on((transactionMessage, transaction) => transactionMessage.sepaCreditTransferTransactionId === transaction.id)
        .map(_._2)
        .result.headOption
    ).flatMap {
      case Some(sepaCreditTransferTransaction) => Future.successful(sepaCreditTransferTransaction)
      case None => Future.failed(new Exception(s"None SepaCreditTransferTransaction found with the obpTransactionRequestId $obpTransactionRequestId"))
    }

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
