import java.time.LocalDateTime
import java.util.UUID

import adapter.Adapter
import adapter.obpApiModel.{AccountRoutingJsonV121, ObpApi}
import com.openbankproject.commons.model._
import model.enums.{SepaCreditTransferTransactionStatus, SepaMessageType}
import model.types.Bic
import model.{SepaCreditTransferTransaction, SepaMessage, SepaTransactionMessage}
import org.scalatest.featurespec.AsyncFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{GivenWhenThen, OptionValues}
import sepa.SepaUtil
import sepa.scheduler.ProcessIncomingFilesActorSystem

import scala.reflect.io.{Directory, File, Path}


class CreditTransferTransactionSpec extends AsyncFeatureSpec with GivenWhenThen with Matchers with OptionValues with AkkaActorSystemTest {

  Feature("Send a credit transfer transaction from an OBP account to an external account") {
    Scenario("Call the OBP-API createTransactionRequest (SEPA) endpoint") {
      Given("an OBP account and transaction content")
      val bankId: BankId = BankId("THE_DEFAULT_BANK_ID")
      val accountId: AccountId = AccountId("1023794c-a412-11ea-bb37-0242ac130002")
      val amount = BigDecimal(100)
      val counterpartyIban: Iban = Iban("DE89370400440532013002")
      val description = "Sepa transaction description"

      When("we send the request")
      val transactionRequestResult = ObpApi.createSepaTransactionRequest(bankId, accountId, amount, counterpartyIban, description)

      Then("we should get a successful response")
      transactionRequestResult.map(transactionRequest => {
        transactionRequest.`type` should be("SEPA")
        transactionRequest.status should be("COMPLETED")
        transactionRequest.challenge should be(None)
        transactionRequest.details.description should be(description)
        transactionRequest.details.value.amount should be(amount.toString())
        transactionRequest.details.value.currency should be("EUR")
        transactionRequest.from.bank_id should be(bankId.value)
        transactionRequest.from.account_id should be(accountId.value)
      }
      )

      And("the transaction should exist on the OBP-API")
      for {
        transactionRequest <- transactionRequestResult
        transaction <- ObpApi.getTransactionById(bankId, accountId, TransactionId(transactionRequest.transaction_ids.head))
      } yield {
        transaction.other_account.account_routings should contain(AccountRoutingJsonV121("IBAN", counterpartyIban.iban))
        transaction.details.`type` should be("SEPA")
        transaction.details.description should be(description)
        BigDecimal(transaction.details.value.amount) should be(amount)
      }

      And("the transaction should be saved in the SEPA Adapter")
      for {
        transactionRequest <- transactionRequestResult
        creditTransferTransaction <- SepaCreditTransferTransaction.getByObpTransactionId(TransactionId(transactionRequest.transaction_ids.head))
      } yield {
        creditTransferTransaction.status should be(SepaCreditTransferTransactionStatus.UNPROCESSED)
        creditTransferTransaction.description.value should be(description)
        creditTransferTransaction.amount should be(amount)
        creditTransferTransaction.creditorAccount.value should be(counterpartyIban)
      }
    }
  }

  Feature("Receive a SEPA transaction from an external account to an OBP account") {
    Scenario("We receive a transaction to an existing OBP account") {
      Given("An XML file containing the received transaction")
      val receivedMessageId = SepaUtil.removeDashesToUUID(UUID.randomUUID())
      val transactionAmount = BigDecimal(25)
      val currentDateTime = LocalDateTime.now()
      val settlementDateTime = currentDateTime.toLocalDate.plusDays(1)
      val receivedSepaTransactionId = SepaUtil.removeDashesToUUID(UUID.randomUUID())
      val debtorName = "Louis Dupont"
      val debtorIban = Iban("DE89370400440532013002")
      val debtorBic = Bic("NTSBDEB1XXX")
      val creditorName = "Guillaume"
      val creditorIban = Iban("DE65500105176582659391")
      val creditorBic = Bic("OBPBDEB1XXX")
      val transactionDescription = "A transaction message for example"

      val creditTransferXml =
        s"""<?xml version='1.0' encoding='UTF-8'?>
           |<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02">
           |    <FIToFICstmrCdtTrf>
           |        <GrpHdr>
           |            <MsgId>$receivedMessageId</MsgId>
           |            <CreDtTm>$currentDateTime</CreDtTm>
           |            <NbOfTxs>1</NbOfTxs>
           |            <TtlIntrBkSttlmAmt Ccy="EUR">$transactionAmount</TtlIntrBkSttlmAmt>
           |            <IntrBkSttlmDt>$settlementDateTime</IntrBkSttlmDt>
           |            <SttlmInf>
           |                <SttlmMtd>CLRG</SttlmMtd>
           |            </SttlmInf>
           |            <PmtTpInf>
           |                <SvcLvl>
           |                    <Cd>SEPA</Cd>
           |                </SvcLvl>
           |            </PmtTpInf>
           |        </GrpHdr>
           |        <CdtTrfTxInf>
           |            <PmtId>
           |                <EndToEndId>$receivedSepaTransactionId</EndToEndId>
           |                <TxId>$receivedSepaTransactionId</TxId>
           |            </PmtId>
           |            <IntrBkSttlmAmt Ccy="EUR">$transactionAmount</IntrBkSttlmAmt>
           |            <ChrgBr>SLEV</ChrgBr>
           |            <Dbtr>
           |                <Nm>$debtorName</Nm>
           |            </Dbtr>
           |            <DbtrAcct>
           |                <Id>
           |                    <IBAN>${debtorIban.iban}</IBAN>
           |                </Id>
           |            </DbtrAcct>
           |            <DbtrAgt>
           |                <FinInstnId>
           |                    <BIC>${debtorBic.bic}</BIC>
           |                </FinInstnId>
           |            </DbtrAgt>
           |            <CdtrAgt>
           |                <FinInstnId>
           |                    <BIC>${creditorBic.bic}</BIC>
           |                </FinInstnId>
           |            </CdtrAgt>
           |            <Cdtr>
           |                <Nm>$creditorName</Nm>
           |            </Cdtr>
           |            <CdtrAcct>
           |                <Id>
           |                    <IBAN>${creditorIban.iban}</IBAN>
           |                </Id>
           |            </CdtrAcct>
           |            <RmtInf>
           |                <Ustrd>$transactionDescription</Ustrd>
           |            </RmtInf>
           |        </CdtTrfTxInf>
           |    </FIToFICstmrCdtTrf>
           |</Document>
           |""".stripMargin

      Directory("src/test/tempFiles").createDirectory()
      val filepath = Path(s"src/test/tempFiles/$receivedMessageId.xml")
      val creditTransferTestFile = File(filepath).createFile()
      creditTransferTestFile.writeAll(creditTransferXml)

      When("we integrate the SEPA file in the SEPA Adapter")
      ProcessIncomingFilesActorSystem.main(Array(filepath.path))
      Thread.sleep(5000)

      Then("the transaction should be integrated in the SEPA Adapter")
      val creditTransferTransaction = SepaCreditTransferTransaction.getByTransactionIdInSepaFile(receivedSepaTransactionId)
      creditTransferTransaction.map(creditTransferTransaction => {
        creditTransferTransaction.creditor.value.name.value should be(creditorName)
        creditTransferTransaction.creditorAccount.value should be(creditorIban)
        creditTransferTransaction.creditorAgent.value should be(creditorBic)
        creditTransferTransaction.debtor.value.name.value should be(debtorName)
        creditTransferTransaction.debtorAccount.value should be(debtorIban)
        creditTransferTransaction.debtorAgent.value should be(debtorBic)
        creditTransferTransaction.amount should be(transactionAmount)
        creditTransferTransaction.description.value should be(transactionDescription)
        creditTransferTransaction.status should be(SepaCreditTransferTransactionStatus.TRANSFERED)
      }
      )

      Then("the transaction should be integrated in the OBP-API")
      for {
        sepaAdapterTransaction <- creditTransferTransaction
        sepaAdapterMessage <- SepaMessage.getBySepaCreditTransferTransactionId(sepaAdapterTransaction.id)
          .map(_.find(_.messageType == SepaMessageType.B2B_CREDIT_TRANSFER).value)
        sepaMessageTransaction <- SepaTransactionMessage.getBySepaCreditTransferTransactionIdAndSepaMessageId(sepaAdapterTransaction.id, sepaAdapterMessage.id)
        obpAccount <- ObpApi.getAccountByIban(Some(Adapter.BANK_ID), sepaAdapterTransaction.creditorAccount.value)
        bankId = BankId(obpAccount.bank_id)
        accountId = AccountId(obpAccount.id)
        obpApiTransaction <- ObpApi.getTransactionById(bankId, accountId, sepaMessageTransaction.obpTransactionId.value)
      } yield {
        obpApiTransaction.this_account.account_routings should contain(AccountRoutingJsonV121("IBAN", creditorIban.iban))
        obpApiTransaction.other_account.account_routings should contain(AccountRoutingJsonV121("IBAN", debtorIban.iban))
        obpApiTransaction.details.`type` should be("SEPA")
        obpApiTransaction.details.description should be(transactionDescription)
        BigDecimal(obpApiTransaction.details.value.amount) should be(transactionAmount)
      }

    }
  }
}
