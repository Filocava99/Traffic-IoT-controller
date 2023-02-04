package it.pps.ddos.device.sensor

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.pps.ddos.device.Device
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.utils.{DataType, GivenDataType}
import it.pps.ddos.utils.GivenDataType.*
import it.pps.ddos.device.sensor.SensorActor

import java.time.format.DateTimeFormatter
import java.time.temporal.{Temporal, TemporalAmount, TemporalField, TemporalUnit}
import scala.collection.immutable.List
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

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

class StoreDataSensor[O: DataType](id: String,
                                   destinations: List[ActorRef[DeviceMessage]],
                                   processFun: O => O,
                                   condition: Long => Boolean,
                                   replyTo: ActorRef[DeviceMessage]) extends Device[O](id, destinations) with Sensor[O, O]:

  var timeStamp: java.time.LocalDateTime = java.time.LocalDateTime.now()

  // Implementation using a random number as map key
  // var currentKey: Double = Random.nextDouble()
  // var storedStatus: Map[Double, List[O]] = Map((currentKey, List.empty))

  // Implementation using the DateTime value as map key
  var storedStatus: Map[java.time.LocalDateTime, List[O]] = Map((timeStamp, List.empty))

  override def update(selfId: ActorRef[SensorMessage], physicalInput: O): Unit =
    val currentTime = java.time.LocalDateTime.now()
    status = Option(preProcess(physicalInput))

    // Implementation using the random number
    // storedStatus = storedStatus + (currentKey -> (storedStatus(currentKey) :+ preProcess(physicalInput)))

    storedStatus = storedStatus + (timeStamp -> (storedStatus(timeStamp) :+ preProcess(physicalInput)))
    if condition(currentTime.getMinute - timeStamp.getMinute) then
      replyTo ! SendData((storedStatus, timeStamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))))
      println("SENT: " + (storedStatus, timeStamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))))
      timeStamp = currentTime

      // Implementation using the random number
      // currentKey = Random.nextDouble()
      // storedStatus = storedStatus + (currentKey -> List.empty)

      storedStatus = storedStatus + (timeStamp -> List.empty)


  override def preProcess: O => O = processFun
  override def behavior(): Behavior[DeviceMessage] = SensorActor(this).dataStorageBehavior()