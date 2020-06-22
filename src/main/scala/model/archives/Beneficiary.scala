package model.archives

import java.util.UUID

import model.types.{Bic, Iban}

case class Beneficiary(
                        id: UUID,
                        customerId: UUID,
                        iban: Iban,
                        bic: Bic,
                        name: String,
                        label: Option[String],
                      )
