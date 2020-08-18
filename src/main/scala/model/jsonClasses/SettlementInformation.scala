package model.jsonClasses

import com.openbankproject.commons.model.Iban
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import model.jsonClasses.ClearingSystemIdentificationType.ClearingSystemIdentificationType
import model.jsonClasses.OtherAccountIdentificationSchemeType.OtherAccountIdentificationSchemeType
import model.jsonClasses.SettlementMethod.SettlementMethod
import scalaxb.DataRecord
import scalaxb.DataRecord.__StringXMLFormat
import sepa.sct.generated._

case class SettlementInformation(
                                  settlementMethod: SettlementMethod,
                                  settlementAccountIban: Option[Iban] = None,
                                  settlementAccountOtherIdentification: Option[OtherAccountIdentification] = None,
                                  clearingSystem: Option[ClearingSystemIdentification] = None
                                ) {
  def toJson: Json = this.asJson

  def toSettlementInformation13CT(implicit documentType: creditTransfer.Document): creditTransfer.SettlementInformation13 = creditTransfer.SettlementInformation13(
    SttlmMtd = creditTransfer.SettlementMethod1Code.fromString(this.settlementMethod.toString, creditTransfer.defaultScope),
    SttlmAcct = this.settlementAccountIban.map(accountIban => creditTransfer.CashAccount16(Id = creditTransfer.AccountIdentification4Choice(
      accountidentification4choicableoption = DataRecord(None, Some(SettlementAccountType.IBAN.toString), accountIban.iban)
    ))).orElse(this.settlementAccountOtherIdentification.map(otherAccountIdentification =>
      creditTransfer.CashAccount16(creditTransfer.AccountIdentification4Choice(
        DataRecord(None, Some(SettlementAccountType.OTHER.toString),
          creditTransfer.GenericAccountIdentification1(
            Id = otherAccountIdentification.identification,
            SchmeNm = otherAccountIdentification.scheme.map(accountIdentificationScheme =>
              creditTransfer.AccountSchemeName1Choice(
                DataRecord(None, Some(accountIdentificationScheme.schemeType.toString), accountIdentificationScheme.schemeName)
              )
            ),
            Issr = otherAccountIdentification.issuer
          )
        )
      ))
    )),
    ClrSys = this.clearingSystem.map(clearingSystem =>
      creditTransfer.ClearingSystemIdentification3Choice(
        DataRecord(None, Some(clearingSystem.identificationType.toString), clearingSystem.identification)
      )
    )
  )
  def toSettlementInformation13PR(implicit documentType: paymentReturn.Document): paymentReturn.SettlementInformation13 = paymentReturn.SettlementInformation13(
    SttlmMtd = paymentReturn.SettlementMethod1Code.fromString(this.settlementMethod.toString, paymentReturn.defaultScope),
    SttlmAcct = this.settlementAccountIban.map(accountIban => paymentReturn.CashAccount16(Id = paymentReturn.AccountIdentification4Choice(
      accountidentification4choicableoption = DataRecord(None, Some(SettlementAccountType.IBAN.toString), accountIban.iban)
    ))).orElse(this.settlementAccountOtherIdentification.map(otherAccountIdentification =>
      paymentReturn.CashAccount16(paymentReturn.AccountIdentification4Choice(
        DataRecord(None, Some(SettlementAccountType.OTHER.toString),
          paymentReturn.GenericAccountIdentification1(
            Id = otherAccountIdentification.identification,
            SchmeNm = otherAccountIdentification.scheme.map(accountIdentificationScheme =>
              paymentReturn.AccountSchemeName1Choice(
                DataRecord(None, Some(accountIdentificationScheme.schemeType.toString), accountIdentificationScheme.schemeName)
              )
            ),
            Issr = otherAccountIdentification.issuer
          )
        )
      ))
    )),
    ClrSys = this.clearingSystem.map(clearingSystem =>
      paymentReturn.ClearingSystemIdentification3Choice(
        DataRecord(None, Some(clearingSystem.identificationType.toString), clearingSystem.identification)
      )
    )
  )
  def toSettlementInformation13(implicit documentType: paymentRecall.Document): paymentRecall.SettlementInformation13 = paymentRecall.SettlementInformation13(
    SttlmMtd = paymentRecall.SettlementMethod1Code.fromString(this.settlementMethod.toString, paymentRecall.defaultScope),
    SttlmAcct = this.settlementAccountIban.map(accountIban => paymentRecall.CashAccount16(Id = paymentRecall.AccountIdentification4Choice(
      accountidentification4choicableoption = DataRecord(None, Some(SettlementAccountType.IBAN.toString), accountIban.iban)
    ))).orElse(this.settlementAccountOtherIdentification.map(otherAccountIdentification =>
      paymentRecall.CashAccount16(paymentRecall.AccountIdentification4Choice(
        DataRecord(None, Some(SettlementAccountType.OTHER.toString),
          paymentRecall.GenericAccountIdentification1(
            Id = otherAccountIdentification.identification,
            SchmeNm = otherAccountIdentification.scheme.map(accountIdentificationScheme =>
              paymentRecall.AccountSchemeName1Choice(
                DataRecord(None, Some(accountIdentificationScheme.schemeType.toString), accountIdentificationScheme.schemeName)
              )
            ),
            Issr = otherAccountIdentification.issuer
          )
        )
      ))
    )),
    ClrSys = this.clearingSystem.map(clearingSystem =>
      paymentRecall.ClearingSystemIdentification3Choice(
        DataRecord(None, Some(clearingSystem.identificationType.toString), clearingSystem.identification)
      )
    )
  )
  def toSettlementInformation13(implicit documentType: paymentRecallNegativeAnswer.Document): paymentRecallNegativeAnswer.SettlementInformation13 = paymentRecallNegativeAnswer.SettlementInformation13(
    SttlmMtd = paymentRecallNegativeAnswer.SettlementMethod1Code.fromString(this.settlementMethod.toString, paymentRecallNegativeAnswer.defaultScope),
    SttlmAcct = this.settlementAccountIban.map(accountIban => paymentRecallNegativeAnswer.CashAccount16(Id = paymentRecallNegativeAnswer.AccountIdentification4Choice(
      accountidentification4choicableoption = DataRecord(None, Some(SettlementAccountType.IBAN.toString), accountIban.iban)
    ))).orElse(this.settlementAccountOtherIdentification.map(otherAccountIdentification =>
      paymentRecallNegativeAnswer.CashAccount16(paymentRecallNegativeAnswer.AccountIdentification4Choice(
        DataRecord(None, Some(SettlementAccountType.OTHER.toString),
          paymentRecallNegativeAnswer.GenericAccountIdentification1(
            Id = otherAccountIdentification.identification,
            SchmeNm = otherAccountIdentification.scheme.map(accountIdentificationScheme =>
              paymentRecallNegativeAnswer.AccountSchemeName1Choice(
                DataRecord(None, Some(accountIdentificationScheme.schemeType.toString), accountIdentificationScheme.schemeName)
              )
            ),
            Issr = otherAccountIdentification.issuer
          )
        )
      ))
    )),
    ClrSys = this.clearingSystem.map(clearingSystem =>
      paymentRecallNegativeAnswer.ClearingSystemIdentification3Choice(
        DataRecord(None, Some(clearingSystem.identificationType.toString), clearingSystem.identification)
      )
    )
  )
  
  def toSettlementInstruction4(implicit documentType: inquiryClaimNonReceipt.Document): inquiryClaimNonReceipt.SettlementInstruction4 = inquiryClaimNonReceipt.SettlementInstruction4(
    SttlmMtd = inquiryClaimNonReceipt.SettlementMethod1Code.fromString(this.settlementMethod.toString, inquiryClaimNonReceipt.defaultScope),
    SttlmAcct = this.settlementAccountIban.map(accountIban => inquiryClaimNonReceipt.CashAccount24(Id = inquiryClaimNonReceipt.AccountIdentification4Choice(
      accountidentification4choicableoption = DataRecord(None, Some(SettlementAccountType.IBAN.toString), accountIban.iban)
    ))).orElse(this.settlementAccountOtherIdentification.map(otherAccountIdentification =>
      inquiryClaimNonReceipt.CashAccount24(inquiryClaimNonReceipt.AccountIdentification4Choice(
        DataRecord(None, Some(SettlementAccountType.OTHER.toString),
          inquiryClaimNonReceipt.GenericAccountIdentification1(
            Id = otherAccountIdentification.identification,
            SchmeNm = otherAccountIdentification.scheme.map(accountIdentificationScheme =>
              inquiryClaimNonReceipt.AccountSchemeName1Choice(
                DataRecord(None, Some(accountIdentificationScheme.schemeType.toString), accountIdentificationScheme.schemeName)
              )
            ),
            Issr = otherAccountIdentification.issuer
          )
        )
      ))
    )),
    ClrSys = this.clearingSystem.map(clearingSystem =>
      inquiryClaimNonReceipt.ClearingSystemIdentification3Choice(
        DataRecord(None, Some(clearingSystem.identificationType.toString), clearingSystem.identification)
      )
    )
  )
  def toSettlementInstruction4(implicit documentType: inquiryClaimNonReceiptPositiveAnswer.Document): inquiryClaimNonReceiptPositiveAnswer.SettlementInstruction4 = inquiryClaimNonReceiptPositiveAnswer.SettlementInstruction4(
    SttlmMtd = inquiryClaimNonReceiptPositiveAnswer.SettlementMethod1Code.fromString(this.settlementMethod.toString, inquiryClaimNonReceiptPositiveAnswer.defaultScope),
    SttlmAcct = this.settlementAccountIban.map(accountIban => inquiryClaimNonReceiptPositiveAnswer.CashAccount24(Id = inquiryClaimNonReceiptPositiveAnswer.AccountIdentification4Choice(
      accountidentification4choicableoption = DataRecord(None, Some(SettlementAccountType.IBAN.toString), accountIban.iban)
    ))).orElse(this.settlementAccountOtherIdentification.map(otherAccountIdentification =>
      inquiryClaimNonReceiptPositiveAnswer.CashAccount24(inquiryClaimNonReceiptPositiveAnswer.AccountIdentification4Choice(
        DataRecord(None, Some(SettlementAccountType.OTHER.toString),
          inquiryClaimNonReceiptPositiveAnswer.GenericAccountIdentification1(
            Id = otherAccountIdentification.identification,
            SchmeNm = otherAccountIdentification.scheme.map(accountIdentificationScheme =>
              inquiryClaimNonReceiptPositiveAnswer.AccountSchemeName1Choice(
                DataRecord(None, Some(accountIdentificationScheme.schemeType.toString), accountIdentificationScheme.schemeName)
              )
            ),
            Issr = otherAccountIdentification.issuer
          )
        )
      ))
    )),
    ClrSys = this.clearingSystem.map(clearingSystem =>
      inquiryClaimNonReceiptPositiveAnswer.ClearingSystemIdentification3Choice(
        DataRecord(None, Some(clearingSystem.identificationType.toString), clearingSystem.identification)
      )
    )
  )
  def toSettlementInstruction4(implicit documentType: inquiryClaimValueDateCorrection.Document): inquiryClaimValueDateCorrection.SettlementInstruction4 = inquiryClaimValueDateCorrection.SettlementInstruction4(
    SttlmMtd = inquiryClaimValueDateCorrection.SettlementMethod1Code.fromString(this.settlementMethod.toString, inquiryClaimValueDateCorrection.defaultScope),
    SttlmAcct = this.settlementAccountIban.map(accountIban => inquiryClaimValueDateCorrection.CashAccount24(Id = inquiryClaimValueDateCorrection.AccountIdentification4Choice(
      accountidentification4choicableoption = DataRecord(None, Some(SettlementAccountType.IBAN.toString), accountIban.iban)
    ))).orElse(this.settlementAccountOtherIdentification.map(otherAccountIdentification =>
      inquiryClaimValueDateCorrection.CashAccount24(inquiryClaimValueDateCorrection.AccountIdentification4Choice(
        DataRecord(None, Some(SettlementAccountType.OTHER.toString),
          inquiryClaimValueDateCorrection.GenericAccountIdentification1(
            Id = otherAccountIdentification.identification,
            SchmeNm = otherAccountIdentification.scheme.map(accountIdentificationScheme =>
              inquiryClaimValueDateCorrection.AccountSchemeName1Choice(
                DataRecord(None, Some(accountIdentificationScheme.schemeType.toString), accountIdentificationScheme.schemeName)
              )
            ),
            Issr = otherAccountIdentification.issuer
          )
        )
      ))
    )),
    ClrSys = this.clearingSystem.map(clearingSystem =>
      inquiryClaimValueDateCorrection.ClearingSystemIdentification3Choice(
        DataRecord(None, Some(clearingSystem.identificationType.toString), clearingSystem.identification)
      )
    )
  )
  def toSettlementInstruction4(implicit documentType: inquiryClaimValueDateCorrectionPositiveAnswer.Document): inquiryClaimValueDateCorrectionPositiveAnswer.SettlementInstruction4 = inquiryClaimValueDateCorrectionPositiveAnswer.SettlementInstruction4(
    SttlmMtd = inquiryClaimValueDateCorrectionPositiveAnswer.SettlementMethod1Code.fromString(this.settlementMethod.toString, inquiryClaimValueDateCorrectionPositiveAnswer.defaultScope),
    SttlmAcct = this.settlementAccountIban.map(accountIban => inquiryClaimValueDateCorrectionPositiveAnswer.CashAccount24(Id = inquiryClaimValueDateCorrectionPositiveAnswer.AccountIdentification4Choice(
      accountidentification4choicableoption = DataRecord(None, Some(SettlementAccountType.IBAN.toString), accountIban.iban)
    ))).orElse(this.settlementAccountOtherIdentification.map(otherAccountIdentification =>
      inquiryClaimValueDateCorrectionPositiveAnswer.CashAccount24(inquiryClaimValueDateCorrectionPositiveAnswer.AccountIdentification4Choice(
        DataRecord(None, Some(SettlementAccountType.OTHER.toString),
          inquiryClaimValueDateCorrectionPositiveAnswer.GenericAccountIdentification1(
            Id = otherAccountIdentification.identification,
            SchmeNm = otherAccountIdentification.scheme.map(accountIdentificationScheme =>
              inquiryClaimValueDateCorrectionPositiveAnswer.AccountSchemeName1Choice(
                DataRecord(None, Some(accountIdentificationScheme.schemeType.toString), accountIdentificationScheme.schemeName)
              )
            ),
            Issr = otherAccountIdentification.issuer
          )
        )
      ))
    )),
    ClrSys = this.clearingSystem.map(clearingSystem =>
      inquiryClaimValueDateCorrectionPositiveAnswer.ClearingSystemIdentification3Choice(
        DataRecord(None, Some(clearingSystem.identificationType.toString), clearingSystem.identification)
      )
    )
  )
  def toSettlementInstruction4(implicit documentType: inquiryClaimValueDateCorrectionNegativeAnswer.Document): inquiryClaimValueDateCorrectionNegativeAnswer.SettlementInstruction4 = inquiryClaimValueDateCorrectionNegativeAnswer.SettlementInstruction4(
    SttlmMtd = inquiryClaimValueDateCorrectionNegativeAnswer.SettlementMethod1Code.fromString(this.settlementMethod.toString, inquiryClaimValueDateCorrectionNegativeAnswer.defaultScope),
    SttlmAcct = this.settlementAccountIban.map(accountIban => inquiryClaimValueDateCorrectionNegativeAnswer.CashAccount24(Id = inquiryClaimValueDateCorrectionNegativeAnswer.AccountIdentification4Choice(
      accountidentification4choicableoption = DataRecord(None, Some(SettlementAccountType.IBAN.toString), accountIban.iban)
    ))).orElse(this.settlementAccountOtherIdentification.map(otherAccountIdentification =>
      inquiryClaimValueDateCorrectionNegativeAnswer.CashAccount24(inquiryClaimValueDateCorrectionNegativeAnswer.AccountIdentification4Choice(
        DataRecord(None, Some(SettlementAccountType.OTHER.toString),
          inquiryClaimValueDateCorrectionNegativeAnswer.GenericAccountIdentification1(
            Id = otherAccountIdentification.identification,
            SchmeNm = otherAccountIdentification.scheme.map(accountIdentificationScheme =>
              inquiryClaimValueDateCorrectionNegativeAnswer.AccountSchemeName1Choice(
                DataRecord(None, Some(accountIdentificationScheme.schemeType.toString), accountIdentificationScheme.schemeName)
              )
            ),
            Issr = otherAccountIdentification.issuer
          )
        )
      ))
    )),
    ClrSys = this.clearingSystem.map(clearingSystem =>
      inquiryClaimValueDateCorrectionNegativeAnswer.ClearingSystemIdentification3Choice(
        DataRecord(None, Some(clearingSystem.identificationType.toString), clearingSystem.identification)
      )
    )
  )
  def toSettlementInstruction4(implicit documentType: requestStatusUpdate.Document): requestStatusUpdate.SettlementInstruction4 = requestStatusUpdate.SettlementInstruction4(
    SttlmMtd = requestStatusUpdate.SettlementMethod1Code.fromString(this.settlementMethod.toString, requestStatusUpdate.defaultScope),
    SttlmAcct = this.settlementAccountIban.map(accountIban => requestStatusUpdate.CashAccount24(Id = requestStatusUpdate.AccountIdentification4Choice(
      accountidentification4choicableoption = DataRecord(None, Some(SettlementAccountType.IBAN.toString), accountIban.iban)
    ))).orElse(this.settlementAccountOtherIdentification.map(otherAccountIdentification =>
      requestStatusUpdate.CashAccount24(requestStatusUpdate.AccountIdentification4Choice(
        DataRecord(None, Some(SettlementAccountType.OTHER.toString),
          requestStatusUpdate.GenericAccountIdentification1(
            Id = otherAccountIdentification.identification,
            SchmeNm = otherAccountIdentification.scheme.map(accountIdentificationScheme =>
              requestStatusUpdate.AccountSchemeName1Choice(
                DataRecord(None, Some(accountIdentificationScheme.schemeType.toString), accountIdentificationScheme.schemeName)
              )
            ),
            Issr = otherAccountIdentification.issuer
          )
        )
      ))
    )),
    ClrSys = this.clearingSystem.map(clearingSystem =>
      requestStatusUpdate.ClearingSystemIdentification3Choice(
        DataRecord(None, Some(clearingSystem.identificationType.toString), clearingSystem.identification)
      )
    )
  )


}


case object SettlementInformation {
  def fromJson(jsonString: String): Option[SettlementInformation] = decode[SettlementInformation](jsonString).toOption

  def fromSettlementInformation13(settlementInformation13: creditTransfer.SettlementInformation13able): SettlementInformation = {
    SettlementInformation(
      settlementMethod = SettlementMethod.withName(settlementInformation13.SttlmMtd.toString),
      settlementAccountIban = settlementInformation13.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.key.getOrElse("") == SettlementAccountType.IBAN.toString =>
          Iban(cashAccount.Id.accountidentification4choicableoption.value.toString)
      },
      settlementAccountOtherIdentification = settlementInformation13.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.value.isInstanceOf[creditTransfer.GenericAccountIdentification1] =>
          val accountIdentification = cashAccount.Id.accountidentification4choicableoption.value.asInstanceOf[creditTransfer.GenericAccountIdentification1]
          OtherAccountIdentification(
            identification = accountIdentification.Id,
            scheme = accountIdentification.SchmeNm.map(scheme =>
              OtherAccountIdentificationScheme(
                schemeType = OtherAccountIdentificationSchemeType.withName(scheme.accountschemename1choiceoption.key.get),
                schemeName = scheme.accountschemename1choiceoption.value.toString
              )),
            issuer = accountIdentification.Issr
          )
      },
      clearingSystem = settlementInformation13.ClrSys.map(clearingSystem =>
        ClearingSystemIdentification(
          identificationType = ClearingSystemIdentificationType.withName(clearingSystem.clearingsystemidentification3choiceoption.key.get),
          identification = clearingSystem.clearingsystemidentification3choiceoption.value.toString
        )
      )
    )
  }
  def fromSettlementInformation13(settlementInformation13: paymentReturn.SettlementInformation13able): SettlementInformation = {
    SettlementInformation(
      settlementMethod = SettlementMethod.withName(settlementInformation13.SttlmMtd.toString),
      settlementAccountIban = settlementInformation13.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.key.getOrElse("") == SettlementAccountType.IBAN.toString =>
          Iban(cashAccount.Id.accountidentification4choicableoption.value.toString)
      },
      settlementAccountOtherIdentification = settlementInformation13.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.value.isInstanceOf[paymentReturn.GenericAccountIdentification1] =>
          val accountIdentification = cashAccount.Id.accountidentification4choicableoption.value.asInstanceOf[paymentReturn.GenericAccountIdentification1]
          OtherAccountIdentification(
            identification = accountIdentification.Id,
            scheme = accountIdentification.SchmeNm.map(scheme =>
              OtherAccountIdentificationScheme(
                schemeType = OtherAccountIdentificationSchemeType.withName(scheme.accountschemename1choiceoption.key.get),
                schemeName = scheme.accountschemename1choiceoption.value.toString
              )),
            issuer = accountIdentification.Issr
          )
      },
      clearingSystem = settlementInformation13.ClrSys.map(clearingSystem =>
        ClearingSystemIdentification(
          identificationType = ClearingSystemIdentificationType.withName(clearingSystem.clearingsystemidentification3choiceoption.key.get),
          identification = clearingSystem.clearingsystemidentification3choiceoption.value.toString
        )
      )
    )
  }
  def fromSettlementInformation13(settlementInformation13: paymentReject.SettlementInformation13): SettlementInformation = {
    SettlementInformation(
      settlementMethod = SettlementMethod.withName(settlementInformation13.SttlmMtd.toString),
      settlementAccountIban = settlementInformation13.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.key.getOrElse("") == SettlementAccountType.IBAN.toString =>
          Iban(cashAccount.Id.accountidentification4choicableoption.value.toString)
      },
      settlementAccountOtherIdentification = settlementInformation13.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.value.isInstanceOf[paymentReject.GenericAccountIdentification1] =>
          val accountIdentification = cashAccount.Id.accountidentification4choicableoption.value.asInstanceOf[paymentReject.GenericAccountIdentification1]
          OtherAccountIdentification(
            identification = accountIdentification.Id,
            scheme = accountIdentification.SchmeNm.map(scheme =>
              OtherAccountIdentificationScheme(
                schemeType = OtherAccountIdentificationSchemeType.withName(scheme.accountschemename1choiceoption.key.get),
                schemeName = scheme.accountschemename1choiceoption.value.toString
              )),
            issuer = accountIdentification.Issr
          )
      },
      clearingSystem = settlementInformation13.ClrSys.map(clearingSystem =>
        ClearingSystemIdentification(
          identificationType = ClearingSystemIdentificationType.withName(clearingSystem.clearingsystemidentification3choiceoption.key.get),
          identification = clearingSystem.clearingsystemidentification3choiceoption.value.toString
        )
      )
    )
  }
  def fromSettlementInformation13(settlementInformation13: paymentRecall.SettlementInformation13): SettlementInformation = {
    SettlementInformation(
      settlementMethod = SettlementMethod.withName(settlementInformation13.SttlmMtd.toString),
      settlementAccountIban = settlementInformation13.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.key.getOrElse("") == SettlementAccountType.IBAN.toString =>
          Iban(cashAccount.Id.accountidentification4choicableoption.value.toString)
      },
      settlementAccountOtherIdentification = settlementInformation13.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.value.isInstanceOf[paymentRecall.GenericAccountIdentification1] =>
          val accountIdentification = cashAccount.Id.accountidentification4choicableoption.value.asInstanceOf[paymentRecall.GenericAccountIdentification1]
          OtherAccountIdentification(
            identification = accountIdentification.Id,
            scheme = accountIdentification.SchmeNm.map(scheme =>
              OtherAccountIdentificationScheme(
                schemeType = OtherAccountIdentificationSchemeType.withName(scheme.accountschemename1choiceoption.key.get),
                schemeName = scheme.accountschemename1choiceoption.value.toString
              )),
            issuer = accountIdentification.Issr
          )
      },
      clearingSystem = settlementInformation13.ClrSys.map(clearingSystem =>
        ClearingSystemIdentification(
          identificationType = ClearingSystemIdentificationType.withName(clearingSystem.clearingsystemidentification3choiceoption.key.get),
          identification = clearingSystem.clearingsystemidentification3choiceoption.value.toString
        )
      )
    )
  }
  def fromSettlementInformation13(settlementInformation13: paymentRecallNegativeAnswer.SettlementInformation13): SettlementInformation = {
    SettlementInformation(
      settlementMethod = SettlementMethod.withName(settlementInformation13.SttlmMtd.toString),
      settlementAccountIban = settlementInformation13.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.key.getOrElse("") == SettlementAccountType.IBAN.toString =>
          Iban(cashAccount.Id.accountidentification4choicableoption.value.toString)
      },
      settlementAccountOtherIdentification = settlementInformation13.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.value.isInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1] =>
          val accountIdentification = cashAccount.Id.accountidentification4choicableoption.value.asInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1]
          OtherAccountIdentification(
            identification = accountIdentification.Id,
            scheme = accountIdentification.SchmeNm.map(scheme =>
              OtherAccountIdentificationScheme(
                schemeType = OtherAccountIdentificationSchemeType.withName(scheme.accountschemename1choiceoption.key.get),
                schemeName = scheme.accountschemename1choiceoption.value.toString
              )),
            issuer = accountIdentification.Issr
          )
      },
      clearingSystem = settlementInformation13.ClrSys.map(clearingSystem =>
        ClearingSystemIdentification(
          identificationType = ClearingSystemIdentificationType.withName(clearingSystem.clearingsystemidentification3choiceoption.key.get),
          identification = clearingSystem.clearingsystemidentification3choiceoption.value.toString
        )
      )
    )
  }
  
  def fromSettlementInstruction4(settlementInstruction4: inquiryClaimNonReceipt.SettlementInstruction4): SettlementInformation = {
    SettlementInformation(
      settlementMethod = SettlementMethod.withName(settlementInstruction4.SttlmMtd.toString),
      settlementAccountIban = settlementInstruction4.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.key.getOrElse("") == SettlementAccountType.IBAN.toString =>
          Iban(cashAccount.Id.accountidentification4choicableoption.value.toString)
      },
      settlementAccountOtherIdentification = settlementInstruction4.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.value.isInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1] =>
          val accountIdentification = cashAccount.Id.accountidentification4choicableoption.value.asInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1]
          OtherAccountIdentification(
            identification = accountIdentification.Id,
            scheme = accountIdentification.SchmeNm.map(scheme =>
              OtherAccountIdentificationScheme(
                schemeType = OtherAccountIdentificationSchemeType.withName(scheme.accountschemename1choiceoption.key.get),
                schemeName = scheme.accountschemename1choiceoption.value.toString
              )),
            issuer = accountIdentification.Issr
          )
      },
      clearingSystem = settlementInstruction4.ClrSys.map(clearingSystem =>
        ClearingSystemIdentification(
          identificationType = ClearingSystemIdentificationType.withName(clearingSystem.clearingsystemidentification3choiceoption.key.get),
          identification = clearingSystem.clearingsystemidentification3choiceoption.value.toString
        )
      )
    )
  }
  def fromSettlementInstruction4(settlementInstruction4: inquiryClaimNonReceiptPositiveAnswer.SettlementInstruction4): SettlementInformation = {
    SettlementInformation(
      settlementMethod = SettlementMethod.withName(settlementInstruction4.SttlmMtd.toString),
      settlementAccountIban = settlementInstruction4.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.key.getOrElse("") == SettlementAccountType.IBAN.toString =>
          Iban(cashAccount.Id.accountidentification4choicableoption.value.toString)
      },
      settlementAccountOtherIdentification = settlementInstruction4.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.value.isInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1] =>
          val accountIdentification = cashAccount.Id.accountidentification4choicableoption.value.asInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1]
          OtherAccountIdentification(
            identification = accountIdentification.Id,
            scheme = accountIdentification.SchmeNm.map(scheme =>
              OtherAccountIdentificationScheme(
                schemeType = OtherAccountIdentificationSchemeType.withName(scheme.accountschemename1choiceoption.key.get),
                schemeName = scheme.accountschemename1choiceoption.value.toString
              )),
            issuer = accountIdentification.Issr
          )
      },
      clearingSystem = settlementInstruction4.ClrSys.map(clearingSystem =>
        ClearingSystemIdentification(
          identificationType = ClearingSystemIdentificationType.withName(clearingSystem.clearingsystemidentification3choiceoption.key.get),
          identification = clearingSystem.clearingsystemidentification3choiceoption.value.toString
        )
      )
    )
  }
  def fromSettlementInstruction4(settlementInstruction4: inquiryClaimValueDateCorrection.SettlementInstruction4): SettlementInformation = {
    SettlementInformation(
      settlementMethod = SettlementMethod.withName(settlementInstruction4.SttlmMtd.toString),
      settlementAccountIban = settlementInstruction4.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.key.getOrElse("") == SettlementAccountType.IBAN.toString =>
          Iban(cashAccount.Id.accountidentification4choicableoption.value.toString)
      },
      settlementAccountOtherIdentification = settlementInstruction4.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.value.isInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1] =>
          val accountIdentification = cashAccount.Id.accountidentification4choicableoption.value.asInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1]
          OtherAccountIdentification(
            identification = accountIdentification.Id,
            scheme = accountIdentification.SchmeNm.map(scheme =>
              OtherAccountIdentificationScheme(
                schemeType = OtherAccountIdentificationSchemeType.withName(scheme.accountschemename1choiceoption.key.get),
                schemeName = scheme.accountschemename1choiceoption.value.toString
              )),
            issuer = accountIdentification.Issr
          )
      },
      clearingSystem = settlementInstruction4.ClrSys.map(clearingSystem =>
        ClearingSystemIdentification(
          identificationType = ClearingSystemIdentificationType.withName(clearingSystem.clearingsystemidentification3choiceoption.key.get),
          identification = clearingSystem.clearingsystemidentification3choiceoption.value.toString
        )
      )
    )
  }
  def fromSettlementInstruction4(settlementInstruction4: inquiryClaimValueDateCorrectionPositiveAnswer.SettlementInstruction4): SettlementInformation = {
    SettlementInformation(
      settlementMethod = SettlementMethod.withName(settlementInstruction4.SttlmMtd.toString),
      settlementAccountIban = settlementInstruction4.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.key.getOrElse("") == SettlementAccountType.IBAN.toString =>
          Iban(cashAccount.Id.accountidentification4choicableoption.value.toString)
      },
      settlementAccountOtherIdentification = settlementInstruction4.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.value.isInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1] =>
          val accountIdentification = cashAccount.Id.accountidentification4choicableoption.value.asInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1]
          OtherAccountIdentification(
            identification = accountIdentification.Id,
            scheme = accountIdentification.SchmeNm.map(scheme =>
              OtherAccountIdentificationScheme(
                schemeType = OtherAccountIdentificationSchemeType.withName(scheme.accountschemename1choiceoption.key.get),
                schemeName = scheme.accountschemename1choiceoption.value.toString
              )),
            issuer = accountIdentification.Issr
          )
      },
      clearingSystem = settlementInstruction4.ClrSys.map(clearingSystem =>
        ClearingSystemIdentification(
          identificationType = ClearingSystemIdentificationType.withName(clearingSystem.clearingsystemidentification3choiceoption.key.get),
          identification = clearingSystem.clearingsystemidentification3choiceoption.value.toString
        )
      )
    )
  }
  def fromSettlementInstruction4(settlementInstruction4: inquiryClaimValueDateCorrectionNegativeAnswer.SettlementInstruction4): SettlementInformation = {
    SettlementInformation(
      settlementMethod = SettlementMethod.withName(settlementInstruction4.SttlmMtd.toString),
      settlementAccountIban = settlementInstruction4.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.key.getOrElse("") == SettlementAccountType.IBAN.toString =>
          Iban(cashAccount.Id.accountidentification4choicableoption.value.toString)
      },
      settlementAccountOtherIdentification = settlementInstruction4.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.value.isInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1] =>
          val accountIdentification = cashAccount.Id.accountidentification4choicableoption.value.asInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1]
          OtherAccountIdentification(
            identification = accountIdentification.Id,
            scheme = accountIdentification.SchmeNm.map(scheme =>
              OtherAccountIdentificationScheme(
                schemeType = OtherAccountIdentificationSchemeType.withName(scheme.accountschemename1choiceoption.key.get),
                schemeName = scheme.accountschemename1choiceoption.value.toString
              )),
            issuer = accountIdentification.Issr
          )
      },
      clearingSystem = settlementInstruction4.ClrSys.map(clearingSystem =>
        ClearingSystemIdentification(
          identificationType = ClearingSystemIdentificationType.withName(clearingSystem.clearingsystemidentification3choiceoption.key.get),
          identification = clearingSystem.clearingsystemidentification3choiceoption.value.toString
        )
      )
    )
  }
  def fromSettlementInstruction4(settlementInstruction4: requestStatusUpdate.SettlementInstruction4): SettlementInformation = {
    SettlementInformation(
      settlementMethod = SettlementMethod.withName(settlementInstruction4.SttlmMtd.toString),
      settlementAccountIban = settlementInstruction4.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.key.getOrElse("") == SettlementAccountType.IBAN.toString =>
          Iban(cashAccount.Id.accountidentification4choicableoption.value.toString)
      },
      settlementAccountOtherIdentification = settlementInstruction4.SttlmAcct.collect {
        case cashAccount if cashAccount.Id.accountidentification4choicableoption.value.isInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1] =>
          val accountIdentification = cashAccount.Id.accountidentification4choicableoption.value.asInstanceOf[paymentRecallNegativeAnswer.GenericAccountIdentification1]
          OtherAccountIdentification(
            identification = accountIdentification.Id,
            scheme = accountIdentification.SchmeNm.map(scheme =>
              OtherAccountIdentificationScheme(
                schemeType = OtherAccountIdentificationSchemeType.withName(scheme.accountschemename1choiceoption.key.get),
                schemeName = scheme.accountschemename1choiceoption.value.toString
              )),
            issuer = accountIdentification.Issr
          )
      },
      clearingSystem = settlementInstruction4.ClrSys.map(clearingSystem =>
        ClearingSystemIdentification(
          identificationType = ClearingSystemIdentificationType.withName(clearingSystem.clearingsystemidentification3choiceoption.key.get),
          identification = clearingSystem.clearingsystemidentification3choiceoption.value.toString
        )
      )
    )
  }


}


object SettlementAccountType extends Enumeration {
  type SettlementAccountType = Value

  implicit val decoder: Decoder[Value] = Decoder.decodeEnumeration(this)
  implicit val encoder: Encoder[Value] = Encoder.encodeEnumeration(this)

  val IBAN = Value
  val OTHER = Value("Othr")
}

object SettlementMethod extends Enumeration {
  type SettlementMethod = Value

  implicit val decoder: Decoder[Value] = Decoder.decodeEnumeration(this)
  implicit val encoder: Encoder[Value] = Encoder.encodeEnumeration(this)

  val CLEARING_SYSTEM = Value("CLRG") // Settlement is done through a payment clearing system
  val INSTRUCTED_AGENT = Value("INDA") // Settlement is done by the agent instructed to execute a payment instruction.
  val INSTRUCTING_AGENT = Value("INGA") //Settlement is done by the agent instructing and forwarding the payment to the next party in the payment chain.
}

case class OtherAccountIdentification(
                                       identification: String,
                                       scheme: Option[OtherAccountIdentificationScheme] = None,
                                       issuer: Option[String] = None
                                     )

case class OtherAccountIdentificationScheme(
                                             schemeType: OtherAccountIdentificationSchemeType,
                                             schemeName: String
                                           )

object OtherAccountIdentificationSchemeType extends Enumeration {
  type OtherAccountIdentificationSchemeType = Value

  implicit val decoder: Decoder[Value] = Decoder.decodeEnumeration(this)
  implicit val encoder: Encoder[Value] = Encoder.encodeEnumeration(this)

  val CODE = Value("Cd")
  val PROPRIETARY = Value("Prtry")
}

case class ClearingSystemIdentification(
                                         identificationType: ClearingSystemIdentificationType,
                                         identification: String
                                       )

object ClearingSystemIdentificationType extends Enumeration {
  type ClearingSystemIdentificationType = Value

  implicit val decoder: Decoder[Value] = Decoder.decodeEnumeration(this)
  implicit val encoder: Encoder[Value] = Encoder.encodeEnumeration(this)

  val CODE = Value("Cd")
  val PROPRIETARY = Value("Prtry")
}