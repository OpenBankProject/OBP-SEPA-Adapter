package adapter

import akka.actor.{ActorSystem, Props}
import com.openbankproject.commons.model.{BankId, ViewId}
import com.typesafe.config.ConfigFactory
import model.types.Bic

object Adapter extends App {

  val config = ConfigFactory.load()
  val systemName = "SouthSideAkkaConnector_" + config.getString("akka.remote.netty.tcp.hostname").replace('.', '-')
  val system = ActorSystem.create(systemName, config)
  system.actorOf(Props.create(classOf[AkkaConnectorActor]), "akka-connector-actor")

  // Information about the bank using the adapter
  def BANK_ID = BankId("THE_DEFAULT_BANK_ID")
  def BANK_BIC = Bic("OBPBDEB1XXX")

  def VIEW_ID = ViewId("owner")

  def CSM_BIC = Bic("EBAPFRPPPSA")
  def CSM_NAME = Bic("EBA CLEARING")

}
