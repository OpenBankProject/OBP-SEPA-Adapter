package adapter

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Adapter extends App {

  val config = ConfigFactory.load()

  val systemName = "SouthSideAkkaConnector_" + config.getString("akka.remote.netty.tcp.hostname").replace('.', '-')

  val system = ActorSystem.create(systemName, config)

  system.actorOf(Props.create(classOf[AkkaConnectorActor]), "akka-connector-actor")

}
