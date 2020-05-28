package model

import java.time.LocalDate
import java.util.UUID

import model.enums.CardPaymentNetwork.CardPaymentNetwork
import model.enums.CardPaymentStatus.CardPaymentStatus
import model.enums.CardRange.CardRange
import model.enums.CardStatus.CardStatus
import model.enums.CardType.CardType

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
