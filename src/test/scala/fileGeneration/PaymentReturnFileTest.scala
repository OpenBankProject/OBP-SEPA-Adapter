package fileGeneration

import java.time.{LocalDate, LocalDateTime}

import com.openbankproject.commons.model.Iban
import model.enums.sepaReasonCodes.PaymentReturnReasonCode.PaymentReturnReasonCode
import model.types.Bic

import scala.reflect.io.{Directory, File, Path}

case class PaymentReturnFileTest(
                                  messageIdInSepaFile: String,
                                  originalTransactionAmount: BigDecimal,
                                  currentDateTime: LocalDateTime,
                                  settlementDate: LocalDate,
                                  originalMessageIdInSepaFile: String,
                                  paymentReturnIdInSepaFile: String,
                                  originalEndToEndId: String,
                                  originalTransactionIdInSepaFile: String,
                                  paymentReturnOriginatorBic: Bic,
                                  paymentReturnReasonCode: PaymentReturnReasonCode,
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
       |<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.004.001.02">
       |    <PmtRtr>
       |        <GrpHdr>
       |            <MsgId>$messageIdInSepaFile</MsgId>
       |            <CreDtTm>$currentDateTime</CreDtTm>
       |            <NbOfTxs>1</NbOfTxs>
       |            <TtlRtrdIntrBkSttlmAmt Ccy="EUR">$originalTransactionAmount</TtlRtrdIntrBkSttlmAmt>
       |            <IntrBkSttlmDt>$settlementDate</IntrBkSttlmDt>
       |            <SttlmInf>
       |                <SttlmMtd>CLRG</SttlmMtd>
       |            </SttlmInf>
       |        </GrpHdr>
       |        <OrgnlGrpInf>
       |            <OrgnlMsgId>$originalMessageIdInSepaFile</OrgnlMsgId>
       |            <OrgnlMsgNmId>pacs.008.001.02</OrgnlMsgNmId>
       |        </OrgnlGrpInf>
       |        <TxInf>
       |            <RtrId>$paymentReturnIdInSepaFile</RtrId>
       |            <OrgnlEndToEndId>$originalEndToEndId</OrgnlEndToEndId>
       |            <OrgnlTxId>$originalTransactionIdInSepaFile</OrgnlTxId>
       |            <OrgnlIntrBkSttlmAmt Ccy="EUR">$originalTransactionAmount</OrgnlIntrBkSttlmAmt>
       |            <RtrdIntrBkSttlmAmt Ccy="EUR">$originalTransactionAmount</RtrdIntrBkSttlmAmt>
       |            <ChrgBr>SLEV</ChrgBr>
       |            <RtrRsnInf>
       |                <Orgtr>
       |                    <Id>
       |                        <OrgId>
       |                            <BICOrBEI>${paymentReturnOriginatorBic.bic}</BICOrBEI>
       |                        </OrgId>
       |                    </Id>
       |                </Orgtr>
       |                <Rsn>
       |                    <Cd>${paymentReturnReasonCode.toString}</Cd>
       |                </Rsn>
       |            </RtrRsnInf>
       |            <OrgnlTxRef>
       |                <RmtInf>
       |                    <Ustrd>$originalTransactionDescription</Ustrd>
       |                </RmtInf>
       |                <Dbtr>
       |                    <Nm>$originalDebtorName</Nm>
       |                </Dbtr>
       |                <DbtrAcct>
       |                    <Id>
       |                        <IBAN>${originalDebtorIban.iban}</IBAN>
       |                    </Id>
       |                </DbtrAcct>
       |                <DbtrAgt>
       |                    <FinInstnId>
       |                        <BIC>${originalDebtorBic.bic}</BIC>
       |                    </FinInstnId>
       |                </DbtrAgt>
       |                <CdtrAgt>
       |                    <FinInstnId>
       |                        <BIC>${originalCreditorBic.bic}</BIC>
       |                    </FinInstnId>
       |                </CdtrAgt>
       |                <Cdtr>
       |                    <Nm>$originalCreditorName</Nm>
       |                </Cdtr>
       |                <CdtrAcct>
       |                    <Id>
       |                        <IBAN>${originalCreditorIban.iban}</IBAN>
       |                    </Id>
       |                </CdtrAcct>
       |            </OrgnlTxRef>
       |        </TxInf>
       |    </PmtRtr>
       |</Document>
       |""".stripMargin

}
