package model.archives

import java.time.LocalDate
import java.util.UUID

import model.archives.enums.CardPaymentNetwork.CardPaymentNetwork
import model.archives.enums.CardPaymentStatus.CardPaymentStatus
import model.archives.enums.CardRange.CardRange
import model.archives.enums.CardStatus.CardStatus
import model.archives.enums.CardType.CardType

case class Card(
                 id: UUID,
                 customerId: UUID,
                 accountId: UUID,
                 pan: String,
                 cvv: String,
                 pin: String,
                 creationDate: LocalDate,
                 activationDate: Option[LocalDate],
                 oppositionDate: Option[LocalDate],
                 expirationDate: LocalDate,
                 paymentNetwork: CardPaymentNetwork,
                 cardType: CardType,
                 range: CardRange,
                 status: CardStatus,
                 withdrawalStatus: CardPaymentStatus,
                 internetPaymentStatus: CardPaymentStatus,
                 abroadPaymentStatus: CardPaymentStatus,
                 nfcPaymentStatus: CardPaymentStatus,
                 nfcPaidAmount: BigDecimal,
                 nfcPaymentLimit: BigDecimal,
                 paidAmount: BigDecimal,
                 paymentLimit: BigDecimal,
                 withdrawnAmount: BigDecimal,
                 withdrawalLimit: BigDecimal
               )
