package adapter.obpApiModel

import io.circe.Json

/**
 * This define the structure of the body for a POST request on the createTransactionRequest (REFUND) endpoint
 */
case class RefundTransactionRequest(
                                     from: Option[Json] = None,
                                     to: Option[Json] = None,
                                     value: Json,
                                     description: String,
                                     refund: Json
                                   )

case class RefundTransactionRequestCounterpartyIban(
                                                     counterparty_iban: String
                                                   )

case class RefundTransactionRequestContent(
                                            transaction_id: String,
                                            reason_code: String
                                          )