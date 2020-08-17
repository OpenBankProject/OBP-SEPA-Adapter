package model.jsonClasses

import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import model.jsonClasses.CategoryPurposeType.CategoryPurposeType
import model.jsonClasses.LocalInstrumentType.LocalInstrumentType
import model.jsonClasses.ServiceLevelCode.ServiceLevelCode
import scalaxb.DataRecord
import scalaxb.DataRecord.__StringXMLFormat
import sepa.sct.generated.{creditTransfer, paymentReturn}

case class PaymentTypeInformation(
                                   serviceLevelCode: Option[ServiceLevelCode],
                                   localInstrument: Option[LocalInstrument] = None,
                                   categoryPurpose: Option[CategoryPurpose] = None
                                 ) {
  def toJson: Json = this.asJson

  def toSettlementInformation13(implicit documentType: paymentReturn.Document): paymentReturn.PaymentTypeInformation22 = paymentReturn.PaymentTypeInformation22(
    SvcLvl = this.serviceLevelCode.map(serviceLevelCode => paymentReturn.ServiceLevel8Choice(
      DataRecord(<Cd></Cd>, serviceLevelCode.toString)
    )),
    LclInstrm = this.localInstrument.map(localInstrument =>
      paymentReturn.LocalInstrument2Choice(
        DataRecord(None, Some(localInstrument.localInstrumentType.toString), localInstrument.localInstrumentValue)
      )
    ),
    CtgyPurp = this.categoryPurpose.map(categoryPurpose =>
      paymentReturn.CategoryPurpose1Choice(
        DataRecord(None, Some(categoryPurpose.categoryPurposeType.toString), categoryPurpose.categoryPurposeValue)
      )
    )
  )
}

case object PaymentTypeInformation {
  def fromJson(jsonString: String): Option[PaymentTypeInformation] = decode[PaymentTypeInformation](jsonString).toOption

  def fromPaymentTypeInformation21(paymentTypeInformation21: creditTransfer.PaymentTypeInformation21able): PaymentTypeInformation = {
    PaymentTypeInformation(
      serviceLevelCode = paymentTypeInformation21.SvcLvl.map(serviceLevel =>
        ServiceLevelCode.withName(serviceLevel.servicelevel8choicableoption.value)
      ),
      localInstrument = paymentTypeInformation21.LclInstrm.map(localInstrument =>
        LocalInstrument(
          localInstrumentType = LocalInstrumentType.withName(localInstrument.localinstrument2choicableoption.key.get),
          localInstrumentValue = localInstrument.localinstrument2choicableoption.value.toString
        )
      ),
      categoryPurpose = paymentTypeInformation21.CtgyPurp.map(categoryPurpose =>
        CategoryPurpose(
          categoryPurposeType = CategoryPurposeType.withName(categoryPurpose.categorypurpose1choiceoption.key.get),
          categoryPurposeValue = categoryPurpose.categorypurpose1choiceoption.value.toString
        )
      )
    )
  }

}

object ServiceLevelCode extends Enumeration {
  type ServiceLevelCode = Value

  implicit val decoder: Decoder[Value] = Decoder.decodeEnumeration(this)
  implicit val encoder: Encoder[Value] = Encoder.encodeEnumeration(this)

  val SEPA = Value

}

case class LocalInstrument(
                            localInstrumentType: LocalInstrumentType,
                            localInstrumentValue: String
                          )

object LocalInstrumentType extends Enumeration {
  type LocalInstrumentType = Value

  implicit val decoder: Decoder[Value] = Decoder.decodeEnumeration(this)
  implicit val encoder: Encoder[Value] = Encoder.encodeEnumeration(this)

  val CODE = Value("Cd")
  val PROPRIETARY = Value("Prtry")
}

case class CategoryPurpose(
                            categoryPurposeType: CategoryPurposeType,
                            categoryPurposeValue: String
                          )

object CategoryPurposeType extends Enumeration {
  type CategoryPurposeType = Value

  implicit val decoder: Decoder[Value] = Decoder.decodeEnumeration(this)
  implicit val encoder: Encoder[Value] = Encoder.encodeEnumeration(this)

  val CODE = Value("Cd")
  val PROPRIETARY = Value("Prtry")
}