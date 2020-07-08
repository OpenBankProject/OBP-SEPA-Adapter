package model.archives

import java.util.UUID

import model.archives.enums.AccountStatus.AccountStatus
import model.archives.enums.AccountType.AccountType
import model.types.Iban

case class Account(
                    id: UUID,
                    iban: Iban,
                    customerId: UUID,
                    accountType: AccountType,
                    status: AccountStatus,
                    balance: BigDecimal,
                    accountingBalance: BigDecimal
                  )
