package adapter.obpApiModel

import java.time.format.DateTimeFormatter

import io.circe.Json

/**
 * this define the structure of the body for a POST request on the saveHistoricalTransactionEndpoint
 */
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

object HistoricalTransactionJson {
  val jsonDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
}
