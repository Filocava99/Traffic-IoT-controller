import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Status, Subscribe, SubscribeAck, UnsubscribeAck}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.pps.ddos.device.sensor.BasicSensor
import it.pps.ddos.utils.GivenDataType.StringDataType

import scala.language.postfixOps

class ClientActor:
  var actualActorRef: ActorRef[DeviceMessage] = _

  def behavior(): Behavior[DeviceMessage] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage { msg => msg match
        case Status(ref: ActorRef[DeviceMessage], value) if ref != actualActorRef =>
          ref ! Subscribe(context.self)
          actualActorRef = ref
          Behaviors.same
        case SubscribeAck(author) =>
          println("Subscribed to " + author)
          Behaviors.same
        case UnsubscribeAck(author) =>
          println("Unsubscribed from " + author)
          Behaviors.same
      }
    }

object ClientActor:
  def apply() = new ClientActor()