package adapter.obpApiModel

import scala.collection.immutable.List


case class TransactionRequestBodySEPAJsonV400(
                                               value: AmountOfMoneyJsonV121,
                                               to: IbanJson,
                                               description: String,
                                               charge_policy: String,
                                               future_date: Option[String] = None,
                                               reasons: Option[List[TransactionRequestReasonJsonV400]] = None
                                             ) extends TransactionRequestCommonBodyJSON

case class TransactionRequestReasonJsonV400(
                                             code: String,
                                             document_number: Option[String],
                                             amount: Option[String],
                                             currency: Option[String],
                                             description: Option[String]
                                           )

case class IbanJson(val iban: String)
