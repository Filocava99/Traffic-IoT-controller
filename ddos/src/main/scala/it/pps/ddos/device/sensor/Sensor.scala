package it.pps.ddos.device.sensor

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.pps.ddos.device.Device
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.utils.{DataType, GivenDataType}
import it.pps.ddos.utils.GivenDataType.*

import scala.collection.immutable.List
import scala.concurrent.duration.FiniteDuration

/**
 * Abstract definition of sensor
 *
 * @tparam I DataType
 * @tparam O DataType
 */
trait Sensor[I: DataType, O: DataType] extends Device[O]:
  status = Option(DataType.defaultValue[O])
  def preProcess: I => O
  def update(selfId: ActorRef[SensorMessage], physicalInput: I): Unit = this.status = Option(preProcess(physicalInput))

/**
 * Abstract definition of sensor modules
 *
 * @tparam I DataType
 * @tparam O DataType
 */
trait Condition[I: DataType, O: DataType](condition: O => Boolean, replyTo: ActorRef[DeviceMessage]):
  self: Sensor[I, O] =>
  override def update(selfId: ActorRef[SensorMessage], physicalInput: I): Unit =
    self.status = Option(preProcess(physicalInput))
    if condition(self.status.get) then replyTo ! Status[O](selfId, self.status.get)

/**
 * Concrete definition of the basic sensor
 *
 * @param id the sensor id
 * @param destinations the list of other devices
 *
 * @tparam O DataType
 */
class BasicSensor[O: DataType](id: String, destinations: List[ActorRef[DeviceMessage]]) extends Device[O](id, destinations) with Sensor[O, O]:
  override def preProcess: O => O = x => x
  override def behavior(): Behavior[DeviceMessage] = SensorActor(this).behavior()

/**
 * Concrete definition of the sensor sensor capable of data processing
 *
 * @param id the sensor id
 * @param destinations the list of other devices
 * @param processFun the function that processes the sensor value
 *
 * @tparam I DataType
 * @tparam O DataType
 */
class ProcessedDataSensor[I: DataType, O: DataType](id: String,
                                                    destinations: List[ActorRef[DeviceMessage]],
                                                    processFun: I => O) extends Device[O](id, destinations) with Sensor[I, O]:
  override def preProcess: I => O = processFun
  override def behavior(): Behavior[DeviceMessage] = SensorActor(this).behavior()