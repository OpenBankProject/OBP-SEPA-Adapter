package sepa

import java.time.LocalDateTime
import java.util.UUID

import io.circe.{Json, JsonObject}
import model.enums.sepaReasonCodes.PaymentReturnMessageReasonCode.PaymentReturnMessageReasonCode
import model.enums.{SepaCreditTransferTransactionStatus, SepaMessageStatus, SepaMessageType}
import model.{SepaCreditTransferTransaction, SepaMessage}

import scala.concurrent.ExecutionContext.Implicits.global

case class PaymentReturnMessage(
                                 message: SepaMessage,
                                 creditTransferTransactions: Seq[SepaCreditTransferTransaction]
                               ) {
  def toXML = {

  }
}

object PaymentReturnMessage {
  def returnTransaction(transactionToReturn: SepaCreditTransferTransaction, originalsepaMessage: SepaMessage, reasonCode: PaymentReturnMessageReasonCode) = {
    for {
      returnSepaMessage <- SepaMessage.getUnprocessedByType(SepaMessageType.B2B_PAYMENT_RETURN).map(_.headOption.getOrElse {
        val sepaMessageId = UUID.randomUUID()
        val message = SepaMessage(
          sepaMessageId, LocalDateTime.now(), SepaMessageType.B2B_PAYMENT_RETURN,
          SepaMessageStatus.UNPROCESSED, sepaFileId = None, SepaUtil.removeDashesToUUID(sepaMessageId),
          numberOfTransactions = 0, totalAmount = 0, None, None, None,
          Some(Json.fromJsonObject(JsonObject.empty
            .add("OriginalMessageId", Json.fromString(originalsepaMessage.messageIdInSepaFile))
            .add("OriginalMessageNameId", Json.fromString(originalsepaMessage.messageType.toString))))
        )
        message.insert()
        message
      })
      _ <- transactionToReturn.copy(status = SepaCreditTransferTransactionStatus.TO_RETURN).update()
      _ <- transactionToReturn.linkMessage(returnSepaMessage.id, SepaUtil.removeDashesToUUID(UUID.randomUUID()), None, None)
    } yield ()
  }
}



