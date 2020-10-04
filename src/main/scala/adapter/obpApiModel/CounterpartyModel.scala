package adapter.obpApiModel

import java.util.Date

import scala.collection.immutable.List

case class CounterpartiesJson400(
                                  counterparties: List[CounterpartyJson400]
                                )

case class CounterpartyJson400(
                                name: String,
                                description: String,
                                currency: String,
                                created_by_user_id: String,
                                this_bank_id: String,
                                this_account_id: String,
                                this_view_id: String,
                                counterparty_id: String,
                                other_bank_routing_scheme: String,
                                other_bank_routing_address: String,
                                other_branch_routing_scheme: String,
                                other_branch_routing_address: String,
                                other_account_routing_scheme: String,
                                other_account_routing_address: String,
                                other_account_secondary_routing_scheme: String,
                                other_account_secondary_routing_address: String,
                                is_beneficiary: Boolean,
                                bespoke: List[PostCounterpartyBespokeJson]
                              )

case object CounterpartyJson400 {
  def fromCounterpartyWithMetadataJson400(counterpartyWithMetadataJson400: CounterpartyWithMetadataJson400): CounterpartyJson400 =
    CounterpartyJson400(
      name = counterpartyWithMetadataJson400.name,
      description = counterpartyWithMetadataJson400.description,
      currency = counterpartyWithMetadataJson400.currency,
      created_by_user_id = counterpartyWithMetadataJson400.created_by_user_id,
      this_bank_id = counterpartyWithMetadataJson400.this_bank_id,
      this_account_id = counterpartyWithMetadataJson400.this_account_id,
      this_view_id = counterpartyWithMetadataJson400.this_view_id,
      counterparty_id = counterpartyWithMetadataJson400.counterparty_id,
      other_bank_routing_scheme = counterpartyWithMetadataJson400.other_bank_routing_scheme,
      other_bank_routing_address = counterpartyWithMetadataJson400.other_bank_routing_address,
      other_branch_routing_scheme = counterpartyWithMetadataJson400.other_branch_routing_scheme,
      other_branch_routing_address = counterpartyWithMetadataJson400.other_branch_routing_address,
      other_account_routing_scheme = counterpartyWithMetadataJson400.other_account_routing_scheme,
      other_account_routing_address = counterpartyWithMetadataJson400.other_account_routing_address,
      other_account_secondary_routing_scheme = counterpartyWithMetadataJson400.other_account_secondary_routing_scheme,
      other_account_secondary_routing_address = counterpartyWithMetadataJson400.other_account_secondary_routing_address,
      is_beneficiary = counterpartyWithMetadataJson400.is_beneficiary,
      bespoke = counterpartyWithMetadataJson400.bespoke
    )
}

case class PostCounterpartyBespokeJson(
                                        key: String,
                                        value: String
                                      )


case class PostCounterpartyJson400(
                                    name: String,
                                    description: String,
                                    currency: String,
                                    other_account_routing_scheme: String,
                                    other_account_routing_address: String,
                                    other_account_secondary_routing_scheme: String,
                                    other_account_secondary_routing_address: String,
                                    other_bank_routing_scheme: String,
                                    other_bank_routing_address: String,
                                    other_branch_routing_scheme: String,
                                    other_branch_routing_address: String,
                                    is_beneficiary: Boolean,
                                    bespoke: List[PostCounterpartyBespokeJson]
                                  )

case class CounterpartyWithMetadataJson400(
                                            name: String,
                                            description: String,
                                            currency: String,
                                            created_by_user_id: String,
                                            this_bank_id: String,
                                            this_account_id: String,
                                            this_view_id: String,
                                            counterparty_id: String,
                                            other_bank_routing_scheme: String,
                                            other_bank_routing_address: String,
                                            other_branch_routing_scheme: String,
                                            other_branch_routing_address: String,
                                            other_account_routing_scheme: String,
                                            other_account_routing_address: String,
                                            other_account_secondary_routing_scheme: String,
                                            other_account_secondary_routing_address: String,
                                            is_beneficiary: Boolean,
                                            bespoke: List[PostCounterpartyBespokeJson],
                                            metadata: CounterpartyMetadataJson
                                          )

case class CounterpartyMetadataJson(
                                     public_alias: String,
                                     more_info: String,
                                     url: String,
                                     image_url: String,
                                     open_corporates_url: String,
                                     corporate_location: Option[LocationJsonV210],
                                     physical_location: Option[LocationJsonV210],
                                     private_alias: String
                                   )

case class LocationJsonV210(
                             latitude: Double,
                             longitude: Double,
                             date: Date,
                             user: UserJSONV210
                           )

case class UserJSONV210(
                         id: String,
                         provider: String,
                         username: String
                       )