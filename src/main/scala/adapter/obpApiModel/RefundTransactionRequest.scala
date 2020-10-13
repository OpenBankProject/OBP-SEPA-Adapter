package adapter.obpApiModel

import java.util.Date

import com.openbankproject.commons.model.TransactionRequestBodyAllTypes

import scala.collection.immutable.List

/**
 * This define the structure of the body for a POST request on the createTransactionRequest (REFUND) endpoint
 */

/* Request body */
trait TransactionRequestCommonBodyJSON {
  val value: AmountOfMoneyJsonV121
  val description: String
}

case class TransactionRequestBodyRefundJsonV400(
                                                 to: Option[TransactionRequestRefundTo],
                                                 from: Option[TransactionRequestRefundFrom],
                                                 value: AmountOfMoneyJsonV121,
                                                 description: String,
                                                 refund: RefundJson
                                               ) extends TransactionRequestCommonBodyJSON

case class TransactionRequestRefundTo(
                                       bank_id: Option[String],
                                       account_id: Option[String],
                                       counterparty_id: Option[String]
                                     )

case class TransactionRequestRefundFrom(
                                         counterparty_id: String
                                       )

case class RefundJson(
                       transaction_id: String,
                       reason_code: String
                     )

/* Request response */

case class TransactionRequestWithChargeJSON210(
                                                id: String,
                                                `type`: String,
                                                from: TransactionRequestAccountJsonV140,
                                                details: TransactionRequestBodyAllTypes,
                                                transaction_ids: List[String],
                                                status: String,
                                                start_date: Date,
                                                end_date: Date,
                                                challenge: Option[ChallengeJsonV140],
                                                charge: TransactionRequestChargeJsonV200
                                              )

case class TransactionRequestAccountJsonV140(
                                              bank_id: String,
                                              account_id: String
                                            )

case class ChallengeJsonV140(
                              id: String,
                              allowed_attempts: Int,
                              challenge_type: String
                            )

case class TransactionRequestChargeJsonV200(
                                             summary: String,
                                             value: AmountOfMoneyJsonV121
                                           )
