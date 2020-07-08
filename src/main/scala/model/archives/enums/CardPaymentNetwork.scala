package model.archives.enums

object CardPaymentNetwork extends Enumeration {
  type CardPaymentNetwork = Value

  val MASTERCARD: CardPaymentNetwork.Value = Value
  val VISA: CardPaymentNetwork.Value = Value
  val AMERICAN_EXPRESS: CardPaymentNetwork.Value = Value
}
