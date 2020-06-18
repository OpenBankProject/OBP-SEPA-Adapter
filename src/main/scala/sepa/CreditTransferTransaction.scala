package sepa

import com.openbankproject.commons.model.Iban
import model.types.Bic

case class CreditTransferTransaction(
                                      id: String,
                                      amount: BigDecimal,
                                      debtorName: Option[String],
                                      debtorAccount: Option[Iban],
                                      debtorAgent: Option[Bic],
                                      creditorName: Option[String],
                                      creditorAccount: Option[Iban],
                                      creditorAgent: Option[Bic],
                                      purposeCode: Option[String],
                                      descripton: Option[String]
                                    )
