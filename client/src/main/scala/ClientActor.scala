import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.github.nscala_time.time.Imports.DateTime
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.device.sensor.BasicSensor
import it.pps.ddos.utils.DataType
import it.pps.ddos.utils.GivenDataType.given
import it.sc.server.entities.RecordedData
import org.bson.types.ObjectId

import scala.language.postfixOps

class ClientActor:
  var actualEntry: RecordedData = RecordedData(new ObjectId().toHexString, DateTime.now().getMillis, Map.empty[Int, Int])

  def behavior(): Behavior[DeviceMessage] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage { msg => msg match
        case Status(ref: ActorRef[DeviceMessage], value: RecordedData) =>
          if !value.equals(actualEntry) && value.idCamera != actualEntry.idCamera then
            ref ! Subscribe(context.self)
            actualEntry = value
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
  def apply(): ClientActor = new ClientActor()