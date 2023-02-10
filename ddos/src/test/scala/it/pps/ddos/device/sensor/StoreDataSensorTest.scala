package it.pps.ddos.device.sensor

import akka.actor.testkit.typed.javadsl.BehaviorTestKit
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestInbox}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.pps.ddos.device.DeviceBehavior
import it.pps.ddos.device.DeviceBehavior.Tick
import it.pps.ddos.device.DeviceProtocol.{AckedStatus, DeviceMessage, Message, PropagateStatus, StatusAck, SensorMessage, Status, UpdateStatus}
import it.pps.ddos.device.{Device, DeviceProtocol, Public, Timer}
import org.scalatest.flatspec.AnyFlatSpec
import it.pps.ddos.utils.GivenDataType.given
import it.pps.ddos.utils.DataType

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.Duration
import scala.util.Random
import com.github.nscala_time.time.Imports.*


case class DataCamera(idCamera: Int, timeStamp: DateTime, data: Map[Int, Int])
given GenericDataType: DataType[DataCamera] with
  override def defaultValue: DataCamera = DataCamera(-1, DateTime.now(), Map.empty)


class StoreDataSensorTest extends AnyFlatSpec:

  "A StoreDataSensor " should "be able to store new received data" in testStoringDataInSensor()
  "A StoreDataSensor " should "be able to send data to the group every N seconds" in testSendDataToDatabase()
  "A StoreDataSensor " should
    "be able to update its stored data after receiving the ack message from the group" in testUpdateStoredDataInSensorAfterAck()
  "A StoreDataSensor " should "be able to send multiple data to the group and receive an ack message from it in order to update data" in testStoreDataSensorBehavior()

  val testKit = ActorTestKit()

  def testStoringDataInSensor(): Unit =
    val testProbe = testKit.createTestProbe[Message]()

    val startTime = DateTime.now()
    val storeDataSensor = new StoreDataSensor[DataCamera]("storage", List(testProbe.ref), x => x)
    val storeDataSensorActor = testKit.spawn(storeDataSensor.behavior())

    var randNumb: Map[Int, Int] = Map.empty
    var listRandNumb: List[Map[Int, Int]] = List.empty

    while(DateTime.now().minute.get() - startTime.minute.get() < 1)
      Thread.sleep(5000)
      // generate new data to store
      randNumb = Map(Random.nextInt() -> Random.nextInt())
      println("DATA GENERATED: " + randNumb)

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! Status(testProbe.ref, DataCamera(1, DateTime.now(), randNumb))
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      testProbe.expectMessageType[AckedStatus[DataCamera]]

    println("Stored values: " + storeDataSensor.data)

  def testSendDataToDatabase(): Unit =
    val testProbe = testKit.createTestProbe[Message]()

    val startTime = DateTime.now()
    val storeDataSensor = new StoreDataSensor[DataCamera]("storage", List(testProbe.ref), x => x)
    val storeDataSensorActor = testKit.spawn(storeDataSensor.behavior())

    var randNumb: Map[Int, Int] = Map.empty
    var listRandNumb: List[Map[Int, Int]] = List.empty

    while (DateTime.now().minute.get() - startTime.minute.get() < 1)
      Thread.sleep(5000)
      // generate new data to store
      randNumb = Map(Random.nextInt() -> Random.nextInt())
      println("DATA GENERATED: " + randNumb)

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! Status(testProbe.ref, DataCamera(1, DateTime.now(), randNumb))
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      testProbe.expectMessageType[AckedStatus[DataCamera]]


  def testUpdateStoredDataInSensorAfterAck(): Unit =
    val testProbe = testKit.createTestProbe[Message]()

    val startTime = DateTime.now()
    val storeDataSensor = new StoreDataSensor[DataCamera]("storage", List(testProbe.ref), x => x)
    val storeDataSensorActor = testKit.spawn(storeDataSensor.behavior())

    var randNumb: Map[Int, Int] = Map.empty
    var listRandNumb: List[Map[Int, Int]] = List.empty
    var keyList: List[DateTime] = List.empty

    while (DateTime.now().minute.get() - startTime.minute.get() < 1)
      Thread.sleep(5000)
      // generate new data to store
      randNumb = Map(Random.nextInt() -> Random.nextInt())
      println("DATA GENERATED: " + randNumb)

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! Status(testProbe.ref, DataCamera(1, DateTime.now(), randNumb))
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      testProbe.expectMessageType[AckedStatus[DataCamera]]

    println("Stored values: " + storeDataSensor.data)

    // decide to remove the first detected data
    keyList = storeDataSensor.data.keys.toList.slice(0, 1)
    println("Elements to remove: " + keyList.last)

    // send the ack message from the database including the data to remove
    storeDataSensorActor ! StatusAck(keyList.last)
    Thread.sleep(500)

    println("Stored values after the ack message: " + storeDataSensor.data)

  def testStoreDataSensorBehavior(): Unit =
    val testProbe = testKit.createTestProbe[Message]()

    var startTime = DateTime.now()
    val storeDataSensor = new StoreDataSensor[DataCamera]("storage", List(testProbe.ref), x => x)
    val storeDataSensorActor = testKit.spawn(storeDataSensor.behavior())

    var randNumb: Map[Int, Int] = Map.empty
    var listRandNumb: List[Map[Int, Int]] = List.empty
    var keyList: List[DateTime] = List.empty

    while (DateTime.now().minute.get() - startTime.minute.get() < 1)
      Thread.sleep(5000)
      // generate new data to store
      randNumb = Map(Random.nextInt() -> Random.nextInt())
      println("DATA GENERATED: " + randNumb)

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! Status(testProbe.ref, DataCamera(1, DateTime.now(), randNumb))
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      testProbe.expectMessageType[AckedStatus[DataCamera]]

    println("Stored values: " + storeDataSensor.data)

    // decide to remove the first detected data
    keyList = storeDataSensor.data.keys.toList.slice(0, 1)
    println("Elements to remove: " + keyList.last)

    // send the ack message from the database including the data to remove
    storeDataSensorActor ! StatusAck(keyList.last)
    Thread.sleep(500)

    println("Stored values after the ack message: " + storeDataSensor.data)

    startTime = DateTime.now()

    while (DateTime.now().minute.get() - startTime.minute.get() < 1)
      Thread.sleep(5000)
      // generate new data to store
      randNumb = Map(Random.nextInt() -> Random.nextInt())
      println("DATA GENERATED: " + randNumb)

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! Status(testProbe.ref, DataCamera(1, DateTime.now(), randNumb))
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      testProbe.expectMessageType[AckedStatus[DataCamera]]

    println("Stored values: " + storeDataSensor.data)

    // decide to remove the first detected data
    keyList = storeDataSensor.data.keys.toList.slice(0, 1)
    println("Elements to remove: " + keyList.last)

    // send the ack message from the database including the data to remove
    storeDataSensorActor ! StatusAck(keyList.last)
    Thread.sleep(500)

    println("Stored values after the ack message: " + storeDataSensor.data)