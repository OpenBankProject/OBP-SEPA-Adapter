package model

import java.util.UUID

import model.enums.AccountStatus.AccountStatus
import model.enums.AccountType.AccountType
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
