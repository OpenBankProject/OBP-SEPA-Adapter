package adapter.obpApiModel

import java.time.format.DateTimeFormatter
import java.util.Date

import io.circe.Json

/**
 * this define the structure of the body for a POST request on the saveHistoricalTransactionEndpoint
 */

case class PostHistoricalTransactionJson(
                                          from: HistoricalTransactionAccountJsonV310,
                                          to: HistoricalTransactionAccountJsonV310,
                                          value: AmountOfMoneyJsonV121,
                                          description: String,
                                          posted: String,
                                          completed: String,
                                          `type`: String,
                                          charge_policy: String
                                        )

object PostHistoricalTransactionJson {
  val jsonDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
}

case class HistoricalTransactionAccountJsonV310(
                                                 bank_id: Option[String],
                                                 account_id: Option[String],
                                                 counterparty_id: Option[String],
                                               )

case class PostHistoricalTransactionResponseJson(
                                                  transaction_id: String,
                                                  from: HistoricalTransactionAccountJsonV310,
                                                  to: HistoricalTransactionAccountJsonV310,
                                                  value: AmountOfMoneyJsonV121,
                                                  description: String,
                                                  posted: Date,
                                                  completed: Date,
                                                  transaction_request_type: String,
                                                  charge_policy: String
                                                )
