package adapter.obpApiModel

import java.util.Date

import com.openbankproject.commons.model.TransactionRequestBody

case class ChallengeAnswerJson400(
                                   id: String,
                                   answer: String,
                                   reason_code: Option[String] = None,
                                   additional_information: Option[String] = None
                                 )

case class TransactionRequestWithChargeJson(
                                             id: String,
                                             `type`: String,
                                             from: TransactionRequestAccountJsonV140,
                                             details: TransactionRequestBody,
                                             transaction_ids: String,
                                             status: String,
                                             start_date: Date,
                                             end_date: Date,
                                             challenge: ChallengeJsonV140,
                                             charge: TransactionRequestChargeJsonV200
                                           )
