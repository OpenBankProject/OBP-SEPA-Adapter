package fileGeneration

import java.time.{LocalDate, LocalDateTime}

import com.openbankproject.commons.model.Iban
import model.enums.sepaReasonCodes.PaymentRecallReasonCode.PaymentRecallReasonCode
import model.types.Bic

import scala.reflect.io.{Directory, File, Path}

case class PaymentRecallFileTest(
                                  messageIdInSepaFile: String,
                                  originalTransactionAmount: BigDecimal,
                                  currentDateTime: LocalDateTime,
                                  originalSettlementDate: LocalDate,
                                  originalMessageIdInSepaFile: String,
                                  paymentRecallIdInSepaFile: String,
                                  originalEndToEndId: String,
                                  originalTransactionIdInSepaFile: String,
                                  paymentRecallOriginatorName: String,
                                  paymentRecallReasonCode: PaymentRecallReasonCode,
                                  paymentRecallAdditionalInformation: String,
                                  originalDebtorName: String,
                                  originalDebtorIban: Iban,
                                  originalDebtorBic: Bic,
                                  originalCreditorName: String,
                                  originalCreditorIban: Iban,
                                  originalCreditorBic: Bic,
                                  originalTransactionDescription: String
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
       |<Document xmlns="urn:iso:std:iso:20022:tech:xsd:camt.056.001.01">
       |    <FIToFIPmtCxlReq>
       |        <Assgnmt>
       |            <Id>$messageIdInSepaFile</Id>
       |            <Assgnr>
       |                <Agt>
       |                    <FinInstnId>
       |                        <BIC>${originalDebtorBic.bic}</BIC>
       |                    </FinInstnId>
       |                </Agt>
       |            </Assgnr>
       |            <Assgne>
       |                <Agt>
       |                    <FinInstnId>
       |                        <BIC>${originalCreditorBic.bic}</BIC>
       |                    </FinInstnId>
       |                </Agt>
       |            </Assgne>
       |            <CreDtTm>$currentDateTime</CreDtTm>
       |        </Assgnmt>
       |        <CtrlData>
       |            <NbOfTxs>1</NbOfTxs>
       |        </CtrlData>
       |        <Undrlyg>
       |            <TxInf>
       |                <CxlId>$paymentRecallIdInSepaFile</CxlId>
       |                <OrgnlGrpInf>
       |                    <OrgnlMsgId>$originalMessageIdInSepaFile</OrgnlMsgId>
       |                    <OrgnlMsgNmId>pacs.008.001.02</OrgnlMsgNmId>
       |                </OrgnlGrpInf>
       |                <OrgnlEndToEndId>$originalEndToEndId</OrgnlEndToEndId>
       |                <OrgnlTxId>$originalTransactionIdInSepaFile</OrgnlTxId>
       |                <OrgnlIntrBkSttlmAmt Ccy="EUR">$originalTransactionAmount</OrgnlIntrBkSttlmAmt>
       |                <OrgnlIntrBkSttlmDt>$originalSettlementDate</OrgnlIntrBkSttlmDt>
       |                <CxlRsnInf>
       |                    <Orgtr>
       |                        <Nm>$paymentRecallOriginatorName</Nm>
       |                    </Orgtr>
       |                    <Rsn>
       |                        <Prtry>${paymentRecallReasonCode.toString}</Prtry>
       |                    </Rsn>
       |                    <AddtlInf>$paymentRecallAdditionalInformation</AddtlInf>
       |                </CxlRsnInf>
       |                <OrgnlTxRef>
       |                    <RmtInf>
       |                        <Ustrd>$originalTransactionDescription</Ustrd>
       |                    </RmtInf>
       |                    <Dbtr>
       |                        <Nm>$originalDebtorName</Nm>
       |                    </Dbtr>
       |                    <DbtrAcct>
       |                        <Id>
       |                            <IBAN>${originalDebtorIban.iban}</IBAN>
       |                        </Id>
       |                    </DbtrAcct>
       |                    <DbtrAgt>
       |                        <FinInstnId>
       |                            <BIC>${originalDebtorBic.bic}</BIC>
       |                        </FinInstnId>
       |                    </DbtrAgt>
       |                    <CdtrAgt>
       |                        <FinInstnId>
       |                            <BIC>${originalCreditorBic.bic}</BIC>
       |                        </FinInstnId>
       |                    </CdtrAgt>
       |                    <Cdtr>
       |                        <Nm>$originalCreditorName</Nm>
       |                    </Cdtr>
       |                    <CdtrAcct>
       |                        <Id>
       |                            <IBAN>${originalCreditorIban.iban}</IBAN>
       |                        </Id>
       |                    </CdtrAcct>
       |                </OrgnlTxRef>
       |            </TxInf>
       |        </Undrlyg>
       |    </FIToFIPmtCxlReq>
       |</Document>
       |""".stripMargin

}
