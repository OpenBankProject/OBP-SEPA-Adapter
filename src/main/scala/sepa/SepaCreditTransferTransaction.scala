package sepa

import java.util.UUID

import com.openbankproject.commons.model.Iban
import model.types.Bic

case class SepaCreditTransferTransaction(
                                          id: UUID,
                                          amount: BigDecimal,
                                          debtorName: Option[String],
                                          debtorAccount: Option[Iban],
                                          debtorAgent: Option[Bic],
                                          creditorName: Option[String],
                                          creditorAccount: Option[Iban],
                                          creditorAgent: Option[Bic],
                                          purposeCode: Option[String],
                                          descripton: Option[String],
                                          sepaMessageId: UUID,
                                          idInSepaFile: String,
                                          instructionId: Option[String],
                                          endToEndId: String
                                        )
