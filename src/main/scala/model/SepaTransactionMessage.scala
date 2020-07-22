package model

import java.util.UUID

import com.openbankproject.commons.model.{TransactionId, TransactionRequestId}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SepaTransactionMessage(
                                   sepaCreditTransferTransactionId: UUID,
                                   sepaMessageId: UUID,
                                   transactionStatusIdInSepaFile: String,
                                   obpTransactionRequestId: Option[TransactionRequestId],
                                   obpTransactionId: Option[TransactionId]
                                 )

object SepaTransactionMessage {
  def getBySepaCreditTransferTransactionIdAndSepaMessageId(sepaCreditTransferTransactionId: UUID, sepaMessageId: UUID): Future[SepaTransactionMessage] =
    Schema.db.run(
      Schema.sepaTransactionMessages
        .filter(transactionMessage => transactionMessage.sepaCreditTransferTransactionId === sepaCreditTransferTransactionId
          && transactionMessage.sepaMessageId === sepaMessageId)
        .result.headOption
    ).flatMap {
      case Some(sepaTransactionMessage) => Future.successful(sepaTransactionMessage)
      case None => Future.failed(new Exception(s"None SepaTransactionMessage found with the sepaCreditTransferTransactionId $sepaCreditTransferTransactionId and sepaMessageId $sepaMessageId"))
    }
}


