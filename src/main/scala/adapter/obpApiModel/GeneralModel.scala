package adapter.obpApiModel

import java.util.Date

import com.openbankproject.commons.model.TransactionRequestBodyAllTypes

import scala.collection.immutable.List

case class AmountOfMoneyJsonV121(
                                  currency: String,
                                  amount: String
                                )

/* Request body */
trait TransactionRequestCommonBodyJSON {
  val value: AmountOfMoneyJsonV121
  val description: String
}

case class TransactionRequestWithChargeJSON400(
                                                id: String,
                                                `type`: String,
                                                from: TransactionRequestAccountJsonV140,
                                                details: TransactionRequestBodyAllTypes,
                                                transaction_ids: List[String],
                                                status: String,
                                                start_date: Date,
                                                end_date: Date,
                                                challenges: Option[List[ChallengeJsonV400]],
                                                charge: TransactionRequestChargeJsonV200
                                              )

case class TransactionRequestAccountJsonV140(
                                              bank_id: String,
                                              account_id: String
                                            )

case class ChallengeJsonV140(
                              id: Option[String],
                              allowed_attempts: Int,
                              challenge_type: Option[String]
                            )

case class ChallengeJsonV400(
                              id: String,
                              user_id: String,
                              allowed_attempts: Int,
                              challenge_type: String,
                              link: String
                            )

case class TransactionRequestChargeJsonV200(
                                             summary: String,
                                             value: AmountOfMoneyJsonV121
                                           )
