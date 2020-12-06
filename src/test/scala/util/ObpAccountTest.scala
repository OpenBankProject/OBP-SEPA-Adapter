package util

import com.openbankproject.commons.model.{AccountId, BankId, Iban}
import model.types.Bic

object ObpAccountTest {
  val ACCOUNT_ID: AccountId = AccountId("1023794c-a412-11ea-bb37-0242ac130002")
  val BANK_ID: BankId = BankId("THE_DEFAULT_BANK_ID")
  val ACCOUNT_HOLDER_NAME = "Guillaume"
  val IBAN: Iban = Iban("DE65500105176582659391")
  val BIC: Bic = Bic("OBPBDEB1XXX")
}
