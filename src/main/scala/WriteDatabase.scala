import java.nio.file.Path
import java.time.{LocalDate, LocalDateTime, Month}
import java.util.UUID

import model.enums._
import model.types.{Bic, Iban}
import model._
import slick.dbio.DBIOAction
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object WriteDatabase extends App {
  val db = Database.forConfig("databaseConfig")

  val customers = Seq(
    Customer(UUID.randomUUID(), "Callie", "Rivera", "2316 Hickman Street", "60106", "Bensenville", "United States", LocalDate.of(1976, Month.APRIL, 11), "Bensenville", "United States"),
    Customer(UUID.randomUUID(), "Gustave", "Veilleux", "97, rue de la Boétie", "78370", "PLAISIR", "France", LocalDate.of(1998, Month.JANUARY, 3), "Paris", "France"),
    Customer(UUID.randomUUID(), "Benjamin", "Blau", "Buelowstrasse 2", "56769", "Arbach", "Germany", LocalDate.of(1961, Month.MARCH, 26), "Frankfurt", "Germany")
  )

  val accounts = Seq(
    Account(UUID.randomUUID(), Iban("DE28500105175254349914"), customers(0).id, AccountType.CURRENT, AccountStatus.ACTIVE, BigDecimal(500.0), BigDecimal(521.59)),
    Account(UUID.randomUUID(), Iban("DE74500105177926551355"), customers(1).id, AccountType.CURRENT, AccountStatus.ACTIVE, BigDecimal(100.0), BigDecimal(150.0)),
    Account(UUID.randomUUID(), Iban("DE62500105177222842858"), customers(2).id, AccountType.CURRENT, AccountStatus.ACTIVE, BigDecimal(200.0), BigDecimal(200.0)),
    Account(UUID.randomUUID(), Iban("DE49500105173712986459"), customers(2).id, AccountType.SAVING, AccountStatus.ACTIVE, BigDecimal(1000.0), BigDecimal(1000.0))
  )

  val beneficiaries = Seq(
    Beneficiary(UUID.randomUUID(), customers(0).id, Iban("DE74500105177926551355"), Bic("OBPCDEB1XXX"), "Gustave Veilleux", null),
    Beneficiary(UUID.randomUUID(), customers(1).id, Iban("DE62500105177222842858"), Bic("OBPCDEB1XXX"), "Benjamin Blau", null),
    Beneficiary(UUID.randomUUID(), customers(1).id, Iban("DE05500105179384878339"), Bic("NTSBDEB1XXX"), "Phillipp Kuefer", Some("My friend Phillipp")),
    Beneficiary(UUID.randomUUID(), customers(2).id, Iban("DE42500105175212175614"), Bic("DEUTBEBEXXX"), "Lukas Kuhn", Some("Accommodation owner"))
  )

  val sddMandates = Seq(
    SDDMandate(UUID.randomUUID(), "DEMP004581587", customers(1).id, accounts(1).id, "Total Direct Energie", "FR03ZZZ488157", Iban("FR4312739000701887825525P16"), Bic("BNPAFRPPXXX"), SDDMandateType.CORE, recurrent = true, SDDMandateStatus.ACTIVE, LocalDate.of(2020, Month.FEBRUARY, 17)),
    SDDMandate(UUID.randomUUID(), "ABCD087488498", customers(2).id, accounts(2).id, "Deutsche Telekom", "DE03ABC484821", Iban("DE89500105179599249252"), Bic("DEUTBEBEXXX"), SDDMandateType.CORE, recurrent = true, SDDMandateStatus.ACTIVE, LocalDate.of(2019, Month.DECEMBER, 28)),
  )

  val cards = Seq(
    Card(UUID.randomUUID(), customers(0).id, accounts(0).id, "5393976247256782", "607", "4589", LocalDate.of(2020, Month.MAY, 20), Some(LocalDate.of(2020, Month.MAY, 25)), None, LocalDate.of(2024, Month.MAY, 1), CardPaymentNetwork.MASTERCARD, CardType.DEBIT, CardRange.STANDARD, CardStatus.ACTIVE, CardPaymentStatus.ACTIVE, CardPaymentStatus.DEACTIVATED, CardPaymentStatus.ACTIVE, CardPaymentStatus.ACTIVE, BigDecimal(0), BigDecimal(50), BigDecimal(0), BigDecimal(1000), BigDecimal(0), BigDecimal(500)),
    Card(UUID.randomUUID(), customers(1).id, accounts(1).id, "5235161738913694", "301", "1547", LocalDate.of(2019, Month.OCTOBER, 10), Some(LocalDate.of(2019, Month.OCTOBER, 22)), None, LocalDate.of(2028, Month.SEPTEMBER, 1), CardPaymentNetwork.MASTERCARD, CardType.CREDIT, CardRange.GOLD, CardStatus.ACTIVE, CardPaymentStatus.ACTIVE, CardPaymentStatus.ACTIVE, CardPaymentStatus.DEACTIVATED, CardPaymentStatus.ACTIVE, BigDecimal(0), BigDecimal(50), BigDecimal(0), BigDecimal(5000), BigDecimal(0), BigDecimal(2000)),
    Card(UUID.randomUUID(), customers(2).id, accounts(2).id, "5430898082797073", "192", "8569", LocalDate.of(2019, Month.DECEMBER, 5), Some(LocalDate.of(2019, Month.DECEMBER, 30)), Some(LocalDate.of(2020, Month.MARCH, 14)), LocalDate.of(2023, Month.NOVEMBER, 1), CardPaymentNetwork.MASTERCARD, CardType.DEBIT, CardRange.PLATINIUM, CardStatus.OPPOSED, CardPaymentStatus.ACTIVE, CardPaymentStatus.DEACTIVATED, CardPaymentStatus.DEACTIVATED, CardPaymentStatus.ACTIVE, BigDecimal(0), BigDecimal(50), BigDecimal(0), BigDecimal(10000), BigDecimal(0), BigDecimal(4000))
  )

  val files = Seq(
    CoreBankingFile(UUID.randomUUID(), "SCT_OUT_2020-05-21_001", Path.of("files/archives/sepa/sct/out"), CoreBankingFileType.SCT_OUT_FILE, CoreBankingFileStatus.PROCESSED, None, Some(LocalDateTime.of(2020, Month.MAY, 21, 8, 0, 0)), Some(4582)),
    CoreBankingFile(UUID.randomUUID(), "CARD_CLEARING_2020-05-06", Path.of("files/archives/card/clearing"), CoreBankingFileType.CARD_CLEARING_FILE, CoreBankingFileStatus.PROCESSED, Some(LocalDateTime.of(2020, Month.MAY, 6, 2, 5, 51)), Some(LocalDateTime.of(2020, Month.MAY, 6, 2, 35, 24)), Some(21326)),
  )

  val cardTransactions = Seq(
    CardTransaction(UUID.randomUUID(), accounts(0).id, TransactionType.CARD_INTERNET_PAYMENT, TransactionStatus.NOT_CLEARED, BigDecimal(21.59), LocalDateTime.now(), accounts(0).balance + 21.59, accounts(0).balance, cards(0).id, "Amazon.com, Inc", None, None),
    CardTransaction(UUID.randomUUID(), accounts(1).id, TransactionType.CARD_CHIP_PAYMENT, TransactionStatus.NOT_CLEARED, BigDecimal(50), LocalDateTime.of(2020, Month.MARCH, 12, 17, 35, 21), accounts(1).balance + 50, accounts(1).balance, cards(1).id, "Restaurant LE MARTRAY", None, None),
    CardTransaction(UUID.randomUUID(), accounts(2).id, TransactionType.CARD_NFC_PAYMENT, TransactionStatus.CLEARED, BigDecimal(20), LocalDateTime.of(2020, Month.MAY, 5, 8, 56, 48), accounts(2).balance + 20, accounts(1).balance, cards(2).id, "Super Shoes Store", Some(LocalDate.of(2020, Month.MAY, 6)), Some(files(1).id)),
  )

  val sepaTransactions = Seq(
    SepaTransaction(UUID.randomUUID(), accounts(0).id, TransactionType.SCT_OUT_TRANSFER, TransactionStatus.PROCESSED, BigDecimal(100), LocalDateTime.of(2020, Month.MAY, 21, 7, 22, 19), accounts(0).balance + 121.59, accounts(0).balance + 21.59, Iban("DE74500105177926551355"), Bic("OBPCDEB1XXX"), "Gustave Veilleux", Some("A gift for your birthday"), Some(LocalDateTime.of(2020, Month.MAY, 21, 8, 0, 0)), Some(files(0).id), Some("SCT_OUT_2020-05-21_001_0000245_0001"))
  )

  val setup = DBIOAction.seq(
    Schema.customers ++= customers,
    Schema.accounts ++= accounts,
    Schema.beneficiaries ++= beneficiaries,
    Schema.sddMandates ++= sddMandates,
    Schema.cards ++= cards,
    Schema.coreBankingFiles ++= files,
    Schema.cardTransactions ++= cardTransactions,
    Schema.sepaTransactions ++= sepaTransactions
  )

  Await.result(db.run(setup), Duration.Inf)
}

