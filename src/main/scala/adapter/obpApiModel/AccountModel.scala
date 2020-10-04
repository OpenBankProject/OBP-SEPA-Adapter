package adapter.obpApiModel

import java.util.Date

case class BankAccountRoutingJson(
                                   bank_id: Option[String],
                                   account_routing: AccountRoutingJsonV121
                                 )

case class AccountRoutingJsonV121(
                                   scheme: String,
                                   address: String
                                 )

case class ModeratedAccountJSON400(
                                    id: String,
                                    label: String,
                                    number: String,
                                    owners: List[UserJSONV121],
                                    product_code: String,
                                    balance: AmountOfMoneyJsonV121,
                                    views_available: List[ViewJSONV121],
                                    bank_id: String,
                                    account_routings: List[AccountRoutingJsonV121],
                                    account_attributes: List[AccountAttributeResponseJson],
                                    tags: List[AccountTagJSON]
                                  )

case class UserJSONV121(
                         id: String,
                         provider: String,
                         display_name: String
                       )



case class ViewJSONV121(
                         val id: String,
                         val short_name: String,
                         val description: String,
                         val is_public: Boolean,
                         val alias: String,
                         val hide_metadata_if_alias_used: Boolean,
                         val can_add_comment: Boolean,
                         val can_add_corporate_location: Boolean,
                         val can_add_image: Boolean,
                         val can_add_image_url: Boolean,
                         val can_add_more_info: Boolean,
                         val can_add_open_corporates_url: Boolean,
                         val can_add_physical_location: Boolean,
                         val can_add_private_alias: Boolean,
                         val can_add_public_alias: Boolean,
                         val can_add_tag: Boolean,
                         val can_add_url: Boolean,
                         val can_add_where_tag: Boolean,
                         val can_delete_comment: Boolean,
                         val can_delete_corporate_location: Boolean,
                         val can_delete_image: Boolean,
                         val can_delete_physical_location: Boolean,
                         val can_delete_tag: Boolean,
                         val can_delete_where_tag: Boolean,
                         val can_edit_owner_comment: Boolean,
                         val can_see_bank_account_balance: Boolean,
                         val can_see_bank_account_bank_name: Boolean,
                         val can_see_bank_account_currency: Boolean,
                         val can_see_bank_account_iban: Boolean,
                         val can_see_bank_account_label: Boolean,
                         val can_see_bank_account_national_identifier: Boolean,
                         val can_see_bank_account_number: Boolean,
                         val can_see_bank_account_owners: Boolean,
                         val can_see_bank_account_swift_bic: Boolean,
                         val can_see_bank_account_type: Boolean,
                         val can_see_comments: Boolean,
                         val can_see_corporate_location: Boolean,
                         val can_see_image_url: Boolean,
                         val can_see_images: Boolean,
                         val can_see_more_info: Boolean,
                         val can_see_open_corporates_url: Boolean,
                         val can_see_other_account_bank_name: Boolean,
                         val can_see_other_account_iban: Boolean,
                         val can_see_other_account_kind: Boolean,
                         val can_see_other_account_metadata: Boolean,
                         val can_see_other_account_national_identifier: Boolean,
                         val can_see_other_account_number: Boolean,
                         val can_see_other_account_swift_bic: Boolean,
                         val can_see_owner_comment: Boolean,
                         val can_see_physical_location: Boolean,
                         val can_see_private_alias: Boolean,
                         val can_see_public_alias: Boolean,
                         val can_see_tags: Boolean,
                         val can_see_transaction_amount: Boolean,
                         val can_see_transaction_balance: Boolean,
                         val can_see_transaction_currency: Boolean,
                         val can_see_transaction_description: Boolean,
                         val can_see_transaction_finish_date: Boolean,
                         val can_see_transaction_metadata: Boolean,
                         val can_see_transaction_other_bank_account: Boolean,
                         val can_see_transaction_start_date: Boolean,
                         val can_see_transaction_this_bank_account: Boolean,
                         val can_see_transaction_type: Boolean,
                         val can_see_url: Boolean,
                         val can_see_where_tag: Boolean
                       )

case class AccountAttributeResponseJson(
                                         product_code: String,
                                         account_attribute_id: String,
                                         name: String,
                                         `type`: String,
                                         value: String
                                       )

case class AccountTagJSON(
                           id: String,
                           value: String,
                           date: Date,
                           user: UserJSONV121
                         )