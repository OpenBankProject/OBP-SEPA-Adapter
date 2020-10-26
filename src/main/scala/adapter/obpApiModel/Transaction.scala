package adapter.obpApiModel

import java.util.Date

import scala.collection.immutable.List

case class TransactionJsonV300(
                                id: String,
                                this_account: ThisAccountJsonV300,
                                other_account: OtherAccountJsonV300,
                                details: TransactionDetailsJSON,
                                metadata: TransactionMetadataJSON,
                                transaction_attributes: List[TransactionAttributeResponseJson]
                              )

case class ThisAccountJsonV300(
                                id: String,
                                bank_routing: BankRoutingJsonV121,
                                account_routings: List[AccountRoutingJsonV121],
                                holders: List[AccountHolderJSON]
                              )

case class OtherAccountJsonV300(
                                 id: String,
                                 holder: AccountHolderJSON,
                                 bank_routing: BankRoutingJsonV121,
                                 account_routings: List[AccountRoutingJsonV121],
                                 metadata: OtherAccountMetadataJSON
                               )

case class OtherAccountMetadataJSON(
                                     public_alias: Option[String],
                                     private_alias: Option[String],
                                     more_info: Option[String],
                                     URL: Option[String],
                                     image_URL: Option[String],
                                     open_corporates_URL: Option[String],
                                     corporate_location: Option[LocationJSONV121],
                                     physical_location: Option[LocationJSONV121]
                                   )

case class LocationJSONV121(
                             latitude: Double,
                             longitude: Double,
                             date: Date,
                             user: UserJSONV121
                           )

case class BankRoutingJsonV121(
                                scheme: Option[String],
                                address: Option[String]
                              )

case class AccountHolderJSON(
                              name: String,
                              is_alias: Boolean
                            )

case class TransactionDetailsJSON(
                                   `type`: String,
                                   description: String,
                                   posted: Date,
                                   completed: Date,
                                   new_balance: AmountOfMoneyJsonV121,
                                   value: AmountOfMoneyJsonV121
                                 )

case class TransactionMetadataJSON(
                                    narrative: Option[String],
                                    comments: List[TransactionCommentJSON],
                                    tags: List[TransactionTagJSON],
                                    images: List[TransactionImageJSON],
                                    where: Option[LocationJSONV121]
                                  )

case class TransactionCommentJSON(
                                   id: String,
                                   value: String,
                                   date: Date,
                                   user: UserJSONV121
                                 )

case class TransactionTagJSON(
                               id: String,
                               value: String,
                               date: Date,
                               user: UserJSONV121
                             )

case class TransactionImageJSON(
                                 id: String,
                                 label: String,
                                 URL: String,
                                 date: Date,
                                 user: UserJSONV121
                               )

case class TransactionAttributeResponseJson(
                                             transaction_attribute_id: String,
                                             name: String,
                                             `type`: String,
                                             value: String
                                           )