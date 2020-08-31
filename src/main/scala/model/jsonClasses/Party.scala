package model.jsonClasses

import java.time.LocalDate

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import model.jsonClasses.OtherIdentificationSchemeType.OtherIdentificationSchemeType
import scalaxb.DataRecord
import scalaxb.DataRecord.__StringXMLFormat
import sepa.SepaUtil
import sepa.sct.generated._

case class Party(
                  name: Option[String] = None,
                  postalAddress: Option[PostalAddress] = None,
                  identification: Option[Either[PersonIdentification, OrganisationIdentification]] = None
                  // TODO : replace the Either by an "idendtificationType" field as in SettlementInformation : more comprehensible than Left/Right in Json
                ) {
  def toJson: Json = this.asJson


  def toPartyIdentification32CreditTransfer(implicit documentType: creditTransfer.Document): creditTransfer.PartyIdentification32 = creditTransfer.PartyIdentification32(
    Nm = this.name,
    PstlAdr = this.postalAddress.map(postalAddress => creditTransfer.PostalAddress6(Ctry = postalAddress.countryCode, AdrLine = postalAddress.addressLines)),
    Id = this.identification.map(_.fold(
      personIdentification => creditTransfer.Party6Choice(DataRecord(<PrvtId></PrvtId>,
        creditTransfer.PersonIdentification5(
          DtAndPlcOfBirth = personIdentification.dateAndPlaceOfBirth.map(dateAndPlaceOfBirth =>
            creditTransfer.DateAndPlaceOfBirth(
              BirthDt = SepaUtil.localDateToXMLGregorianCalendar(dateAndPlaceOfBirth.birthDate),
              PrvcOfBirth = dateAndPlaceOfBirth.provinceOfBirth,
              CityOfBirth = dateAndPlaceOfBirth.cityOfBirth,
              CtryOfBirth = dateAndPlaceOfBirth.countryCodeOfBirth
            )
          ),
          Othr = personIdentification.other.map(otherIdentification =>
            creditTransfer.GenericPersonIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                creditTransfer.PersonIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      )),
      organisationIdentification => creditTransfer.Party6Choice(DataRecord(<OrgId></OrgId>,
        creditTransfer.OrganisationIdentification4(
          BICOrBEI = organisationIdentification.bic,
          Othr = organisationIdentification.other.map(otherIdentification =>
            creditTransfer.GenericOrganisationIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                creditTransfer.OrganisationIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      ))
    ))
  )

  def toPartyIdentification32(implicit documentType: paymentReturn.Document): paymentReturn.PartyIdentification32 = paymentReturn.PartyIdentification32(
    Nm = this.name,
    PstlAdr = this.postalAddress.map(postalAddress => paymentReturn.PostalAddress6(Ctry = postalAddress.countryCode, AdrLine = postalAddress.addressLines)),
    Id = this.identification.map(_.fold(
      personIdentification => paymentReturn.Party6Choice(DataRecord(<PrvtId></PrvtId>,
        paymentReturn.PersonIdentification5(
          DtAndPlcOfBirth = personIdentification.dateAndPlaceOfBirth.map(dateAndPlaceOfBirth =>
            paymentReturn.DateAndPlaceOfBirth(
              BirthDt = SepaUtil.localDateToXMLGregorianCalendar(dateAndPlaceOfBirth.birthDate),
              PrvcOfBirth = dateAndPlaceOfBirth.provinceOfBirth,
              CityOfBirth = dateAndPlaceOfBirth.cityOfBirth,
              CtryOfBirth = dateAndPlaceOfBirth.countryCodeOfBirth
            )
          ),
          Othr = personIdentification.other.map(otherIdentification =>
            paymentReturn.GenericPersonIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                paymentReturn.PersonIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      )),
      organisationIdentification => paymentReturn.Party6Choice(DataRecord(<OrgId></OrgId>,
        paymentReturn.OrganisationIdentification4(
          BICOrBEI = organisationIdentification.bic,
          Othr = organisationIdentification.other.map(otherIdentification =>
            paymentReturn.GenericOrganisationIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                paymentReturn.OrganisationIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      ))
    ))
  )

  def toPartyIdentification32(implicit documentType: paymentReject.Document): paymentReject.PartyIdentification32 = paymentReject.PartyIdentification32(
    Nm = this.name,
    PstlAdr = this.postalAddress.map(postalAddress => paymentReject.PostalAddress6(Ctry = postalAddress.countryCode, AdrLine = postalAddress.addressLines)),
    Id = this.identification.map(_.fold(
      personIdentification => paymentReject.Party6Choice(DataRecord(<PrvtId></PrvtId>,
        paymentReject.PersonIdentification5(
          DtAndPlcOfBirth = personIdentification.dateAndPlaceOfBirth.map(dateAndPlaceOfBirth =>
            paymentReject.DateAndPlaceOfBirth(
              BirthDt = SepaUtil.localDateToXMLGregorianCalendar(dateAndPlaceOfBirth.birthDate),
              PrvcOfBirth = dateAndPlaceOfBirth.provinceOfBirth,
              CityOfBirth = dateAndPlaceOfBirth.cityOfBirth,
              CtryOfBirth = dateAndPlaceOfBirth.countryCodeOfBirth
            )
          ),
          Othr = personIdentification.other.map(otherIdentification =>
            paymentReject.GenericPersonIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                paymentReject.PersonIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      )),
      organisationIdentification => paymentReject.Party6Choice(DataRecord(<OrgId></OrgId>,
        paymentReject.OrganisationIdentification4(
          BICOrBEI = organisationIdentification.bic,
          Othr = organisationIdentification.other.map(otherIdentification =>
            paymentReject.GenericOrganisationIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                paymentReject.OrganisationIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      ))
    ))
  )

  def toPartyIdentification32(implicit documentType: paymentRecall.Document): paymentRecall.PartyIdentification32 = paymentRecall.PartyIdentification32(
    Nm = this.name,
    PstlAdr = this.postalAddress.map(postalAddress => paymentRecall.PostalAddress6(Ctry = postalAddress.countryCode, AdrLine = postalAddress.addressLines)),
    Id = this.identification.map(_.fold(
      personIdentification => paymentRecall.Party6Choice(DataRecord(<PrvtId></PrvtId>,
        paymentRecall.PersonIdentification5(
          DtAndPlcOfBirth = personIdentification.dateAndPlaceOfBirth.map(dateAndPlaceOfBirth =>
            paymentRecall.DateAndPlaceOfBirth(
              BirthDt = SepaUtil.localDateToXMLGregorianCalendar(dateAndPlaceOfBirth.birthDate),
              PrvcOfBirth = dateAndPlaceOfBirth.provinceOfBirth,
              CityOfBirth = dateAndPlaceOfBirth.cityOfBirth,
              CtryOfBirth = dateAndPlaceOfBirth.countryCodeOfBirth
            )
          ),
          Othr = personIdentification.other.map(otherIdentification =>
            paymentRecall.GenericPersonIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                paymentRecall.PersonIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      )),
      organisationIdentification => paymentRecall.Party6Choice(DataRecord(<OrgId></OrgId>,
        paymentRecall.OrganisationIdentification4(
          BICOrBEI = organisationIdentification.bic,
          Othr = organisationIdentification.other.map(otherIdentification =>
            paymentRecall.GenericOrganisationIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                paymentRecall.OrganisationIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      ))
    ))
  )

  def toPartyIdentification32(implicit documentType: paymentRecallNegativeAnswer.Document): paymentRecallNegativeAnswer.PartyIdentification32 = paymentRecallNegativeAnswer.PartyIdentification32(
    Nm = this.name,
    PstlAdr = this.postalAddress.map(postalAddress => paymentRecallNegativeAnswer.PostalAddress6(Ctry = postalAddress.countryCode, AdrLine = postalAddress.addressLines)),
    Id = this.identification.map(_.fold(
      personIdentification => paymentRecallNegativeAnswer.Party6Choice(DataRecord(<PrvtId></PrvtId>,
        paymentRecallNegativeAnswer.PersonIdentification5(
          DtAndPlcOfBirth = personIdentification.dateAndPlaceOfBirth.map(dateAndPlaceOfBirth =>
            paymentRecallNegativeAnswer.DateAndPlaceOfBirth(
              BirthDt = SepaUtil.localDateToXMLGregorianCalendar(dateAndPlaceOfBirth.birthDate),
              PrvcOfBirth = dateAndPlaceOfBirth.provinceOfBirth,
              CityOfBirth = dateAndPlaceOfBirth.cityOfBirth,
              CtryOfBirth = dateAndPlaceOfBirth.countryCodeOfBirth
            )
          ),
          Othr = personIdentification.other.map(otherIdentification =>
            paymentRecallNegativeAnswer.GenericPersonIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                paymentRecallNegativeAnswer.PersonIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      )),
      organisationIdentification => paymentRecallNegativeAnswer.Party6Choice(DataRecord(<OrgId></OrgId>,
        paymentRecallNegativeAnswer.OrganisationIdentification4(
          BICOrBEI = organisationIdentification.bic,
          Othr = organisationIdentification.other.map(otherIdentification =>
            paymentRecallNegativeAnswer.GenericOrganisationIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                paymentRecallNegativeAnswer.OrganisationIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      ))
    ))
  )


  def toParty35Choice(implicit documentType: inquiryClaimNonReceipt.Document): inquiryClaimNonReceipt.Party35Choice = inquiryClaimNonReceipt.Party35Choice(DataRecord(<Pty></Pty>, inquiryClaimNonReceipt.PartyIdentification125(
    Nm = this.name,
    PstlAdr = this.postalAddress.map(postalAddress => inquiryClaimNonReceipt.PostalAddress6(Ctry = postalAddress.countryCode, AdrLine = postalAddress.addressLines)),
    Id = this.identification.map(_.fold(
      personIdentification => inquiryClaimNonReceipt.Party34Choice(DataRecord(<PrvtId></PrvtId>,
        inquiryClaimNonReceipt.PersonIdentification13(
          DtAndPlcOfBirth = personIdentification.dateAndPlaceOfBirth.map(dateAndPlaceOfBirth =>
            inquiryClaimNonReceipt.DateAndPlaceOfBirth1(
              BirthDt = SepaUtil.localDateToXMLGregorianCalendar(dateAndPlaceOfBirth.birthDate),
              PrvcOfBirth = dateAndPlaceOfBirth.provinceOfBirth,
              CityOfBirth = dateAndPlaceOfBirth.cityOfBirth,
              CtryOfBirth = dateAndPlaceOfBirth.countryCodeOfBirth
            )
          ),
          Othr = personIdentification.other.map(otherIdentification =>
            inquiryClaimNonReceipt.GenericPersonIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                inquiryClaimNonReceipt.PersonIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      )),
      organisationIdentification => inquiryClaimNonReceipt.Party34Choice(DataRecord(<OrgId></OrgId>,
        inquiryClaimNonReceipt.OrganisationIdentification8(
          AnyBIC = organisationIdentification.bic,
          Othr = organisationIdentification.other.map(otherIdentification =>
            inquiryClaimNonReceipt.GenericOrganisationIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                inquiryClaimNonReceipt.OrganisationIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      ))
    ))
  )))

  def toParty35Choice(implicit documentType: inquiryClaimNonReceiptPositiveAnswer.Document): inquiryClaimNonReceiptPositiveAnswer.Party35Choice = inquiryClaimNonReceiptPositiveAnswer.Party35Choice(DataRecord(<Pty></Pty>, inquiryClaimNonReceiptPositiveAnswer.PartyIdentification125(
    Nm = this.name,
    PstlAdr = this.postalAddress.map(postalAddress => inquiryClaimNonReceiptPositiveAnswer.PostalAddress6(Ctry = postalAddress.countryCode, AdrLine = postalAddress.addressLines)),
    Id = this.identification.map(_.fold(
      personIdentification => inquiryClaimNonReceiptPositiveAnswer.Party34Choice(DataRecord(<PrvtId></PrvtId>,
        inquiryClaimNonReceiptPositiveAnswer.PersonIdentification13(
          DtAndPlcOfBirth = personIdentification.dateAndPlaceOfBirth.map(dateAndPlaceOfBirth =>
            inquiryClaimNonReceiptPositiveAnswer.DateAndPlaceOfBirth1(
              BirthDt = SepaUtil.localDateToXMLGregorianCalendar(dateAndPlaceOfBirth.birthDate),
              PrvcOfBirth = dateAndPlaceOfBirth.provinceOfBirth,
              CityOfBirth = dateAndPlaceOfBirth.cityOfBirth,
              CtryOfBirth = dateAndPlaceOfBirth.countryCodeOfBirth
            )
          ),
          Othr = personIdentification.other.map(otherIdentification =>
            inquiryClaimNonReceiptPositiveAnswer.GenericPersonIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                inquiryClaimNonReceiptPositiveAnswer.PersonIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      )),
      organisationIdentification => inquiryClaimNonReceiptPositiveAnswer.Party34Choice(DataRecord(<OrgId></OrgId>,
        inquiryClaimNonReceiptPositiveAnswer.OrganisationIdentification8(
          AnyBIC = organisationIdentification.bic,
          Othr = organisationIdentification.other.map(otherIdentification =>
            inquiryClaimNonReceiptPositiveAnswer.GenericOrganisationIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                inquiryClaimNonReceiptPositiveAnswer.OrganisationIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      ))
    ))
  )))

  def toParty35Choice(implicit documentType: inquiryClaimValueDateCorrection.Document): inquiryClaimValueDateCorrection.Party35Choice = inquiryClaimValueDateCorrection.Party35Choice(DataRecord(<Pty></Pty>, inquiryClaimValueDateCorrection.PartyIdentification125(
    Nm = this.name,
    PstlAdr = this.postalAddress.map(postalAddress => inquiryClaimValueDateCorrection.PostalAddress6(Ctry = postalAddress.countryCode, AdrLine = postalAddress.addressLines)),
    Id = this.identification.map(_.fold(
      personIdentification => inquiryClaimValueDateCorrection.Party34Choice(DataRecord(<PrvtId></PrvtId>,
        inquiryClaimValueDateCorrection.PersonIdentification13(
          DtAndPlcOfBirth = personIdentification.dateAndPlaceOfBirth.map(dateAndPlaceOfBirth =>
            inquiryClaimValueDateCorrection.DateAndPlaceOfBirth1(
              BirthDt = SepaUtil.localDateToXMLGregorianCalendar(dateAndPlaceOfBirth.birthDate),
              PrvcOfBirth = dateAndPlaceOfBirth.provinceOfBirth,
              CityOfBirth = dateAndPlaceOfBirth.cityOfBirth,
              CtryOfBirth = dateAndPlaceOfBirth.countryCodeOfBirth
            )
          ),
          Othr = personIdentification.other.map(otherIdentification =>
            inquiryClaimValueDateCorrection.GenericPersonIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                inquiryClaimValueDateCorrection.PersonIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      )),
      organisationIdentification => inquiryClaimValueDateCorrection.Party34Choice(DataRecord(<OrgId></OrgId>,
        inquiryClaimValueDateCorrection.OrganisationIdentification8(
          AnyBIC = organisationIdentification.bic,
          Othr = organisationIdentification.other.map(otherIdentification =>
            inquiryClaimValueDateCorrection.GenericOrganisationIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                inquiryClaimValueDateCorrection.OrganisationIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      ))
    ))
  )))

  def toParty35Choice(implicit documentType: inquiryClaimValueDateCorrectionPositiveAnswer.Document): inquiryClaimValueDateCorrectionPositiveAnswer.Party35Choice = inquiryClaimValueDateCorrectionPositiveAnswer.Party35Choice(DataRecord(<Pty></Pty>, inquiryClaimValueDateCorrectionPositiveAnswer.PartyIdentification125(
    Nm = this.name,
    PstlAdr = this.postalAddress.map(postalAddress => inquiryClaimValueDateCorrectionPositiveAnswer.PostalAddress6(Ctry = postalAddress.countryCode, AdrLine = postalAddress.addressLines)),
    Id = this.identification.map(_.fold(
      personIdentification => inquiryClaimValueDateCorrectionPositiveAnswer.Party34Choice(DataRecord(<PrvtId></PrvtId>,
        inquiryClaimValueDateCorrectionPositiveAnswer.PersonIdentification13(
          DtAndPlcOfBirth = personIdentification.dateAndPlaceOfBirth.map(dateAndPlaceOfBirth =>
            inquiryClaimValueDateCorrectionPositiveAnswer.DateAndPlaceOfBirth1(
              BirthDt = SepaUtil.localDateToXMLGregorianCalendar(dateAndPlaceOfBirth.birthDate),
              PrvcOfBirth = dateAndPlaceOfBirth.provinceOfBirth,
              CityOfBirth = dateAndPlaceOfBirth.cityOfBirth,
              CtryOfBirth = dateAndPlaceOfBirth.countryCodeOfBirth
            )
          ),
          Othr = personIdentification.other.map(otherIdentification =>
            inquiryClaimValueDateCorrectionPositiveAnswer.GenericPersonIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                inquiryClaimValueDateCorrectionPositiveAnswer.PersonIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      )),
      organisationIdentification => inquiryClaimValueDateCorrectionPositiveAnswer.Party34Choice(DataRecord(<OrgId></OrgId>,
        inquiryClaimValueDateCorrectionPositiveAnswer.OrganisationIdentification8(
          AnyBIC = organisationIdentification.bic,
          Othr = organisationIdentification.other.map(otherIdentification =>
            inquiryClaimValueDateCorrectionPositiveAnswer.GenericOrganisationIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                inquiryClaimValueDateCorrectionPositiveAnswer.OrganisationIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      ))
    ))
  )))

  def toParty35Choice(implicit documentType: inquiryClaimValueDateCorrectionNegativeAnswer.Document): inquiryClaimValueDateCorrectionNegativeAnswer.Party35Choice = inquiryClaimValueDateCorrectionNegativeAnswer.Party35Choice(DataRecord(<Pty></Pty>, inquiryClaimValueDateCorrectionNegativeAnswer.PartyIdentification125(
    Nm = this.name,
    PstlAdr = this.postalAddress.map(postalAddress => inquiryClaimValueDateCorrectionNegativeAnswer.PostalAddress6(Ctry = postalAddress.countryCode, AdrLine = postalAddress.addressLines)),
    Id = this.identification.map(_.fold(
      personIdentification => inquiryClaimValueDateCorrectionNegativeAnswer.Party34Choice(DataRecord(<PrvtId></PrvtId>,
        inquiryClaimValueDateCorrectionNegativeAnswer.PersonIdentification13(
          DtAndPlcOfBirth = personIdentification.dateAndPlaceOfBirth.map(dateAndPlaceOfBirth =>
            inquiryClaimValueDateCorrectionNegativeAnswer.DateAndPlaceOfBirth1(
              BirthDt = SepaUtil.localDateToXMLGregorianCalendar(dateAndPlaceOfBirth.birthDate),
              PrvcOfBirth = dateAndPlaceOfBirth.provinceOfBirth,
              CityOfBirth = dateAndPlaceOfBirth.cityOfBirth,
              CtryOfBirth = dateAndPlaceOfBirth.countryCodeOfBirth
            )
          ),
          Othr = personIdentification.other.map(otherIdentification =>
            inquiryClaimValueDateCorrectionNegativeAnswer.GenericPersonIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                inquiryClaimValueDateCorrectionNegativeAnswer.PersonIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      )),
      organisationIdentification => inquiryClaimValueDateCorrectionNegativeAnswer.Party34Choice(DataRecord(<OrgId></OrgId>,
        inquiryClaimValueDateCorrectionNegativeAnswer.OrganisationIdentification8(
          AnyBIC = organisationIdentification.bic,
          Othr = organisationIdentification.other.map(otherIdentification =>
            inquiryClaimValueDateCorrectionNegativeAnswer.GenericOrganisationIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                inquiryClaimValueDateCorrectionNegativeAnswer.OrganisationIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      ))
    ))
  )))


  def toPartyIdentification43(implicit documentType: requestStatusUpdate.Document): requestStatusUpdate.PartyIdentification43 = requestStatusUpdate.PartyIdentification43(
    Nm = this.name,
    PstlAdr = this.postalAddress.map(postalAddress => requestStatusUpdate.PostalAddress6(Ctry = postalAddress.countryCode, AdrLine = postalAddress.addressLines)),
    Id = this.identification.map(_.fold(
      personIdentification => requestStatusUpdate.Party11Choice(DataRecord(<PrvtId></PrvtId>,
        requestStatusUpdate.PersonIdentification5(
          DtAndPlcOfBirth = personIdentification.dateAndPlaceOfBirth.map(dateAndPlaceOfBirth =>
            requestStatusUpdate.DateAndPlaceOfBirth(
              BirthDt = SepaUtil.localDateToXMLGregorianCalendar(dateAndPlaceOfBirth.birthDate),
              PrvcOfBirth = dateAndPlaceOfBirth.provinceOfBirth,
              CityOfBirth = dateAndPlaceOfBirth.cityOfBirth,
              CtryOfBirth = dateAndPlaceOfBirth.countryCodeOfBirth
            )
          ),
          Othr = personIdentification.other.map(otherIdentification =>
            requestStatusUpdate.GenericPersonIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                requestStatusUpdate.PersonIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      )),
      organisationIdentification => requestStatusUpdate.Party11Choice(DataRecord(<OrgId></OrgId>,
        requestStatusUpdate.OrganisationIdentification8(
          AnyBIC = organisationIdentification.bic,
          Othr = organisationIdentification.other.map(otherIdentification =>
            requestStatusUpdate.GenericOrganisationIdentification1(
              Id = otherIdentification.identification,
              SchmeNm = otherIdentification.scheme.map(scheme =>
                requestStatusUpdate.OrganisationIdentificationSchemeName1Choice(DataRecord(None, Some(scheme.schemeType.toString), scheme.schemeName))
              ),
              Issr = otherIdentification.issuer
            )
          ).toSeq
        )
      ))
    ))
  )

}

case object Party {
  def fromJson(jsonString: String): Option[Party] = decode[Party](jsonString).toOption


  def fromPartyIdentification32(partyIdentification32: creditTransfer.PartyIdentification32able): Party = {
    Party(
      name = partyIdentification32.Nm,
      postalAddress = partyIdentification32.PstlAdr.map(postalAddress =>
        PostalAddress(
          countryCode = postalAddress.Ctry,
          addressLines = postalAddress.AdrLine
        )
      ),
      identification = partyIdentification32.Id.map(identification =>
        identification.party6choicableoption.value match {
          case personIdentification: creditTransfer.PersonIdentification5able =>
            Left(PersonIdentification(
              dateAndPlaceOfBirth = personIdentification.DtAndPlcOfBirth.map(dateAndPlaceOfBirth =>
                DateAndPlaceOfBirth(
                  birthDate = dateAndPlaceOfBirth.BirthDt.toGregorianCalendar.toZonedDateTime.toLocalDate,
                  provinceOfBirth = dateAndPlaceOfBirth.PrvcOfBirth,
                  cityOfBirth = dateAndPlaceOfBirth.CityOfBirth,
                  countryCodeOfBirth = dateAndPlaceOfBirth.CtryOfBirth
                )
              ),
              other = personIdentification.Othr.headOption.map(otherIdentication =>
                OtherIdentification(
                  identification = otherIdentication.Id,
                  scheme = otherIdentication.SchmeNm.map(scheme =>
                    OtherIdentificationScheme(
                      schemeType = OtherIdentificationSchemeType.withName(scheme.personidentificationschemename1choiceoption.key.get),
                      schemeName = scheme.personidentificationschemename1choiceoption.value.toString
                    )
                  ),
                  issuer = otherIdentication.Issr
                )
              )
            ))
          case organisationIdentification: creditTransfer.OrganisationIdentification4able =>
            Right(OrganisationIdentification(
              bic = organisationIdentification.BICOrBEI,
              other = organisationIdentification.Othr.headOption.map(otherIdentication =>
                OtherIdentification(
                  identification = otherIdentication.Id,
                  scheme = otherIdentication.SchmeNm.map(scheme =>
                    OtherIdentificationScheme(
                      schemeType = OtherIdentificationSchemeType.withName(scheme.organisationidentificationschemename1choiceoption.key.get),
                      schemeName = scheme.organisationidentificationschemename1choiceoption.value.toString
                    )
                  ),
                  issuer = otherIdentication.Issr
                )
              )
            ))
        }
      )
    )
  }

  def fromPartyIdentification32(partyIdentification32: paymentReturn.PartyIdentification32able): Party = {
    Party(
      name = partyIdentification32.Nm,
      postalAddress = partyIdentification32.PstlAdr.map(postalAddress =>
        PostalAddress(
          countryCode = postalAddress.Ctry,
          addressLines = postalAddress.AdrLine
        )
      ),
      identification = partyIdentification32.Id.map(identification =>
        identification.party6choicableoption.value match {
          case personIdentification: paymentReturn.PersonIdentification5able =>
            Left(PersonIdentification(
              dateAndPlaceOfBirth = personIdentification.DtAndPlcOfBirth.map(dateAndPlaceOfBirth =>
                DateAndPlaceOfBirth(
                  birthDate = dateAndPlaceOfBirth.BirthDt.toGregorianCalendar.toZonedDateTime.toLocalDate,
                  provinceOfBirth = dateAndPlaceOfBirth.PrvcOfBirth,
                  cityOfBirth = dateAndPlaceOfBirth.CityOfBirth,
                  countryCodeOfBirth = dateAndPlaceOfBirth.CtryOfBirth
                )
              ),
              other = personIdentification.Othr.headOption.map(otherIdentication =>
                OtherIdentification(
                  identification = otherIdentication.Id,
                  scheme = otherIdentication.SchmeNm.map(scheme =>
                    OtherIdentificationScheme(
                      schemeType = OtherIdentificationSchemeType.withName(scheme.personidentificationschemename1choiceoption.key.get),
                      schemeName = scheme.personidentificationschemename1choiceoption.value.toString
                    )
                  ),
                  issuer = otherIdentication.Issr
                )
              )
            ))
          case organisationIdentification: paymentReturn.OrganisationIdentification4able =>
            Right(OrganisationIdentification(
              bic = organisationIdentification.BICOrBEI,
              other = organisationIdentification.Othr.headOption.map(otherIdentication =>
                OtherIdentification(
                  identification = otherIdentication.Id,
                  scheme = otherIdentication.SchmeNm.map(scheme =>
                    OtherIdentificationScheme(
                      schemeType = OtherIdentificationSchemeType.withName(scheme.organisationidentificationschemename1choiceoption.key.get),
                      schemeName = scheme.organisationidentificationschemename1choiceoption.value.toString
                    )
                  ),
                  issuer = otherIdentication.Issr
                )
              )
            ))
        }
      )
    )
  }

  def fromPartyIdentification32(partyIdentification32: paymentReject.PartyIdentification32able): Party = {
    Party(
      name = partyIdentification32.Nm,
      postalAddress = partyIdentification32.PstlAdr.map(postalAddress =>
        PostalAddress(
          countryCode = postalAddress.Ctry,
          addressLines = postalAddress.AdrLine
        )
      ),
      identification = partyIdentification32.Id.map(identification =>
        identification.party6choicableoption.value match {
          case personIdentification: paymentReject.PersonIdentification5able =>
            Left(PersonIdentification(
              dateAndPlaceOfBirth = personIdentification.DtAndPlcOfBirth.map(dateAndPlaceOfBirth =>
                DateAndPlaceOfBirth(
                  birthDate = dateAndPlaceOfBirth.BirthDt.toGregorianCalendar.toZonedDateTime.toLocalDate,
                  provinceOfBirth = dateAndPlaceOfBirth.PrvcOfBirth,
                  cityOfBirth = dateAndPlaceOfBirth.CityOfBirth,
                  countryCodeOfBirth = dateAndPlaceOfBirth.CtryOfBirth
                )
              ),
              other = personIdentification.Othr.headOption.map(otherIdentication =>
                OtherIdentification(
                  identification = otherIdentication.Id,
                  scheme = otherIdentication.SchmeNm.map(scheme =>
                    OtherIdentificationScheme(
                      schemeType = OtherIdentificationSchemeType.withName(scheme.personidentificationschemename1choiceoption.key.get),
                      schemeName = scheme.personidentificationschemename1choiceoption.value.toString
                    )
                  ),
                  issuer = otherIdentication.Issr
                )
              )
            ))
          case organisationIdentification: paymentReject.OrganisationIdentification4able =>
            Right(OrganisationIdentification(
              bic = organisationIdentification.BICOrBEI,
              other = organisationIdentification.Othr.headOption.map(otherIdentication =>
                OtherIdentification(
                  identification = otherIdentication.Id,
                  scheme = otherIdentication.SchmeNm.map(scheme =>
                    OtherIdentificationScheme(
                      schemeType = OtherIdentificationSchemeType.withName(scheme.organisationidentificationschemename1choiceoption.key.get),
                      schemeName = scheme.organisationidentificationschemename1choiceoption.value.toString
                    )
                  ),
                  issuer = otherIdentication.Issr
                )
              )
            ))
        }
      )
    )
  }

  def fromPartyIdentification32(partyIdentification32: paymentRecall.PartyIdentification32able): Party = {
    Party(
      name = partyIdentification32.Nm,
      postalAddress = partyIdentification32.PstlAdr.map(postalAddress =>
        PostalAddress(
          countryCode = postalAddress.Ctry,
          addressLines = postalAddress.AdrLine
        )
      ),
      identification = partyIdentification32.Id.map(identification =>
        identification.party6choicableoption.value match {
          case personIdentification: paymentRecall.PersonIdentification5able =>
            Left(PersonIdentification(
              dateAndPlaceOfBirth = personIdentification.DtAndPlcOfBirth.map(dateAndPlaceOfBirth =>
                DateAndPlaceOfBirth(
                  birthDate = dateAndPlaceOfBirth.BirthDt.toGregorianCalendar.toZonedDateTime.toLocalDate,
                  provinceOfBirth = dateAndPlaceOfBirth.PrvcOfBirth,
                  cityOfBirth = dateAndPlaceOfBirth.CityOfBirth,
                  countryCodeOfBirth = dateAndPlaceOfBirth.CtryOfBirth
                )
              ),
              other = personIdentification.Othr.headOption.map(otherIdentication =>
                OtherIdentification(
                  identification = otherIdentication.Id,
                  scheme = otherIdentication.SchmeNm.map(scheme =>
                    OtherIdentificationScheme(
                      schemeType = OtherIdentificationSchemeType.withName(scheme.personidentificationschemename1choiceoption.key.get),
                      schemeName = scheme.personidentificationschemename1choiceoption.value.toString
                    )
                  ),
                  issuer = otherIdentication.Issr
                )
              )
            ))
          case organisationIdentification: paymentRecall.OrganisationIdentification4able =>
            Right(OrganisationIdentification(
              bic = organisationIdentification.BICOrBEI,
              other = organisationIdentification.Othr.headOption.map(otherIdentication =>
                OtherIdentification(
                  identification = otherIdentication.Id,
                  scheme = otherIdentication.SchmeNm.map(scheme =>
                    OtherIdentificationScheme(
                      schemeType = OtherIdentificationSchemeType.withName(scheme.organisationidentificationschemename1choiceoption.key.get),
                      schemeName = scheme.organisationidentificationschemename1choiceoption.value.toString
                    )
                  ),
                  issuer = otherIdentication.Issr
                )
              )
            ))
        }
      )
    )
  }

  def fromPartyIdentification32(partyIdentification32: paymentRecallNegativeAnswer.PartyIdentification32able): Party = {
    Party(
      name = partyIdentification32.Nm,
      postalAddress = partyIdentification32.PstlAdr.map(postalAddress =>
        PostalAddress(
          countryCode = postalAddress.Ctry,
          addressLines = postalAddress.AdrLine
        )
      ),
      identification = partyIdentification32.Id.map(identification =>
        identification.party6choicableoption.value match {
          case personIdentification: paymentRecallNegativeAnswer.PersonIdentification5able =>
            Left(PersonIdentification(
              dateAndPlaceOfBirth = personIdentification.DtAndPlcOfBirth.map(dateAndPlaceOfBirth =>
                DateAndPlaceOfBirth(
                  birthDate = dateAndPlaceOfBirth.BirthDt.toGregorianCalendar.toZonedDateTime.toLocalDate,
                  provinceOfBirth = dateAndPlaceOfBirth.PrvcOfBirth,
                  cityOfBirth = dateAndPlaceOfBirth.CityOfBirth,
                  countryCodeOfBirth = dateAndPlaceOfBirth.CtryOfBirth
                )
              ),
              other = personIdentification.Othr.headOption.map(otherIdentication =>
                OtherIdentification(
                  identification = otherIdentication.Id,
                  scheme = otherIdentication.SchmeNm.map(scheme =>
                    OtherIdentificationScheme(
                      schemeType = OtherIdentificationSchemeType.withName(scheme.personidentificationschemename1choiceoption.key.get),
                      schemeName = scheme.personidentificationschemename1choiceoption.value.toString
                    )
                  ),
                  issuer = otherIdentication.Issr
                )
              )
            ))
          case organisationIdentification: paymentRecallNegativeAnswer.OrganisationIdentification4able =>
            Right(OrganisationIdentification(
              bic = organisationIdentification.BICOrBEI,
              other = organisationIdentification.Othr.headOption.map(otherIdentication =>
                OtherIdentification(
                  identification = otherIdentication.Id,
                  scheme = otherIdentication.SchmeNm.map(scheme =>
                    OtherIdentificationScheme(
                      schemeType = OtherIdentificationSchemeType.withName(scheme.organisationidentificationschemename1choiceoption.key.get),
                      schemeName = scheme.organisationidentificationschemename1choiceoption.value.toString
                    )
                  ),
                  issuer = otherIdentication.Issr
                )
              )
            ))
        }
      )
    )
  }


  def fromParty35Choice(party35choicableoption: inquiryClaimNonReceipt.Party35ChoicableOption): Party = {
    party35choicableoption match {
      case party: inquiryClaimNonReceipt.PartyIdentification125able => Party(
        name = party.Nm,
        postalAddress = party.PstlAdr.map(postalAddress =>
          PostalAddress(
            countryCode = postalAddress.Ctry,
            addressLines = postalAddress.AdrLine
          )
        ),
        identification = party.Id.map(identification =>
          identification.party34choicableoption.value match {
            case personIdentification: inquiryClaimNonReceipt.PersonIdentification13able =>
              Left(PersonIdentification(
                dateAndPlaceOfBirth = personIdentification.DtAndPlcOfBirth.map(dateAndPlaceOfBirth =>
                  DateAndPlaceOfBirth(
                    birthDate = dateAndPlaceOfBirth.BirthDt.toGregorianCalendar.toZonedDateTime.toLocalDate,
                    provinceOfBirth = dateAndPlaceOfBirth.PrvcOfBirth,
                    cityOfBirth = dateAndPlaceOfBirth.CityOfBirth,
                    countryCodeOfBirth = dateAndPlaceOfBirth.CtryOfBirth
                  )
                ),
                other = personIdentification.Othr.headOption.map(otherIdentication =>
                  OtherIdentification(
                    identification = otherIdentication.Id,
                    scheme = otherIdentication.SchmeNm.map(scheme =>
                      OtherIdentificationScheme(
                        schemeType = OtherIdentificationSchemeType.withName(scheme.personidentificationschemename1choiceoption.key.get),
                        schemeName = scheme.personidentificationschemename1choiceoption.value.toString
                      )
                    ),
                    issuer = otherIdentication.Issr
                  )
                )
              ))
            case organisationIdentification: inquiryClaimNonReceipt.OrganisationIdentification8able =>
              Right(OrganisationIdentification(
                bic = organisationIdentification.AnyBIC,
                other = organisationIdentification.Othr.headOption.map(otherIdentication =>
                  OtherIdentification(
                    identification = otherIdentication.Id,
                    scheme = otherIdentication.SchmeNm.map(scheme =>
                      OtherIdentificationScheme(
                        schemeType = OtherIdentificationSchemeType.withName(scheme.organisationidentificationschemename1choiceoption.key.get),
                        schemeName = scheme.organisationidentificationschemename1choiceoption.value.toString
                      )
                    ),
                    issuer = otherIdentication.Issr
                  )
                )
              ))
          }
        )
      )
    }
  }

  def fromParty35Choice(party35choicableoption: inquiryClaimNonReceiptPositiveAnswer.Party35ChoicableOption): Party = {
    party35choicableoption match {
      case party: inquiryClaimNonReceiptPositiveAnswer.PartyIdentification125able => Party(
        name = party.Nm,
        postalAddress = party.PstlAdr.map(postalAddress =>
          PostalAddress(
            countryCode = postalAddress.Ctry,
            addressLines = postalAddress.AdrLine
          )
        ),
        identification = party.Id.map(identification =>
          identification.party34choicableoption.value match {
            case personIdentification: inquiryClaimNonReceiptPositiveAnswer.PersonIdentification13able =>
              Left(PersonIdentification(
                dateAndPlaceOfBirth = personIdentification.DtAndPlcOfBirth.map(dateAndPlaceOfBirth =>
                  DateAndPlaceOfBirth(
                    birthDate = dateAndPlaceOfBirth.BirthDt.toGregorianCalendar.toZonedDateTime.toLocalDate,
                    provinceOfBirth = dateAndPlaceOfBirth.PrvcOfBirth,
                    cityOfBirth = dateAndPlaceOfBirth.CityOfBirth,
                    countryCodeOfBirth = dateAndPlaceOfBirth.CtryOfBirth
                  )
                ),
                other = personIdentification.Othr.headOption.map(otherIdentication =>
                  OtherIdentification(
                    identification = otherIdentication.Id,
                    scheme = otherIdentication.SchmeNm.map(scheme =>
                      OtherIdentificationScheme(
                        schemeType = OtherIdentificationSchemeType.withName(scheme.personidentificationschemename1choiceoption.key.get),
                        schemeName = scheme.personidentificationschemename1choiceoption.value.toString
                      )
                    ),
                    issuer = otherIdentication.Issr
                  )
                )
              ))
            case organisationIdentification: inquiryClaimNonReceiptPositiveAnswer.OrganisationIdentification8able =>
              Right(OrganisationIdentification(
                bic = organisationIdentification.AnyBIC,
                other = organisationIdentification.Othr.headOption.map(otherIdentication =>
                  OtherIdentification(
                    identification = otherIdentication.Id,
                    scheme = otherIdentication.SchmeNm.map(scheme =>
                      OtherIdentificationScheme(
                        schemeType = OtherIdentificationSchemeType.withName(scheme.organisationidentificationschemename1choiceoption.key.get),
                        schemeName = scheme.organisationidentificationschemename1choiceoption.value.toString
                      )
                    ),
                    issuer = otherIdentication.Issr
                  )
                )
              ))
          }
        )
      )
    }
  }

  def fromParty35Choice(party35choicableoption: inquiryClaimValueDateCorrection.Party35ChoicableOption): Party = {
    party35choicableoption match {
      case party: inquiryClaimValueDateCorrection.PartyIdentification125able => Party(
        name = party.Nm,
        postalAddress = party.PstlAdr.map(postalAddress =>
          PostalAddress(
            countryCode = postalAddress.Ctry,
            addressLines = postalAddress.AdrLine
          )
        ),
        identification = party.Id.map(identification =>
          identification.party34choicableoption.value match {
            case personIdentification: inquiryClaimValueDateCorrection.PersonIdentification13able =>
              Left(PersonIdentification(
                dateAndPlaceOfBirth = personIdentification.DtAndPlcOfBirth.map(dateAndPlaceOfBirth =>
                  DateAndPlaceOfBirth(
                    birthDate = dateAndPlaceOfBirth.BirthDt.toGregorianCalendar.toZonedDateTime.toLocalDate,
                    provinceOfBirth = dateAndPlaceOfBirth.PrvcOfBirth,
                    cityOfBirth = dateAndPlaceOfBirth.CityOfBirth,
                    countryCodeOfBirth = dateAndPlaceOfBirth.CtryOfBirth
                  )
                ),
                other = personIdentification.Othr.headOption.map(otherIdentication =>
                  OtherIdentification(
                    identification = otherIdentication.Id,
                    scheme = otherIdentication.SchmeNm.map(scheme =>
                      OtherIdentificationScheme(
                        schemeType = OtherIdentificationSchemeType.withName(scheme.personidentificationschemename1choiceoption.key.get),
                        schemeName = scheme.personidentificationschemename1choiceoption.value.toString
                      )
                    ),
                    issuer = otherIdentication.Issr
                  )
                )
              ))
            case organisationIdentification: inquiryClaimValueDateCorrection.OrganisationIdentification8able =>
              Right(OrganisationIdentification(
                bic = organisationIdentification.AnyBIC,
                other = organisationIdentification.Othr.headOption.map(otherIdentication =>
                  OtherIdentification(
                    identification = otherIdentication.Id,
                    scheme = otherIdentication.SchmeNm.map(scheme =>
                      OtherIdentificationScheme(
                        schemeType = OtherIdentificationSchemeType.withName(scheme.organisationidentificationschemename1choiceoption.key.get),
                        schemeName = scheme.organisationidentificationschemename1choiceoption.value.toString
                      )
                    ),
                    issuer = otherIdentication.Issr
                  )
                )
              ))
          }
        )
      )
    }
  }

  def fromParty35Choice(party35choicableoption: inquiryClaimValueDateCorrectionPositiveAnswer.Party35ChoicableOption): Party = {
    party35choicableoption match {
      case party: inquiryClaimValueDateCorrectionPositiveAnswer.PartyIdentification125able => Party(
        name = party.Nm,
        postalAddress = party.PstlAdr.map(postalAddress =>
          PostalAddress(
            countryCode = postalAddress.Ctry,
            addressLines = postalAddress.AdrLine
          )
        ),
        identification = party.Id.map(identification =>
          identification.party34choicableoption.value match {
            case personIdentification: inquiryClaimValueDateCorrectionPositiveAnswer.PersonIdentification13able =>
              Left(PersonIdentification(
                dateAndPlaceOfBirth = personIdentification.DtAndPlcOfBirth.map(dateAndPlaceOfBirth =>
                  DateAndPlaceOfBirth(
                    birthDate = dateAndPlaceOfBirth.BirthDt.toGregorianCalendar.toZonedDateTime.toLocalDate,
                    provinceOfBirth = dateAndPlaceOfBirth.PrvcOfBirth,
                    cityOfBirth = dateAndPlaceOfBirth.CityOfBirth,
                    countryCodeOfBirth = dateAndPlaceOfBirth.CtryOfBirth
                  )
                ),
                other = personIdentification.Othr.headOption.map(otherIdentication =>
                  OtherIdentification(
                    identification = otherIdentication.Id,
                    scheme = otherIdentication.SchmeNm.map(scheme =>
                      OtherIdentificationScheme(
                        schemeType = OtherIdentificationSchemeType.withName(scheme.personidentificationschemename1choiceoption.key.get),
                        schemeName = scheme.personidentificationschemename1choiceoption.value.toString
                      )
                    ),
                    issuer = otherIdentication.Issr
                  )
                )
              ))
            case organisationIdentification: inquiryClaimValueDateCorrectionPositiveAnswer.OrganisationIdentification8able =>
              Right(OrganisationIdentification(
                bic = organisationIdentification.AnyBIC,
                other = organisationIdentification.Othr.headOption.map(otherIdentication =>
                  OtherIdentification(
                    identification = otherIdentication.Id,
                    scheme = otherIdentication.SchmeNm.map(scheme =>
                      OtherIdentificationScheme(
                        schemeType = OtherIdentificationSchemeType.withName(scheme.organisationidentificationschemename1choiceoption.key.get),
                        schemeName = scheme.organisationidentificationschemename1choiceoption.value.toString
                      )
                    ),
                    issuer = otherIdentication.Issr
                  )
                )
              ))
          }
        )
      )
    }
  }

  def fromParty35Choice(party35choicableoption: inquiryClaimValueDateCorrectionNegativeAnswer.Party35ChoicableOption): Party = {
    party35choicableoption match {
      case party: inquiryClaimValueDateCorrectionNegativeAnswer.PartyIdentification125able => Party(
        name = party.Nm,
        postalAddress = party.PstlAdr.map(postalAddress =>
          PostalAddress(
            countryCode = postalAddress.Ctry,
            addressLines = postalAddress.AdrLine
          )
        ),
        identification = party.Id.map(identification =>
          identification.party34choicableoption.value match {
            case personIdentification: inquiryClaimValueDateCorrectionNegativeAnswer.PersonIdentification13able =>
              Left(PersonIdentification(
                dateAndPlaceOfBirth = personIdentification.DtAndPlcOfBirth.map(dateAndPlaceOfBirth =>
                  DateAndPlaceOfBirth(
                    birthDate = dateAndPlaceOfBirth.BirthDt.toGregorianCalendar.toZonedDateTime.toLocalDate,
                    provinceOfBirth = dateAndPlaceOfBirth.PrvcOfBirth,
                    cityOfBirth = dateAndPlaceOfBirth.CityOfBirth,
                    countryCodeOfBirth = dateAndPlaceOfBirth.CtryOfBirth
                  )
                ),
                other = personIdentification.Othr.headOption.map(otherIdentication =>
                  OtherIdentification(
                    identification = otherIdentication.Id,
                    scheme = otherIdentication.SchmeNm.map(scheme =>
                      OtherIdentificationScheme(
                        schemeType = OtherIdentificationSchemeType.withName(scheme.personidentificationschemename1choiceoption.key.get),
                        schemeName = scheme.personidentificationschemename1choiceoption.value.toString
                      )
                    ),
                    issuer = otherIdentication.Issr
                  )
                )
              ))
            case organisationIdentification: inquiryClaimValueDateCorrectionNegativeAnswer.OrganisationIdentification8able =>
              Right(OrganisationIdentification(
                bic = organisationIdentification.AnyBIC,
                other = organisationIdentification.Othr.headOption.map(otherIdentication =>
                  OtherIdentification(
                    identification = otherIdentication.Id,
                    scheme = otherIdentication.SchmeNm.map(scheme =>
                      OtherIdentificationScheme(
                        schemeType = OtherIdentificationSchemeType.withName(scheme.organisationidentificationschemename1choiceoption.key.get),
                        schemeName = scheme.organisationidentificationschemename1choiceoption.value.toString
                      )
                    ),
                    issuer = otherIdentication.Issr
                  )
                )
              ))
          }
        )
      )
    }
  }


  def fromPartyIdentification43(partyIdentification43: requestStatusUpdate.PartyIdentification43able): Party = {
    Party(
      name = partyIdentification43.Nm,
      postalAddress = partyIdentification43.PstlAdr.map(postalAddress =>
        PostalAddress(
          countryCode = postalAddress.Ctry,
          addressLines = postalAddress.AdrLine
        )
      ),
      identification = partyIdentification43.Id.map(identification =>
        identification.party11choicableoption.value match {
          case personIdentification: requestStatusUpdate.PersonIdentification5able =>
            Left(PersonIdentification(
              dateAndPlaceOfBirth = personIdentification.DtAndPlcOfBirth.map(dateAndPlaceOfBirth =>
                DateAndPlaceOfBirth(
                  birthDate = dateAndPlaceOfBirth.BirthDt.toGregorianCalendar.toZonedDateTime.toLocalDate,
                  provinceOfBirth = dateAndPlaceOfBirth.PrvcOfBirth,
                  cityOfBirth = dateAndPlaceOfBirth.CityOfBirth,
                  countryCodeOfBirth = dateAndPlaceOfBirth.CtryOfBirth
                )
              ),
              other = personIdentification.Othr.headOption.map(otherIdentication =>
                OtherIdentification(
                  identification = otherIdentication.Id,
                  scheme = otherIdentication.SchmeNm.map(scheme =>
                    OtherIdentificationScheme(
                      schemeType = OtherIdentificationSchemeType.withName(scheme.personidentificationschemename1choiceoption.key.get),
                      schemeName = scheme.personidentificationschemename1choiceoption.value.toString
                    )
                  ),
                  issuer = otherIdentication.Issr
                )
              )
            ))
          case organisationIdentification: requestStatusUpdate.OrganisationIdentification8able =>
            Right(OrganisationIdentification(
              bic = organisationIdentification.AnyBIC,
              other = organisationIdentification.Othr.headOption.map(otherIdentication =>
                OtherIdentification(
                  identification = otherIdentication.Id,
                  scheme = otherIdentication.SchmeNm.map(scheme =>
                    OtherIdentificationScheme(
                      schemeType = OtherIdentificationSchemeType.withName(scheme.organisationidentificationschemename1choiceoption.key.get),
                      schemeName = scheme.organisationidentificationschemename1choiceoption.value.toString
                    )
                  ),
                  issuer = otherIdentication.Issr
                )
              )
            ))
        }
      )
    )
  }

}

case class PostalAddress(
                          countryCode: Option[String] = None,
                          addressLines: Seq[String] = Nil
                        )

trait PartyIdentification

case class PersonIdentification(
                                 dateAndPlaceOfBirth: Option[DateAndPlaceOfBirth] = None,
                                 other: Option[OtherIdentification] = None
                               ) extends PartyIdentification

case class OrganisationIdentification(
                                       bic: Option[String] = None,
                                       other: Option[OtherIdentification] = None
                                     ) extends PartyIdentification

case class DateAndPlaceOfBirth(
                                birthDate: LocalDate,
                                provinceOfBirth: Option[String] = None,
                                cityOfBirth: String,
                                countryCodeOfBirth: String
                              )

case class OtherIdentification(
                                identification: String,
                                scheme: Option[OtherIdentificationScheme] = None,
                                issuer: Option[String] = None
                              )

case class OtherIdentificationScheme(
                                      schemeType: OtherIdentificationSchemeType,
                                      schemeName: String
                                    )

object OtherIdentificationSchemeType extends Enumeration {
  type OtherIdentificationSchemeType = Value

  implicit val decoder: Decoder[Value] = Decoder.decodeEnumeration(this)
  implicit val encoder: Encoder[Value] = Encoder.encodeEnumeration(this)

  val CODE = Value("Cd")
  val PROPRIETARY = Value("Prtry")
}

