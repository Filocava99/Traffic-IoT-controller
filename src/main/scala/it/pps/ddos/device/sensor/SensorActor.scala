package it.pps.ddos.device.sensor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import it.pps.ddos.device.sensor.Sensor
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message, PropagateStatus, SensorMessage, Subscribe, Unsubscribe, UpdateStatus}
import it.pps.ddos.device.DeviceBehavior
import it.pps.ddos.device.DeviceBehavior.Tick
import it.pps.ddos.utils.DataType

import scala.concurrent.duration._

/**
 * Actor definition of a sensor
 */
object SensorActor:
  def apply[I: DataType, O: DataType](sensor: Sensor[I, O]): SensorActor[I, O] = new SensorActor(sensor)

class SensorActor[I: DataType, O: DataType](val sensor: Sensor[I, O]):
  private case object TimedSensorKey
  private def getBasicSensorBehavior(ctx: ActorContext[DeviceMessage]): PartialFunction[DeviceMessage, Behavior[DeviceMessage]] =
    case UpdateStatus[I](value) =>
      sensor.update(ctx.self, value)
      Behaviors.same

  /**
   * The actor timed behavior definition of a sensor
   *
   * @param duration the frequency of sensor data collection
   * @return Behavior[DeviceMessage]
   */
  def behaviorWithTimer(duration: FiniteDuration): Behavior[DeviceMessage] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timer =>
        timer.startTimerWithFixedDelay(TimedSensorKey, Tick, duration)
        Behaviors.receiveMessagePartial(getBasicSensorBehavior(context)
          .orElse(DeviceBehavior.getBasicBehavior(sensor, context))
          .orElse(DeviceBehavior.getTimedBehavior(sensor, context)))
      }
    }

  /**
   * The actor behavior definition of a sensor
   *
   * @return Behavior[DeviceMessage]
   */
  def behavior(): Behavior[DeviceMessage] = Behaviors.setup { context =>
    Behaviors.receiveMessagePartial(getBasicSensorBehavior(context)
      .orElse(DeviceBehavior.getBasicBehavior(sensor, context)))
  }