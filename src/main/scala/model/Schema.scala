package model

import java.nio.file.Path
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import _root_.slick.jdbc.JdbcType
import com.openbankproject.commons.model.{Iban, TransactionId, TransactionRequestId}
import io.circe.{Json, JsonObject, parser}
import model.enums.SepaCreditTransferTransactionStatus.SepaCreditTransferTransactionStatus
import model.enums.SepaFileStatus.SepaFileStatus
import model.enums.SepaFileType.SepaFileType
import model.enums.SepaMessageStatus.SepaMessageStatus
import model.enums.SepaMessageType.SepaMessageType
import model.enums._
import model.jsonClasses.{Party, PaymentTypeInformation, SettlementInformation}
import model.types.Bic
import shapeless.{Generic, HNil}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._
import slickless._

/** Object that contains the mapping between the mode and the database
 *
 */
object Schema {

  val db = Database.forConfig("databaseConfig")

  implicit lazy val ibanColumnType = MappedColumnType.base[Iban, String](_.iban, Iban)
  implicit lazy val bicColumnType = MappedColumnType.base[Bic, String](_.bic, Bic)
  implicit lazy val pathColumnType = MappedColumnType.base[Path, String](_.toString, Path.of(_:String))
  implicit lazy val jsonColumnType = MappedColumnType.base[Json, String](_.toString(), parser.parse(_).toOption.orNull)
  implicit lazy val obpTransactionRequestIdColumnType = MappedColumnType.base[TransactionRequestId, String](_.value, TransactionRequestId(_))
  implicit lazy val obpTransactionIdColumnType = MappedColumnType.base[TransactionId, String](_.value, TransactionId(_))
  implicit lazy val partyColumnType = MappedColumnType.base[Party, String](_.toJson.toString, Party.fromJson(_).get)
  implicit lazy val settlementInformationColumnType = MappedColumnType.base[SettlementInformation, String](_.toJson.toString, SettlementInformation.fromJson(_).get)
  implicit lazy val paymentInformationTypeColumnType = MappedColumnType.base[PaymentTypeInformation, String](_.toJson.toString, PaymentTypeInformation.fromJson(_).get)

  implicit lazy val sepaFileTypeColumnType = MappedColumnType.base[SepaFileType, String](_.toString, SepaFileType.withName)
  implicit lazy val sepaFileStatusColumnType = MappedColumnType.base[SepaFileStatus, String](_.toString, SepaFileStatus.withName)
  implicit lazy val sepaMessageTypeColumnType = MappedColumnType.base[SepaMessageType, String](_.toString, SepaMessageType.withName)
  implicit lazy val sepaMessageStatusColumnType = MappedColumnType.base[SepaMessageStatus, String](_.toString, SepaMessageStatus.withName)
  implicit lazy val sepaCreditTransferTransactionStatusColumnType = MappedColumnType.base[SepaCreditTransferTransactionStatus, String](_.toString, SepaCreditTransferTransactionStatus.withName)

  class SepaFiles(tag: Tag) extends Table[SepaFile](tag, "sepa_file") {
    def id = column[UUID]("id", O.PrimaryKey)
    def name = column[String]("name")
    def path = column[Path]("path")
    def fileType = column[SepaFileType]("type")
    def status = column[SepaFileStatus]("status")
    def receiptDate = column[Option[LocalDateTime]]("receipt_date")
    def processedDate = column[Option[LocalDateTime]]("processed_date")
    def * = (id :: name :: path :: fileType :: status :: receiptDate :: processedDate :: HNil).mappedWith(Generic[SepaFile])

  }
  val sepaFiles = TableQuery[SepaFiles]

  class SepaMessages(tag: Tag) extends Table[SepaMessage](tag, "sepa_message") {
    def id = column[UUID]("id", O.PrimaryKey)
    def creationDateTime = column[LocalDateTime]("creation_date_time")
    def messageType = column[SepaMessageType]("type")
    def status = column[SepaMessageStatus]("status")
    def sepaFileId = column[Option[UUID]]("sepa_file_id")
    def messageIdInSepaFile = column[String]("message_id_in_sepa_file")
    def numberOfTransactions = column[Int]("number_of_transactions")
    def totalAmount = column[BigDecimal]("total_amount")
    def settlementDate = column[Option[LocalDate]]("settlement_date")
    def instigatingAgent = column[Option[Bic]]("instigating_agent")
    def instigatedAgent = column[Option[Bic]]("instigated_agent")
    def customFields = column[Option[Json]]("custom_fields")
    def * = (id :: creationDateTime :: messageType :: status :: sepaFileId :: messageIdInSepaFile :: numberOfTransactions :: totalAmount :: settlementDate :: instigatingAgent :: instigatedAgent :: customFields :: HNil).mappedWith(Generic[SepaMessage])

    def sepaFile = foreignKey("sepa_message_sepa_file_id_fkey", sepaFileId, sepaFiles)(_.id)
  }
  val sepaMessages = TableQuery[SepaMessages]

  class SepaCreditTransferTransactions(tag: Tag) extends Table[SepaCreditTransferTransaction](tag, "sepa_credit_transfer_transaction") {
    def id = column[UUID]("id", O.PrimaryKey)
    def amount = column[BigDecimal]("amount")
    def debtor = column[Option[Party]]("debtor")
    def debtorAccount = column[Option[Iban]]("debtor_account")
    def debtorAgent = column[Option[Bic]]("debtor_agent")
    def ultimateDebtor = column[Option[Party]]("ultimate_debtor")
    def creditor = column[Option[Party]]("creditor")
    def creditorAccount = column[Option[Iban]]("creditor_account")
    def creditorAgent = column[Option[Bic]]("creditor_agent")
    def ultimateCreditor = column[Option[Party]]("ultimate_creditor")
    def purposeCode = column[Option[String]]("purpose_code")
    def description = column[Option[String]]("description")
    def creationDateTime = column[LocalDateTime]("creation_date_time")
    def settlementDate = column[Option[LocalDate]]("settlement_date")
    def transactionIdInSepaFile = column[String]("transaction_id_in_sepa_file")
    def instructionId = column[Option[String]]("instruction_id")
    def endToEndId = column[String]("end_to_end_id")
    def settlementInformation = column[Option[SettlementInformation]]("settlement_information")
    def paymentTypeInformation = column[Option[PaymentTypeInformation]]("payment_type_information")
    def status = column[SepaCreditTransferTransactionStatus]("status")
    def customFields = column[Option[Json]]("custom_fields")
    def * = (id :: amount :: debtor :: debtorAccount :: debtorAgent :: ultimateDebtor :: creditor :: creditorAccount :: creditorAgent :: ultimateCreditor :: purposeCode :: description :: creationDateTime :: settlementDate :: transactionIdInSepaFile :: instructionId :: endToEndId :: settlementInformation :: paymentTypeInformation :: status :: customFields :: HNil).mappedWith(Generic[SepaCreditTransferTransaction])
  }
  val sepaCreditTransferTransactions = TableQuery[SepaCreditTransferTransactions]

  class SepaTransactionMessages(tag: Tag) extends Table[SepaTransactionMessage](tag, "sepa_transaction_message") {
    def sepaCreditTransferTransactionId = column[UUID]("sepa_credit_transfer_transaction_id")
    def sepaMessageId = column[UUID]("sepa_message_id")
    def transactionStatusIdInSepaFile = column[String]("transaction_status_id_in_sepa_file")
    def obpTransactionRequestId = column[Option[TransactionRequestId]]("obp_transaction_request_id")
    def obpTransactionId = column[Option[TransactionId]]("obp_transaction_id")
    def * = (sepaCreditTransferTransactionId :: sepaMessageId :: transactionStatusIdInSepaFile :: obpTransactionRequestId :: obpTransactionId :: HNil).mappedWith(Generic[SepaTransactionMessage])
  }
  val sepaTransactionMessages = TableQuery[SepaTransactionMessages]

}
