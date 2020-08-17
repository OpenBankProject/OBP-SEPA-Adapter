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
import sepa.sct.generated.{creditTransfer, paymentReturn}

case class SettlementInformation(
                                  settlementMethod: SettlementMethod,
                                  settlementAccountIban: Option[Iban] = None,
                                  settlementAccountOtherIdentification: Option[OtherAccountIdentification] = None,
                                  clearingSystem: Option[ClearingSystemIdentification] = None
                                ) {
  def toJson: Json = this.asJson

  def toSettlementInformation13(implicit documentType: paymentReturn.Document): paymentReturn.SettlementInformation13 = paymentReturn.SettlementInformation13(
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