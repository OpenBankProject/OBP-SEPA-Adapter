package adapter.obpApiModel

/**
 * This define the structure of the body for a POST request on the createTransactionRequest (REFUND) endpoint
 */

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

