package it.pps.ddos.device.sensor

import akka.actor.typed.Behavior
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import it.pps.ddos.device.DeviceBehavior
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, PropagateStatus, ReceivedAck, SensorMessage, Status}
import it.pps.ddos.utils.DataType
import com.github.nscala_time.time.Imports.*

object StoreDataSensorActor:
  def apply[O: DataType](sensor: StoreDataSensor[O]): StoreDataSensorActor[O] = new StoreDataSensorActor(sensor)


class StoreDataSensorActor[O: DataType](val sensor: StoreDataSensor[O]):

  private def basicStoreDataSensorBehavior(ctx: ActorContext[DeviceMessage]): PartialFunction[DeviceMessage, Behavior[DeviceMessage]] =
    case Status(ref, data: O) =>
      sensor.update(ctx.self, data)
      Behaviors.same
    case ReceivedAck(key) =>
      sensor.updateStorage(key)
      Behaviors.same
    case PropagateStatus[DeviceMessage](selfRef) =>
      sensor.propagate(selfRef, selfRef)
      Behaviors.same

  def behavior(): Behavior[DeviceMessage] =
    Behaviors.setup { context =>
      Behaviors.receiveMessagePartial(DeviceBehavior.getBasicBehavior(sensor, context)
        .orElse(basicStoreDataSensorBehavior(context))
      )
    }