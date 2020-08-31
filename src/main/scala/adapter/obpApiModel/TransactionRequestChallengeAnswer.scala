package adapter.obpApiModel


/**
 * This define the structure of the body for a POST request on the answerTransactionRequestChallenge endpoint
 */
case class TransactionRequestChallengeAnswer(
                                              id: String,
                                              answer: String,
                                              reason_code: Option[String] = None,
                                              additional_information: Option[String] = None
                                            )
