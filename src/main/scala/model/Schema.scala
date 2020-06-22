package model

import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID

import _root_.slick.jdbc.JdbcType
import com.openbankproject.commons.model.Iban

import model.enums.SepaFileStatus.SepaFileStatus
import model.enums.SepaFileType.SepaFileType
import model.enums.SepaMessageType.SepaMessageType
import model.enums._
import model.types.Bic
import shapeless.{Generic, HNil}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._
import slickless._

object Schema {

  val db = Database.forConfig("databaseConfig")

  implicit lazy val ibanColumnType: JdbcType[Iban] with BaseTypedType[Iban] = MappedColumnType.base[Iban, String](_.iban, Iban)
  implicit lazy val bicColumnType: JdbcType[Bic] with BaseTypedType[Bic] = MappedColumnType.base[Bic, String](_.bic, Bic)
  implicit lazy val pathColumnType: JdbcType[Path] with BaseTypedType[Path] = MappedColumnType.base[Path, String](_.toString, Path.of(_:String))

  implicit lazy val sepaFileTypeColumnType = MappedColumnType.base[SepaFileType, String](_.toString, SepaFileType.withName)
  implicit lazy val sepaFileStatusColumnType = MappedColumnType.base[SepaFileStatus, String](_.toString, SepaFileStatus.withName)
  implicit lazy val sepaMessageTypeColumnType = MappedColumnType.base[SepaMessageType, String](_.toString, SepaMessageType.withName)

  class SepaFiles(tag: Tag) extends Table[SepaFile](tag, "sepa_file") {
    def id = column[UUID]("id", O.PrimaryKey)
    def name = column[String]("name")
    def path = column[Path]("path")
    def fileType = column[SepaFileType]("type")
    def status = column[SepaFileStatus]("status")
    def receiptDate = column[Option[LocalDateTime]]("receipt_date")
    def processedDate = column[Option[LocalDateTime]]("processed_date")
    //def * = (id, name, path, fileType, status, receiptDate, processedDate) <> (SepaFile.tupled, SepaFile.unapply)
    def * = (id :: name :: path :: fileType :: status :: receiptDate :: processedDate :: HNil).mappedWith(Generic[SepaFile])

  }
  val sepaFiles = TableQuery[SepaFiles]

  class SepaMessages(tag: Tag) extends Table[SepaMessage](tag, "sepa_message") {
    def id = column[UUID]("id", O.PrimaryKey)
    def creationDateTime = column[LocalDateTime]("creation_date_time")
    def messageType = column[SepaMessageType]("type")
    def content = column[Option[String]]("content")
    def sepaFileId = column[Option[UUID]]("sepa_file_id")
    def idInSepaFile = column[String]("id_in_sepa_file")
    def * = (id, creationDateTime, messageType, content, sepaFileId, idInSepaFile) <> (SepaMessage.tupled, SepaMessage.unapply)

    def sepaFile = foreignKey("sepa_message_sepa_file_id_fkey", sepaFileId, sepaFiles)(_.id)
  }
  val sepaMessages = TableQuery[SepaMessages]

  class SepaCreditTransferTransactions(tag: Tag) extends Table[SepaCreditTransferTransaction](tag, "sepa_credit_transfer_transaction") {
    def id = column[UUID]("id", O.PrimaryKey)
    def amount = column[BigDecimal]("amount")
    def debtorName = column[Option[String]]("debtor_name")
    def debtorAccount = column[Option[Iban]]("debtor_account")
    def debtorAgent = column[Option[Bic]]("debtor_agent")
    def creditorName = column[Option[String]]("creditor_name")
    def creditorAccount = column[Option[Iban]]("creditor_account")
    def creditorAgent = column[Option[Bic]]("creditor_agent")
    def purposeCode = column[Option[String]]("purpose_code")
    def descripton = column[Option[String]]("descripton")
    def sepaMessageId = column[Option[UUID]]("sepa_message_id")
    def idInSepaFile = column[String]("id_in_sepa_file")
    def instructionId = column[Option[String]]("instruction_id")
    def endToEndId = column[String]("end_to_end_id")
    def * = (id :: amount :: debtorName :: debtorAccount :: debtorAgent :: creditorName :: creditorAccount :: creditorAgent :: purposeCode :: descripton :: sepaMessageId :: idInSepaFile :: instructionId :: endToEndId :: HNil).mappedWith(Generic[SepaCreditTransferTransaction])

    def sepaMessage = foreignKey("sepa_credit_transfer_transaction_sepa_message_id_fkey", sepaMessageId, sepaMessages)(_.id)
  }
  val sepaCreditTransferTransactions = TableQuery[SepaCreditTransferTransactions]

}
