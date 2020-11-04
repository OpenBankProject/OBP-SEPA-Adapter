import java.time.{LocalDate, LocalDateTime}

import com.openbankproject.commons.model.Iban
import model.types.Bic

import scala.reflect.io.{Directory, File, Path}

case class CreditTransferFileTest(
                                   messageIdInSepaFile: String,
                                   transactionAmount: BigDecimal,
                                   currentDateTime: LocalDateTime,
                                   settlementDate: LocalDate,
                                   transactionIdInSepaFile: String,
                                   debtorName: String,
                                   debtorIban: Iban,
                                   debtorBic: Bic,
                                   creditorName: String,
                                   creditorIban: Iban,
                                   creditorBic: Bic,
                                   transactionDescription: String
                                 ) {

  def write(): Path = {
    Directory("src/test/tempFiles").createDirectory()
    val filepath = Path(s"src/test/tempFiles/$messageIdInSepaFile.xml")
    val creditTransferTestFile = File(filepath).createFile()
    creditTransferTestFile.writeAll(toXML)
    filepath
  }

  def toXML: String =
    s"""<?xml version='1.0' encoding='UTF-8'?>
       |<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02">
       |    <FIToFICstmrCdtTrf>
       |        <GrpHdr>
       |            <MsgId>$messageIdInSepaFile</MsgId>
       |            <CreDtTm>$currentDateTime</CreDtTm>
       |            <NbOfTxs>1</NbOfTxs>
       |            <TtlIntrBkSttlmAmt Ccy="EUR">$transactionAmount</TtlIntrBkSttlmAmt>
       |            <IntrBkSttlmDt>$settlementDate</IntrBkSttlmDt>
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
       |                <EndToEndId>$transactionIdInSepaFile</EndToEndId>
       |                <TxId>$transactionIdInSepaFile</TxId>
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

}
