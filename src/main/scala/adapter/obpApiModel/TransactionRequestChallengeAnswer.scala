package adapter.obpApiModel

import java.util.Date

import com.openbankproject.commons.model.TransactionRequestBodyAllTypes

import scala.collection.immutable.List

case class ChallengeAnswerJson400(
                                   id: String,
                                   answer: String,
                                   reason_code: Option[String] = None,
                                   additional_information: Option[String] = None
                                 )

case class TransactionRequestWithChargeJSON210(
                                                id: String,
                                                `type`: String,
                                                from: TransactionRequestAccountJsonV140,
                                                details: TransactionRequestBodyAllTypes,
                                                transaction_ids: List[String],
                                                status: String,
                                                start_date: Date,
                                                end_date: Date,
                                                challenge: ChallengeJsonV140,
                                                charge: TransactionRequestChargeJsonV200
                                              )
