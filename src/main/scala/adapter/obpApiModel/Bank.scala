package adapter.obpApiModel

import scala.collection.immutable.List

case class BankJson400(
                        id: String,
                        short_name: String,
                        full_name: String,
                        logo: Option[String],
                        website: Option[String],
                        bank_routings: List[BankRoutingJsonV121]
                      )