package sepa

import java.util.UUID

import sepa.sct.generated.creditTransfer._
import sepa.sct.generated.creditTransfer.defaultScope
import javax.xml.datatype.DatatypeFactory
import scalaxb.DataRecord

import scala.xml.XML

object TestWrite extends App {

  val IntrBkSttlmDt = DatatypeFactory.newInstance().newXMLGregorianCalendar
  IntrBkSttlmDt.setYear(2020)
  IntrBkSttlmDt.setMonth(5)
  IntrBkSttlmDt.setDay(22)

  val document = Document(
    FIToFICustomerCreditTransferV02(
      GrpHdr = GroupHeader33(
        MsgId = UUID.randomUUID().toString.replace("-", ""),
        CreDtTm = DatatypeFactory.newInstance().newXMLGregorianCalendar(2020, 5, 21, 12, 7, 34, 0, 2),
        NbOfTxs = 3.toString,
        TtlIntrBkSttlmAmt = Some(ActiveCurrencyAndAmount(value = 25.3, Map(("@Ccy", DataRecord("EUR"))))),
        IntrBkSttlmDt = Some(IntrBkSttlmDt),
        SttlmInf = SettlementInformation13(SttlmMtd = CLRG),
        PmtTpInf = Some(PaymentTypeInformation21(SvcLvl = Some(ServiceLevel8Choice(DataRecord(<Cd></Cd>, "SEPA"))))),
        InstgAgt = Some(BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some("DEUTDEBB")))),
        InstdAgt = Some(BranchAndFinancialInstitutionIdentification4(FinancialInstitutionIdentification7(Some("DEUTDEBB")))),
      ),
      CdtTrfTxInf = Seq(
        CreditTransferTransactionInformation11(
          PmtId = PaymentIdentification3(
            EndToEndId = "ID HERE",
            TxId = "ID HERE"
          ),
          IntrBkSttlmAmt = ActiveCurrencyAndAmount(value = 25.3, Map(("@Ccy", DataRecord("EUR")))),
          ChrgBr = SLEV,
          Dbtr = PartyIdentification32(
            Nm = Some("Name of the debtor"),
          ),
          DbtrAcct = Some(CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, "DE08500105171159766486")))),
          DbtrAgt = BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = Some("DEUTDEBB"))),
          Cdtr = PartyIdentification32(
            Nm = Some("Name of the creditor"),
          ),
          CdtrAcct = Some(CashAccount16(Id = AccountIdentification4Choice(DataRecord(<IBAN></IBAN>, "DE33500105176966528834")))),
          CdtrAgt = BranchAndFinancialInstitutionIdentification4(FinInstnId = FinancialInstitutionIdentification7(BIC = Some("DEUTDEBB"))),
          Purp = Some(Purpose2Choice(DataRecord(<Cd></Cd>, "RENT"))),
          RmtInf = Some(RemittanceInformation5(Ustrd = Seq("Some information about the payment here")))
        )
      )
    )
  )

  println(document.toString)
  println()

  val xmlDocument = scalaxb.toXML[Document](document, "Document", defaultScope)
  print(xmlDocument)
  XML.save("src/main/scala/sepa/example.xml", xmlDocument.head, "UTF-8", xmlDecl = true, null)

}
