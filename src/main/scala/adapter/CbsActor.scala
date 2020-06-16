package adapter

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import com.openbankproject.commons.dto.{InBoundGetAdapterInfo, OutBoundGetAdapterInfo}
import com.openbankproject.commons.model.{InboundAdapterCallContext, InboundAdapterInfoInternal, Status}

class CbsActor extends Actor with ActorLogging {

  val cluster: Cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.join(self.path.address)

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive: Receive = {
    case OutBoundGetAdapterInfo(callContext) => {
      val result = InBoundGetAdapterInfo(
        inboundAdapterCallContext = InboundAdapterCallContext(
          callContext.correlationId,
          callContext.sessionId,
          callContext.generalContext
        ),
        status = successInBoundStatus,
        data = InboundAdapterInfoInternal("", Nil, "Adapter-Akka-CBS", "Jun2020", APIUtil.gitCommit, new Date().toString)
      )
      sender ! result
    }
    case _ =>
  }

  def successInBoundStatus: Status = Status("", Nil)
}