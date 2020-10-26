import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

trait AkkaActorSystemTest {
  val config = ConfigFactory.parseString(
    s"""
      akka.remote.netty.tcp.port=0
      """).withFallback(ConfigFactory.load())
  implicit val system = ActorSystem("sepa-adapter-test-request", config)
}
