package model

import java.util.UUID

import com.openbankproject.commons.model.{TransactionId, TransactionRequestId}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

case class SepaTransactionMessage(
                                   sepaCreditTransferTransactionId: UUID,
                                   sepaMessageId: UUID,
                                   transactionStatusIdInSepaFile: String,
                                   obpTransactionRequestId: Option[TransactionRequestId],
                                   obpTransactionId: Option[TransactionId]
                                 )

object SepaTransactionMessage {
  def getBySepaCreditTransferTransactionIdAndSepaMessageId(sepaCreditTransferTransactionId: UUID, sepaMessageId: UUID): Future[Option[SepaTransactionMessage]] =
    Schema.db.run(
      Schema.sepaTransactionMessages
        .filter(transactionMessage => transactionMessage.sepaCreditTransferTransactionId === sepaCreditTransferTransactionId
          && transactionMessage.sepaMessageId === sepaMessageId)
        .result.headOption
    )
}


