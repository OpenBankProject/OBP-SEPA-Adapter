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
import sepa.sct.generated._

case class PaymentTypeInformation(
                                   serviceLevelCode: Option[ServiceLevelCode],
                                   localInstrument: Option[LocalInstrument] = None,
                                   categoryPurpose: Option[CategoryPurpose] = None
                                 ) {
  def toJson: Json = this.asJson

  def toPaymentTypeInformation21(implicit documentType: creditTransfer.Document): creditTransfer.PaymentTypeInformation21 = creditTransfer.PaymentTypeInformation21(
    SvcLvl = this.serviceLevelCode.map(serviceLevelCode => creditTransfer.ServiceLevel8Choice(
      DataRecord(<Cd></Cd>, serviceLevelCode.toString)
    )),
    LclInstrm = this.localInstrument.map(localInstrument =>
      creditTransfer.LocalInstrument2Choice(
        DataRecord(None, Some(localInstrument.localInstrumentType.toString), localInstrument.localInstrumentValue)
      )
    ),
    CtgyPurp = this.categoryPurpose.map(categoryPurpose =>
      creditTransfer.CategoryPurpose1Choice(
        DataRecord(None, Some(categoryPurpose.categoryPurposeType.toString), categoryPurpose.categoryPurposeValue)
      )
    )
  )

  def toPaymentTypeInformation22(implicit documentType: paymentReturn.Document): paymentReturn.PaymentTypeInformation22 = paymentReturn.PaymentTypeInformation22(
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
  def toPaymentTypeInformation22(implicit documentType: paymentRecall.Document): paymentRecall.PaymentTypeInformation22 = paymentRecall.PaymentTypeInformation22(
    SvcLvl = this.serviceLevelCode.map(serviceLevelCode => paymentRecall.ServiceLevel8Choice(
      DataRecord(<Cd></Cd>, serviceLevelCode.toString)
    )),
    LclInstrm = this.localInstrument.map(localInstrument =>
      paymentRecall.LocalInstrument2Choice(
        DataRecord(None, Some(localInstrument.localInstrumentType.toString), localInstrument.localInstrumentValue)
      )
    ),
    CtgyPurp = this.categoryPurpose.map(categoryPurpose =>
      paymentRecall.CategoryPurpose1Choice(
        DataRecord(None, Some(categoryPurpose.categoryPurposeType.toString), categoryPurpose.categoryPurposeValue)
      )
    )
  )
  def toPaymentTypeInformation22(implicit documentType: paymentRecallNegativeAnswer.Document): paymentRecallNegativeAnswer.PaymentTypeInformation22 = paymentRecallNegativeAnswer.PaymentTypeInformation22(
    SvcLvl = this.serviceLevelCode.map(serviceLevelCode => paymentRecallNegativeAnswer.ServiceLevel8Choice(
      DataRecord(<Cd></Cd>, serviceLevelCode.toString)
    )),
    LclInstrm = this.localInstrument.map(localInstrument =>
      paymentRecallNegativeAnswer.LocalInstrument2Choice(
        DataRecord(None, Some(localInstrument.localInstrumentType.toString), localInstrument.localInstrumentValue)
      )
    ),
    CtgyPurp = this.categoryPurpose.map(categoryPurpose =>
      paymentRecallNegativeAnswer.CategoryPurpose1Choice(
        DataRecord(None, Some(categoryPurpose.categoryPurposeType.toString), categoryPurpose.categoryPurposeValue)
      )
    )
  )
 
  def toPaymentTypeInformation25(implicit documentType: inquiryClaimNonReceipt.Document): inquiryClaimNonReceipt.PaymentTypeInformation25 = inquiryClaimNonReceipt.PaymentTypeInformation25(
    SvcLvl = this.serviceLevelCode.map(serviceLevelCode => inquiryClaimNonReceipt.ServiceLevel8Choice(
      DataRecord(<Cd></Cd>, serviceLevelCode.toString)
    )),
    LclInstrm = this.localInstrument.map(localInstrument =>
      inquiryClaimNonReceipt.LocalInstrument2Choice(
        DataRecord(None, Some(localInstrument.localInstrumentType.toString), localInstrument.localInstrumentValue)
      )
    ),
    CtgyPurp = this.categoryPurpose.map(categoryPurpose =>
      inquiryClaimNonReceipt.CategoryPurpose1Choice(
        DataRecord(None, Some(categoryPurpose.categoryPurposeType.toString), categoryPurpose.categoryPurposeValue)
      )
    )
  )
  def toPaymentTypeInformation25(implicit documentType: inquiryClaimNonReceiptPositiveAnswer.Document): inquiryClaimNonReceiptPositiveAnswer.PaymentTypeInformation25 = inquiryClaimNonReceiptPositiveAnswer.PaymentTypeInformation25(
    SvcLvl = this.serviceLevelCode.map(serviceLevelCode => inquiryClaimNonReceiptPositiveAnswer.ServiceLevel8Choice(
      DataRecord(<Cd></Cd>, serviceLevelCode.toString)
    )),
    LclInstrm = this.localInstrument.map(localInstrument =>
      inquiryClaimNonReceiptPositiveAnswer.LocalInstrument2Choice(
        DataRecord(None, Some(localInstrument.localInstrumentType.toString), localInstrument.localInstrumentValue)
      )
    ),
    CtgyPurp = this.categoryPurpose.map(categoryPurpose =>
      inquiryClaimNonReceiptPositiveAnswer.CategoryPurpose1Choice(
        DataRecord(None, Some(categoryPurpose.categoryPurposeType.toString), categoryPurpose.categoryPurposeValue)
      )
    )
  )
  def toPaymentTypeInformation25(implicit documentType: inquiryClaimValueDateCorrection.Document): inquiryClaimValueDateCorrection.PaymentTypeInformation25 = inquiryClaimValueDateCorrection.PaymentTypeInformation25(
    SvcLvl = this.serviceLevelCode.map(serviceLevelCode => inquiryClaimValueDateCorrection.ServiceLevel8Choice(
      DataRecord(<Cd></Cd>, serviceLevelCode.toString)
    )),
    LclInstrm = this.localInstrument.map(localInstrument =>
      inquiryClaimValueDateCorrection.LocalInstrument2Choice(
        DataRecord(None, Some(localInstrument.localInstrumentType.toString), localInstrument.localInstrumentValue)
      )
    ),
    CtgyPurp = this.categoryPurpose.map(categoryPurpose =>
      inquiryClaimValueDateCorrection.CategoryPurpose1Choice(
        DataRecord(None, Some(categoryPurpose.categoryPurposeType.toString), categoryPurpose.categoryPurposeValue)
      )
    )
  )
  def toPaymentTypeInformation25(implicit documentType: inquiryClaimValueDateCorrectionPositiveAnswer.Document): inquiryClaimValueDateCorrectionPositiveAnswer.PaymentTypeInformation25 = inquiryClaimValueDateCorrectionPositiveAnswer.PaymentTypeInformation25(
    SvcLvl = this.serviceLevelCode.map(serviceLevelCode => inquiryClaimValueDateCorrectionPositiveAnswer.ServiceLevel8Choice(
      DataRecord(<Cd></Cd>, serviceLevelCode.toString)
    )),
    LclInstrm = this.localInstrument.map(localInstrument =>
      inquiryClaimValueDateCorrectionPositiveAnswer.LocalInstrument2Choice(
        DataRecord(None, Some(localInstrument.localInstrumentType.toString), localInstrument.localInstrumentValue)
      )
    ),
    CtgyPurp = this.categoryPurpose.map(categoryPurpose =>
      inquiryClaimValueDateCorrectionPositiveAnswer.CategoryPurpose1Choice(
        DataRecord(None, Some(categoryPurpose.categoryPurposeType.toString), categoryPurpose.categoryPurposeValue)
      )
    )
  )
  def toPaymentTypeInformation25(implicit documentType: inquiryClaimValueDateCorrectionNegativeAnswer.Document): inquiryClaimValueDateCorrectionNegativeAnswer.PaymentTypeInformation25 = inquiryClaimValueDateCorrectionNegativeAnswer.PaymentTypeInformation25(
    SvcLvl = this.serviceLevelCode.map(serviceLevelCode => inquiryClaimValueDateCorrectionNegativeAnswer.ServiceLevel8Choice(
      DataRecord(<Cd></Cd>, serviceLevelCode.toString)
    )),
    LclInstrm = this.localInstrument.map(localInstrument =>
      inquiryClaimValueDateCorrectionNegativeAnswer.LocalInstrument2Choice(
        DataRecord(None, Some(localInstrument.localInstrumentType.toString), localInstrument.localInstrumentValue)
      )
    ),
    CtgyPurp = this.categoryPurpose.map(categoryPurpose =>
      inquiryClaimValueDateCorrectionNegativeAnswer.CategoryPurpose1Choice(
        DataRecord(None, Some(categoryPurpose.categoryPurposeType.toString), categoryPurpose.categoryPurposeValue)
      )
    )
  )
  def toPaymentTypeInformation25(implicit documentType: requestStatusUpdate.Document): requestStatusUpdate.PaymentTypeInformation25 = requestStatusUpdate.PaymentTypeInformation25(
    SvcLvl = this.serviceLevelCode.map(serviceLevelCode => requestStatusUpdate.ServiceLevel8Choice(
      DataRecord(<Cd></Cd>, serviceLevelCode.toString)
    )),
    LclInstrm = this.localInstrument.map(localInstrument =>
      requestStatusUpdate.LocalInstrument2Choice(
        DataRecord(None, Some(localInstrument.localInstrumentType.toString), localInstrument.localInstrumentValue)
      )
    ),
    CtgyPurp = this.categoryPurpose.map(categoryPurpose =>
      requestStatusUpdate.CategoryPurpose1Choice(
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

  def fromPaymentTypeInformation22(paymentTypeInformation22: paymentReturn.PaymentTypeInformation22able): PaymentTypeInformation = {
    PaymentTypeInformation(
      serviceLevelCode = paymentTypeInformation22.SvcLvl.map(serviceLevel =>
        ServiceLevelCode.withName(serviceLevel.servicelevel8choicableoption.value)
      ),
      localInstrument = paymentTypeInformation22.LclInstrm.map(localInstrument =>
        LocalInstrument(
          localInstrumentType = LocalInstrumentType.withName(localInstrument.localinstrument2choiceoption.key.get),
          localInstrumentValue = localInstrument.localinstrument2choiceoption.value.toString
        )
      ),
      categoryPurpose = paymentTypeInformation22.CtgyPurp.map(categoryPurpose =>
        CategoryPurpose(
          categoryPurposeType = CategoryPurposeType.withName(categoryPurpose.categorypurpose1choiceoption.key.get),
          categoryPurposeValue = categoryPurpose.categorypurpose1choiceoption.value.toString
        )
      )
    )
  }
  def fromPaymentTypeInformation22(paymentTypeInformation22: paymentReject.PaymentTypeInformation22able): PaymentTypeInformation = {
    PaymentTypeInformation(
      serviceLevelCode = paymentTypeInformation22.SvcLvl.map(serviceLevel =>
        ServiceLevelCode.withName(serviceLevel.servicelevel8choicableoption.value)
      ),
      localInstrument = paymentTypeInformation22.LclInstrm.map(localInstrument =>
        LocalInstrument(
          localInstrumentType = LocalInstrumentType.withName(localInstrument.localinstrument2choiceoption.key.get),
          localInstrumentValue = localInstrument.localinstrument2choiceoption.value.toString
        )
      ),
      categoryPurpose = paymentTypeInformation22.CtgyPurp.map(categoryPurpose =>
        CategoryPurpose(
          categoryPurposeType = CategoryPurposeType.withName(categoryPurpose.categorypurpose1choiceoption.key.get),
          categoryPurposeValue = categoryPurpose.categorypurpose1choiceoption.value.toString
        )
      )
    )
  }
  def fromPaymentTypeInformation22(paymentTypeInformation22: paymentRecall.PaymentTypeInformation22able): PaymentTypeInformation = {
    PaymentTypeInformation(
      serviceLevelCode = paymentTypeInformation22.SvcLvl.map(serviceLevel =>
        ServiceLevelCode.withName(serviceLevel.servicelevel8choicableoption.value)
      ),
      localInstrument = paymentTypeInformation22.LclInstrm.map(localInstrument =>
        LocalInstrument(
          localInstrumentType = LocalInstrumentType.withName(localInstrument.localinstrument2choiceoption.key.get),
          localInstrumentValue = localInstrument.localinstrument2choiceoption.value.toString
        )
      ),
      categoryPurpose = paymentTypeInformation22.CtgyPurp.map(categoryPurpose =>
        CategoryPurpose(
          categoryPurposeType = CategoryPurposeType.withName(categoryPurpose.categorypurpose1choiceoption.key.get),
          categoryPurposeValue = categoryPurpose.categorypurpose1choiceoption.value.toString
        )
      )
    )
  }
  def fromPaymentTypeInformation22(paymentTypeInformation22: paymentRecallNegativeAnswer.PaymentTypeInformation22able): PaymentTypeInformation = {
    PaymentTypeInformation(
      serviceLevelCode = paymentTypeInformation22.SvcLvl.map(serviceLevel =>
        ServiceLevelCode.withName(serviceLevel.servicelevel8choicableoption.value)
      ),
      localInstrument = paymentTypeInformation22.LclInstrm.map(localInstrument =>
        LocalInstrument(
          localInstrumentType = LocalInstrumentType.withName(localInstrument.localinstrument2choiceoption.key.get),
          localInstrumentValue = localInstrument.localinstrument2choiceoption.value.toString
        )
      ),
      categoryPurpose = paymentTypeInformation22.CtgyPurp.map(categoryPurpose =>
        CategoryPurpose(
          categoryPurposeType = CategoryPurposeType.withName(categoryPurpose.categorypurpose1choiceoption.key.get),
          categoryPurposeValue = categoryPurpose.categorypurpose1choiceoption.value.toString
        )
      )
    )
  }
  
  def fromPaymentTypeInformation25(paymentTypeInformation25: inquiryClaimNonReceipt.PaymentTypeInformation25able): PaymentTypeInformation = {
    PaymentTypeInformation(
      serviceLevelCode = paymentTypeInformation25.SvcLvl.map(serviceLevel =>
        ServiceLevelCode.withName(serviceLevel.servicelevel8choiceoption.value)
      ),
      localInstrument = paymentTypeInformation25.LclInstrm.map(localInstrument =>
        LocalInstrument(
          localInstrumentType = LocalInstrumentType.withName(localInstrument.localinstrument2choiceoption.key.get),
          localInstrumentValue = localInstrument.localinstrument2choiceoption.value.toString
        )
      ),
      categoryPurpose = paymentTypeInformation25.CtgyPurp.map(categoryPurpose =>
        CategoryPurpose(
          categoryPurposeType = CategoryPurposeType.withName(categoryPurpose.categorypurpose1choiceoption.key.get),
          categoryPurposeValue = categoryPurpose.categorypurpose1choiceoption.value.toString
        )
      )
    )
  }
  def fromPaymentTypeInformation25(paymentTypeInformation25: inquiryClaimNonReceiptPositiveAnswer.PaymentTypeInformation25able): PaymentTypeInformation = {
    PaymentTypeInformation(
      serviceLevelCode = paymentTypeInformation25.SvcLvl.map(serviceLevel =>
        ServiceLevelCode.withName(serviceLevel.servicelevel8choiceoption.value)
      ),
      localInstrument = paymentTypeInformation25.LclInstrm.map(localInstrument =>
        LocalInstrument(
          localInstrumentType = LocalInstrumentType.withName(localInstrument.localinstrument2choiceoption.key.get),
          localInstrumentValue = localInstrument.localinstrument2choiceoption.value.toString
        )
      ),
      categoryPurpose = paymentTypeInformation25.CtgyPurp.map(categoryPurpose =>
        CategoryPurpose(
          categoryPurposeType = CategoryPurposeType.withName(categoryPurpose.categorypurpose1choiceoption.key.get),
          categoryPurposeValue = categoryPurpose.categorypurpose1choiceoption.value.toString
        )
      )
    )
  }
  def fromPaymentTypeInformation25(paymentTypeInformation25: inquiryClaimValueDateCorrection.PaymentTypeInformation25able): PaymentTypeInformation = {
    PaymentTypeInformation(
      serviceLevelCode = paymentTypeInformation25.SvcLvl.map(serviceLevel =>
        ServiceLevelCode.withName(serviceLevel.servicelevel8choiceoption.value)
      ),
      localInstrument = paymentTypeInformation25.LclInstrm.map(localInstrument =>
        LocalInstrument(
          localInstrumentType = LocalInstrumentType.withName(localInstrument.localinstrument2choiceoption.key.get),
          localInstrumentValue = localInstrument.localinstrument2choiceoption.value.toString
        )
      ),
      categoryPurpose = paymentTypeInformation25.CtgyPurp.map(categoryPurpose =>
        CategoryPurpose(
          categoryPurposeType = CategoryPurposeType.withName(categoryPurpose.categorypurpose1choiceoption.key.get),
          categoryPurposeValue = categoryPurpose.categorypurpose1choiceoption.value.toString
        )
      )
    )
  }
  def fromPaymentTypeInformation25(paymentTypeInformation25: inquiryClaimValueDateCorrectionPositiveAnswer.PaymentTypeInformation25able): PaymentTypeInformation = {
    PaymentTypeInformation(
      serviceLevelCode = paymentTypeInformation25.SvcLvl.map(serviceLevel =>
        ServiceLevelCode.withName(serviceLevel.servicelevel8choiceoption.value)
      ),
      localInstrument = paymentTypeInformation25.LclInstrm.map(localInstrument =>
        LocalInstrument(
          localInstrumentType = LocalInstrumentType.withName(localInstrument.localinstrument2choiceoption.key.get),
          localInstrumentValue = localInstrument.localinstrument2choiceoption.value.toString
        )
      ),
      categoryPurpose = paymentTypeInformation25.CtgyPurp.map(categoryPurpose =>
        CategoryPurpose(
          categoryPurposeType = CategoryPurposeType.withName(categoryPurpose.categorypurpose1choiceoption.key.get),
          categoryPurposeValue = categoryPurpose.categorypurpose1choiceoption.value.toString
        )
      )
    )
  }
  def fromPaymentTypeInformation25(paymentTypeInformation25: inquiryClaimValueDateCorrectionNegativeAnswer.PaymentTypeInformation25able): PaymentTypeInformation = {
    PaymentTypeInformation(
      serviceLevelCode = paymentTypeInformation25.SvcLvl.map(serviceLevel =>
        ServiceLevelCode.withName(serviceLevel.servicelevel8choiceoption.value)
      ),
      localInstrument = paymentTypeInformation25.LclInstrm.map(localInstrument =>
        LocalInstrument(
          localInstrumentType = LocalInstrumentType.withName(localInstrument.localinstrument2choiceoption.key.get),
          localInstrumentValue = localInstrument.localinstrument2choiceoption.value.toString
        )
      ),
      categoryPurpose = paymentTypeInformation25.CtgyPurp.map(categoryPurpose =>
        CategoryPurpose(
          categoryPurposeType = CategoryPurposeType.withName(categoryPurpose.categorypurpose1choiceoption.key.get),
          categoryPurposeValue = categoryPurpose.categorypurpose1choiceoption.value.toString
        )
      )
    )
  }
  def fromPaymentTypeInformation25(paymentTypeInformation25: requestStatusUpdate.PaymentTypeInformation25able): PaymentTypeInformation = {
    PaymentTypeInformation(
      serviceLevelCode = paymentTypeInformation25.SvcLvl.map(serviceLevel =>
        ServiceLevelCode.withName(serviceLevel.servicelevel8choiceoption.value)
      ),
      localInstrument = paymentTypeInformation25.LclInstrm.map(localInstrument =>
        LocalInstrument(
          localInstrumentType = LocalInstrumentType.withName(localInstrument.localinstrument2choiceoption.key.get),
          localInstrumentValue = localInstrument.localinstrument2choiceoption.value.toString
        )
      ),
      categoryPurpose = paymentTypeInformation25.CtgyPurp.map(categoryPurpose =>
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