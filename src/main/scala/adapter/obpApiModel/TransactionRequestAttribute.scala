package adapter.obpApiModel

import scala.collection.immutable.List


case class TransactionRequestAttributesResponseJson(
                                                     transaction_request_attributes: List[TransactionRequestAttributeResponseJson]
                                                   )

case class TransactionRequestAttributeResponseJson(
                                                    transaction_request_attribute_id: String,
                                                    name: String,
                                                    `type`: String,
                                                    value: String
                                                  )
