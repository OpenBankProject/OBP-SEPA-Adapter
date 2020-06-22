import model._
import model.archives.SchemaArchive
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import model.archives.SchemaArchive._

object ReadDatabase extends App {
  val db = Database.forConfig("databaseConfig")

  val query = for {
    customers <- SchemaArchive.customers
    accounts <- SchemaArchive.accounts if accounts.customerId === customers.id
    transactions <- SchemaArchive.transactions if transactions.accountId === accounts.id
  } yield (customers.firstName, accounts.iban, transactions.transactionType, transactions.amount)

  val request = db.run(query.result)

  request.onComplete {
    case Success(value) => value.foreach(println(_))
    case Failure(exception) => exception.printStackTrace()
  }

  Await.result(request, Duration.Inf)

}

