package model

import java.time.LocalDate
import java.util.UUID

import model.enums.SDDMandateStatus.SDDMandateStatus
import model.enums.SDDMandateType.SDDMandateType
import model.types.{Bic, Iban}

case class SDDMandate(
                       id: UUID,
                       reference: String,
                       customerId: UUID,
                       customerAccountId: UUID,
                       billerName: String,
                       billerReference: String,
                       billerIban: Iban,
                       billerBic: Bic,
                       mandateType: SDDMandateType,
                       recurrent: Boolean,
                       status: SDDMandateStatus,
                       implementationDate: LocalDate
                     )