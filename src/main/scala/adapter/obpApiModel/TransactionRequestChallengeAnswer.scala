package adapter.obpApiModel

case class TransactionRequestChallengeAnswer(
                                              id: String,
                                              answer: String,
                                              reason_code: Option[String] = None,
                                              additional_information: Option[String] = None
                                            )
