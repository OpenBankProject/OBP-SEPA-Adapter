package model

import java.util.UUID

case class SepaTransactionMessage(
                                   sepaCreditTransferTransactionId: UUID,
                                   sepaMessageId: UUID,
                                   transactionStatusIdInSepaFile: String,
                                   obpTransactionRequestId: Option[UUID],
                                   obpTransactionId: Option[UUID]
                                 )


