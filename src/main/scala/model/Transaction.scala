package model

import java.time.LocalDateTime
import java.util.UUID

import model.enums.TransactionStatus.TransactionStatus
import model.enums.TransactionType.TransactionType

class Transaction(
                   val id: UUID,
                   val accountId: UUID,
                   val transactionType: TransactionType,
                   val status: TransactionStatus,
                   val amount: BigDecimal,
                   val date: LocalDateTime,
                   val oldBalance: BigDecimal,
                   val newBalance: BigDecimal
                 )

case object Transaction {
  def unapply(transaction: Transaction): Some[(UUID, UUID, TransactionType, TransactionStatus, BigDecimal, LocalDateTime, BigDecimal, BigDecimal)] =
    Some(transaction.id, transaction.accountId, transaction.transactionType, transaction.status, transaction.amount, transaction.date, transaction.oldBalance, transaction.newBalance)

  def tupled: ((UUID, UUID, TransactionType, TransactionStatus, BigDecimal, LocalDateTime, BigDecimal, BigDecimal)) => Transaction = {
    case (id, accountId, transactionType, status, amount, date, oldBalance, newBalance) => new Transaction(id, accountId, transactionType, status, amount, date, oldBalance, newBalance)
  }
}
