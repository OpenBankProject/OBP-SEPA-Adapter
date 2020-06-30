package adapter

import io.circe.Json

case class HistoricalTransactionJson(
                                      from: Json,
                                      to: Json,
                                      value: Json,
                                      description: String,
                                      posted: String,
                                      completed: String,
                                      `type`: String,
                                      charge_policy: String
                                    )

sealed trait AccountReference

case class CounterpartyAccountReference(
                                         counterparty_iban: String,
                                         bank_bic: Option[String],
                                         counterparty_name: Option[String]
                                       ) extends AccountReference

case class CustomerAccountReference(
                                     account_iban: String,
                                     bank_bic: Option[String]
                                   ) extends AccountReference
