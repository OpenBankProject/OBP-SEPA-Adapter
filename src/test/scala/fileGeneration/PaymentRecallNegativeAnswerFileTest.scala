package fileGeneration

import java.time.{LocalDate, LocalDateTime}

import com.openbankproject.commons.model.Iban
import model.enums.sepaReasonCodes.PaymentRecallNegativeAnswerReasonCode.PaymentRecallNegativeAnswerReasonCode
import model.enums.sepaReasonCodes.PaymentRecallReasonCode.PaymentRecallReasonCode
import model.types.Bic

import scala.reflect.io.{Directory, File, Path}

case class PaymentRecallNegativeAnswerFileTest(
                                                messageIdInSepaFile: String,
                                                originalTransactionAmount: BigDecimal,
                                                currentDateTime: LocalDateTime,
                                                originalSettlementDate: LocalDate,
                                                originalMessageIdInSepaFile: String,
                                                paymentRecallNegativeAnswerIdInSepaFile: String,
                                                originalEndToEndId: String,
                                                originalTransactionIdInSepaFile: String,
                                                paymentRecallNegativeAnswerOriginatorName: String,
                                                paymentRecallNegativeAnswerReasonCode: PaymentRecallNegativeAnswerReasonCode,
                                                paymentRecallNegativeAnswerAdditionalInformation: String,
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
       |<Document xmlns="urn:iso:std:iso:20022:tech:xsd:camt.029.001.03">
       |    <RsltnOfInvstgtn>
       |        <Assgnmt>
       |            <Id>$messageIdInSepaFile</Id>
       |            <Assgnr>
       |                <Agt>
       |                    <FinInstnId>
       |                        <BIC>${originalCreditorBic.bic}</BIC>
       |                    </FinInstnId>
       |                </Agt>
       |            </Assgnr>
       |            <Assgne>
       |                <Agt>
       |                    <FinInstnId>
       |                        <BIC>${originalDebtorBic.bic}</BIC>
       |                    </FinInstnId>
       |                </Agt>
       |            </Assgne>
       |            <CreDtTm>$currentDateTime</CreDtTm>
       |        </Assgnmt>
       |        <Sts>
       |            <Conf>RJCR</Conf>
       |        </Sts>
       |        <CxlDtls>
       |            <TxInfAndSts>
       |                <CxlStsId>$paymentRecallNegativeAnswerIdInSepaFile</CxlStsId>
       |                <OrgnlGrpInf>
       |                    <OrgnlMsgId>$originalMessageIdInSepaFile</OrgnlMsgId>
       |                    <OrgnlMsgNmId>pacs.008.001.02</OrgnlMsgNmId>
       |                </OrgnlGrpInf>
       |                <OrgnlEndToEndId>$originalEndToEndId</OrgnlEndToEndId>
       |                <OrgnlTxId>$originalTransactionIdInSepaFile</OrgnlTxId>
       |                <TxCxlSts>RJCR</TxCxlSts>
       |                <CxlStsRsnInf>
       |                    <Orgtr>
       |                        <Nm>$paymentRecallNegativeAnswerOriginatorName</Nm>
       |                    </Orgtr>
       |                    <Rsn>
       |                        <Cd>${paymentRecallNegativeAnswerReasonCode.toString}</Cd>
       |                    </Rsn>
       |                    <AddtlInf>$paymentRecallNegativeAnswerAdditionalInformation</AddtlInf>
       |                </CxlStsRsnInf>
       |                <OrgnlTxRef>
       |                    <IntrBkSttlmAmt Ccy="EUR">$originalTransactionAmount</IntrBkSttlmAmt>
       |                    <IntrBkSttlmDt>$originalSettlementDate</IntrBkSttlmDt>
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
       |            </TxInfAndSts>
       |        </CxlDtls>
       |    </RsltnOfInvstgtn>
       |</Document>
       |""".stripMargin

}
