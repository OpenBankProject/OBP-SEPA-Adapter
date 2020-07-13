package adapter

import akka.actor.{ActorSystem, Props}
import com.openbankproject.commons.model.{BankId, ViewId}
import com.typesafe.config.ConfigFactory
import model.types.Bic

object Adapter extends App {

  // Information about the bank using the adapter
  val BANK_ID = BankId("THE_DEFAULT_BANK_ID")
  val BANK_BIC = Bic("OBPBDEB1XXX")

  val VIEW_ID = ViewId("owner")

  val config = ConfigFactory.load()
  val systemName = "SouthSideAkkaConnector_" + config.getString("akka.remote.netty.tcp.hostname").replace('.', '-')
  val system = ActorSystem.create(systemName, config)
  system.actorOf(Props.create(classOf[AkkaConnectorActor]), "akka-connector-actor")

}
