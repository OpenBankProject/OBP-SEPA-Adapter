package adapter

import akka.actor.{ActorSystem, Props}
import com.openbankproject.commons.model.{BankId, ViewId}
import com.typesafe.config.ConfigFactory
import model.types.Bic

/**
 * This is the Adapter entry point
 */
object Adapter extends App {

  // Configuration loading
  val config = ConfigFactory.load()
  // We construct the actorSystem name according to the OBP-API requirements
  val systemName = "SouthSideAkkaConnector_" + config.getString("akka.remote.netty.tcp.hostname").replace('.', '-')
  val system = ActorSystem.create(systemName, config)
  system.actorOf(Props.create(classOf[AkkaConnectorActor]), "akka-connector-actor")

  // Information about the bank using the adapter
  // Those fields shouldn't be used, those information should be retrieved from the OBP-API
  // (for example by calling the getBank endpoint to get the BIC)
  def BANK_ID = BankId("THE_DEFAULT_BANK_ID")
  def BANK_BIC = Bic("OBPBDEB1XXX")

  // the VIE_ID to use when we call the OBP-API
  def VIEW_ID = ViewId("owner")

  // Information about the CSM
  def CSM_BIC = Bic("EBAPFRPPPSA")
  def CSM_NAME = Bic("EBA CLEARING")

}
