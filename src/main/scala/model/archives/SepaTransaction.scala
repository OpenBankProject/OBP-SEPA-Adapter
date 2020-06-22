package model.archives

import java.time.LocalDateTime
import java.util.UUID

import model.enums.TransactionStatus.TransactionStatus
import model.enums.TransactionType.TransactionType
import model.types.{Bic, Iban}

case class SepaTransaction(
                            override val id: UUID,
                            override val accountId: UUID,
                            override val transactionType: TransactionType,
                            override val status: TransactionStatus,
                            override val amount: BigDecimal,
                            override val date: LocalDateTime,
                            override val oldBalance: BigDecimal,
                            override val newBalance: BigDecimal,
                            counterpartIban: Iban,
                            counterpartBic: Bic,
                            counterpartName: String,
                            description: Option[String],
                            processedDate: Option[LocalDateTime],
                            fileId: Option[UUID],
                            fileTransactionId: Option[String],
                          )
  extends Transaction(id, accountId, transactionType, status, amount, date, oldBalance, newBalance)
