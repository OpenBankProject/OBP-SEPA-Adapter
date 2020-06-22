package model.archives

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import model.enums.TransactionStatus.TransactionStatus
import model.enums.TransactionType.TransactionType

case class CardTransaction(override val id: UUID,
                           override val accountId: UUID,
                           override val transactionType: TransactionType,
                           override val status: TransactionStatus,
                           override val amount: BigDecimal,
                           override val date: LocalDateTime,
                           override val oldBalance: BigDecimal,
                           override val newBalance: BigDecimal,
                           cardId: UUID,
                           merchantName: String,
                           clearingDate: Option[LocalDate],
                           clearingFileId: Option[UUID])
  extends Transaction(id, accountId, transactionType, status, amount, date, oldBalance, newBalance)
