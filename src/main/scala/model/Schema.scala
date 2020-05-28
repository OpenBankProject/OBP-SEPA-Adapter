package model

import java.nio.file.Path

import model.enums.AccountStatus.AccountStatus
import model.enums.{AccountStatus, AccountType, CardPaymentNetwork, CardPaymentStatus, CardRange, CardStatus, CardType, CoreBankingFileStatus, CoreBankingFileType, SDDMandateStatus, SDDMandateType, TransactionStatus, TransactionType}
import model.enums.AccountType.AccountType
import model.enums.CardPaymentNetwork.CardPaymentNetwork
import model.enums.CardPaymentStatus.CardPaymentStatus
import model.enums.CardRange.CardRange
import model.enums.CardStatus.CardStatus
import model.enums.CardType.CardType
import model.enums.CoreBankingFileStatus.CoreBankingFileStatus
import model.enums.CoreBankingFileType.CoreBankingFileType
import model.enums.SDDMandateStatus.SDDMandateStatus
import model.enums.SDDMandateType.SDDMandateType
import model.enums.TransactionStatus.TransactionStatus
import model.enums.TransactionType.TransactionType
import model.types.{Bic, Iban}
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import _root_.slick.jdbc.JdbcType
import slick.ast.BaseTypedType
import slick.jdbc.PostgresProfile.api._
import shapeless.{Generic, HNil}
import slickless._

object Schema {

  implicit lazy val ibanColumnType: JdbcType[Iban] with BaseTypedType[Iban] = MappedColumnType.base[Iban, String](_.iban, Iban)
  implicit lazy val bicColumnType: JdbcType[Bic] with BaseTypedType[Bic] = MappedColumnType.base[Bic, String](_.bic, Bic)
  implicit lazy val pathColumnType: JdbcType[Path] with BaseTypedType[Path] = MappedColumnType.base[Path, String](_.toString, Path.of(_:String))

  implicit lazy val accountStatusColumnType = MappedColumnType.base[AccountStatus, String](_.toString, AccountStatus.withName)
  implicit lazy val accountTypeColumnType = MappedColumnType.base[AccountType, String](_.toString, AccountType.withName)
  implicit lazy val cardPaymentNetworkColumnType = MappedColumnType.base[CardPaymentNetwork, String](_.toString, CardPaymentNetwork.withName)
  implicit lazy val cardPaymentStatusColumnType = MappedColumnType.base[CardPaymentStatus, String](_.toString, CardPaymentStatus.withName)
  implicit lazy val cardRangeColumnType = MappedColumnType.base[CardRange, String](_.toString, CardRange.withName)
  implicit lazy val cardStatusColumnType = MappedColumnType.base[CardStatus, String](_.toString, CardStatus.withName)
  implicit lazy val cardTypeColumnType = MappedColumnType.base[CardType, String](_.toString, CardType.withName)
  implicit lazy val coreBankingFileStatusColumnType = MappedColumnType.base[CoreBankingFileStatus, String](_.toString, CoreBankingFileStatus.withName)
  implicit lazy val coreBankingFileTypeColumnType = MappedColumnType.base[CoreBankingFileType, String](_.toString, CoreBankingFileType.withName)
  implicit lazy val sddMandateStatusColumnType = MappedColumnType.base[SDDMandateStatus, String](_.toString, SDDMandateStatus.withName)
  implicit lazy val sddMandateTypeColumnType = MappedColumnType.base[SDDMandateType, String](_.toString, SDDMandateType.withName)
  implicit lazy val transactionStatusColumnType = MappedColumnType.base[TransactionStatus, String](_.toString, TransactionStatus.withName)
  implicit lazy val transactionTypeColumnType = MappedColumnType.base[TransactionType, String](_.toString, TransactionType.withName)

  class Customers(tag: Tag) extends Table[Customer](tag, "customer") {
    def id = column[UUID]("id", O.PrimaryKey)
    def firstName = column[String]("first_name")
    def lastName = column[String]("last_name")
    def address = column[String]("address")
    def zipCode = column[String]("zip_code")
    def city = column[String]("city")
    def country = column[String]("country")
    def birthDate = column[LocalDate]("birth_date")
    def birthCity = column[String]("birth_city")
    def birthCountry = column[String]("birth_country")
    def * = (id, firstName, lastName, address, zipCode, city, country, birthDate, birthCity, birthCountry) <> (Customer.tupled, Customer.unapply)
  }
  val customers = TableQuery[Customers]

  class Beneficiaries(tag: Tag) extends Table[Beneficiary](tag, "beneficiary") {
    def id = column[UUID]("id", O.PrimaryKey)
    def customerId = column[UUID]("customer_id")
    def iban = column[Iban]("iban")
    def bic = column[Bic]("bic")
    def name = column[String]("name")
    def label = column[Option[String]]("label")
    def * = (id, customerId, iban, bic, name, label) <> (Beneficiary.tupled, Beneficiary.unapply)

    def customer = foreignKey("beneficiary_customer_id_fkey", customerId, customers)(_.id)
  }
  val beneficiaries = TableQuery[Beneficiaries]

  class Accounts(tag: Tag) extends Table[Account](tag, "account") {
    def id = column[UUID]("id", O.PrimaryKey)
    def iban = column[Iban]("iban")
    def customerId = column[UUID]("customer_id")
    def accountType = column[AccountType]("type")
    def status = column[AccountStatus]("status")
    def balance = column[BigDecimal]("balance")
    def accountingBalance = column[BigDecimal]("accounting_balance")
    def * = (id, iban, customerId, accountType, status, balance, accountingBalance) <> (Account.tupled, Account.unapply)

    def customer = foreignKey("account_customer_id_fkey", customerId, customers)(_.id)
  }
  val accounts = TableQuery[Accounts]

  class SDDMandates(tag: Tag) extends Table[SDDMandate](tag, "sdd_mandate") {
    def id = column[UUID]("id", O.PrimaryKey)
    def reference = column[String]("reference")
    def customerId = column[UUID]("customer_id")
    def customerAccountId = column[UUID]("customer_account_id")
    def billerName = column[String]("biller_name")
    def billerReference = column[String]("biller_reference")
    def billerIban = column[Iban]("biller_iban")
    def billerBic = column[Bic]("biller_bic")
    def mandateType = column[SDDMandateType]("type")
    def recurrent = column[Boolean]("recurrent")
    def status = column[SDDMandateStatus]("status")
    def implementationDate = column[LocalDate]("implementation_date")
    def * = (id, reference, customerId, customerAccountId, billerName, billerReference, billerIban, billerBic, mandateType, recurrent, status, implementationDate) <> (SDDMandate.tupled, SDDMandate.unapply)

    def customer = foreignKey("sdd_mandate_customer_id_fkey", customerId, customers)(_.id)
    def account = foreignKey("sdd_mandate_customer_account_id_fkey", customerAccountId, accounts)(_.id)
  }
  val sddMandates = TableQuery[SDDMandates]

  class Cards(tag: Tag) extends Table[Card](tag, "card") {
    def id = column[UUID]("id", O.PrimaryKey)
    def customerId = column[UUID]("customer_id")
    def accountId = column[UUID]("account_id")
    def pan = column[String]("pan")
    def cvv = column[String]("cvv")
    def pin = column[String]("pin")
    def creationDate = column[LocalDate]("creation_date")
    def activationDate = column[Option[LocalDate]]("activation_date")
    def oppositionDate = column[Option[LocalDate]]("opposition_date")
    def expirationDate = column[LocalDate]("expiration_date")
    def paymentNetwork = column[CardPaymentNetwork]("payment_network")
    def cardType = column[CardType]("card_type")
    def range = column[CardRange]("range")
    def status = column[CardStatus]("status")
    def withdrawalStatus = column[CardPaymentStatus]("withdrawal_status")
    def internetPaymentStatus = column[CardPaymentStatus]("internet_payment_status")
    def abroadPaymentStatus = column[CardPaymentStatus]("abroad_payment_status")
    def nfcPaymentStatus = column[CardPaymentStatus]("nfc_payment_status")
    def nfcPaidAmount = column[BigDecimal]("nfc_paid_amount")
    def nfcPaymentLimit = column[BigDecimal]("nfc_payment_limit")
    def paidAmount = column[BigDecimal]("paid_amount")
    def paymentLimit = column[BigDecimal]("payment_limit")
    def withdrawnAmount = column[BigDecimal]("withdrawn_amount")
    def withdrawalLimit = column[BigDecimal]("withdrawal_limit")
    def * = (id :: customerId :: accountId :: pan :: cvv :: pin :: creationDate :: activationDate :: oppositionDate :: expirationDate :: paymentNetwork :: cardType :: range :: status :: withdrawalStatus :: internetPaymentStatus :: abroadPaymentStatus :: nfcPaymentStatus :: nfcPaidAmount :: nfcPaymentLimit :: paidAmount :: paymentLimit :: withdrawnAmount :: withdrawalLimit :: HNil).mappedWith(Generic[Card])

    def customer = foreignKey("card_customer_id_fkey", customerId, customers)(_.id)
    def account = foreignKey("card_account_id_fkey", accountId, accounts)(_.id)
  }
  val cards = TableQuery[Cards]

  class CoreBankingFiles(tag: Tag) extends Table[CoreBankingFile](tag, "core_banking_file") {
    def id = column[UUID]("id")
    def name = column[String]("name")
    def path = column[Path]("path")
    def fileType = column[CoreBankingFileType]("type")
    def status = column[CoreBankingFileStatus]("status")
    def receiptDate = column[Option[LocalDateTime]]("receipt_date")
    def processedDate = column[Option[LocalDateTime]]("processed_date")
    def numberOfTransactions = column[Option[Int]]("number_of_transactions")
    def * = (id, name, path, fileType, status, receiptDate, processedDate, numberOfTransactions) <> (CoreBankingFile.tupled, CoreBankingFile.unapply)
  }
  val coreBankingFiles = TableQuery[CoreBankingFiles]

  class Transactions(tag: Tag) extends Table[Transaction](tag, "transaction") {
    def id = column[UUID]("id", O.PrimaryKey)
    def accountId = column[UUID]("account_id")
    def transactionType = column[TransactionType]("type")
    def status = column[TransactionStatus]("status")
    def amount = column[BigDecimal]("amount")
    def date = column[LocalDateTime]("date")
    def oldBalance = column[BigDecimal]("old_balance")
    def newBalance = column[BigDecimal]("new_balance")
    def * = (id, accountId, transactionType, status, amount, date, oldBalance, newBalance) <> (Transaction.tupled, Transaction.unapply)

    def account = foreignKey("transaction_account_id_fkey", accountId, accounts)(_.id)
  }
  val transactions = TableQuery[Transactions]

  class SepaTransactions(tag: Tag) extends Table[SepaTransaction](tag, "sepa_transaction") {
    def id = column[UUID]("id", O.PrimaryKey)
    def accountId = column[UUID]("account_id")
    def transactionType = column[TransactionType]("type")
    def status = column[TransactionStatus]("status")
    def amount = column[BigDecimal]("amount")
    def date = column[LocalDateTime]("date")
    def oldBalance = column[BigDecimal]("old_balance")
    def newBalance = column[BigDecimal]("new_balance")
    def counterpartIban = column[Iban]("counterpart_iban")
    def counterpartBic = column[Bic]("counterpart_bic")
    def counterpartName = column[String]("counterpart_name")
    def description = column[Option[String]]("description")
    def processedDate = column[Option[LocalDateTime]]("processed_date")
    def fileId = column[Option[UUID]]("file_id")
    def fileTransactionId = column[Option[String]]("file_transaction_id")
    def * = (id, accountId, transactionType, status, amount, date, oldBalance, newBalance, counterpartIban, counterpartBic, counterpartName, description, processedDate, fileId, fileTransactionId) <> (SepaTransaction.tupled, SepaTransaction.unapply)

    def account = foreignKey("sepa_transaction_account_id_fkey", accountId, accounts)(_.id)
    def file = foreignKey("sepa_transaction_file_id_fkey", fileId, coreBankingFiles)(_.id)
  }
  val sepaTransactions = TableQuery[SepaTransactions]

  class CardTransactions(tag: Tag) extends Table[CardTransaction](tag, "card_transaction") {
    def id = column[UUID]("id", O.PrimaryKey)
    def accountId = column[UUID]("account_id")
    def transactionType = column[TransactionType]("type")
    def status = column[TransactionStatus]("status")
    def amount = column[BigDecimal]("amount")
    def date = column[LocalDateTime]("date")
    def oldBalance = column[BigDecimal]("old_balance")
    def newBalance = column[BigDecimal]("new_balance")
    def cardId = column[UUID]("card_id")
    def merchantName = column[String]("merchant_name")
    def clearingDate = column[Option[LocalDate]]("clearing_date")
    def clearingFileId = column[Option[UUID]]("clearing_file_id")
    def * = (id, accountId, transactionType, status, amount, date, oldBalance, newBalance, cardId, merchantName, clearingDate, clearingFileId) <> (CardTransaction.tupled, CardTransaction.unapply)

    def account = foreignKey("card_transaction_account_id_fkey", accountId, accounts)(_.id)
    def card = foreignKey("card_transaction_card_id_fkey", accountId, accounts)(_.id)
    def clearingFile = foreignKey("card_transaction_file_id_fkey", clearingFileId, coreBankingFiles)(_.id)
  }
  val cardTransactions = TableQuery[CardTransactions]

}
