package it.pps.ddos.device.sensor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import it.pps.ddos.device.DeviceBehavior
import it.pps.ddos.device.DeviceProtocol.{DataCamera, DeviceMessage, ReceivedAck}
import it.pps.ddos.utils.DataType

object StoreDataSensorActor:
  def apply[O: DataType](sensor: StoreDataSensor[O]): StoreDataSensorActor[O] = new StoreDataSensorActor(sensor)


class StoreDataSensorActor[O: DataType](val sensor: StoreDataSensor[O]):

  private def basicStoreDataSensorBehavior(ctx: ActorContext[DeviceMessage]): PartialFunction[DeviceMessage, Behavior[DeviceMessage]] =
    case DataCamera(idCamera, dateTime, data: O) =>
      sensor.timeStamp(dateTime)
      sensor.update(ctx.self, data)
      Behaviors.same
    case ReceivedAck(values) =>
      sensor.updateStorage(values)
      Behaviors.same

  def behavior(): Behavior[DeviceMessage] =
    Behaviors.setup { context =>
      Behaviors.receiveMessagePartial(DeviceBehavior.getBasicBehavior(sensor, context)
        .orElse(basicStoreDataSensorBehavior(context))
      )
    }