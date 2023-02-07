package it.pps.ddos.device.sensor

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.pps.ddos.device.DeviceBehavior
import it.pps.ddos.device.DeviceBehavior.Tick
import it.pps.ddos.device.DeviceProtocol.{DataCamera, DeviceMessage, Message, ReceivedAck, DataOutput, SensorMessage, Status, UpdateStatus}
import it.pps.ddos.device.{Device, DeviceProtocol, Public, Timer}
import org.scalatest.flatspec.AnyFlatSpec
import it.pps.ddos.utils.GivenDataType.given

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.Duration
import scala.util.Random
import com.github.nscala_time.time.Imports._

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
    val minute = 1
    val delta = 0.1
    val condition: Int => Boolean = _ == minute
    val storeDataSensor = new StoreDataSensor[(Int, Int)]("storage", List(testProbe.ref), x => x, condition, testProbe.ref)
    val storeDataSensorActor = testKit.spawn(storeDataSensor.behavior())

    var randNumb = (0, 0)
    var listRandNumb: List[(Int, Int)] = List.empty

    while(DateTime.now().minute.get() - startTime.minute.get() < 1)
      Thread.sleep(5000)
      // generate new data to store
      randNumb = (Random.nextInt(), Random.nextInt())
      println("DATA GENERATED: " + randNumb)

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! DataCamera(1, DateTime.now(), randNumb)
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      if (DateTime.now().minute.get() - storeDataSensor.currentDateTime.minute.get()) == minute + delta then
        testProbe.expectMessage(DataOutput(storeDataSensor.currentDateTime.toString("yyyy-MM-dd HH:mm"), storeDataSensor.data))

    println("Stored values: " + storeDataSensor.data)

  def testSendDataToDatabase(): Unit =
    val testProbe = testKit.createTestProbe[Message]()

    val startTime = DateTime.now()
    val minute = 1
    val delta = 0.1
    val condition: Int => Boolean = _ == minute
    val storeDataSensor = new StoreDataSensor[(Int, Int)]("storage", List(testProbe.ref), x => x, condition, testProbe.ref)
    val storeDataSensorActor = testKit.spawn(storeDataSensor.behavior())

    var randNumb = (0, 0)
    var listRandNumb: List[(Int, Int)] = List.empty

    while (DateTime.now().minute.get() - startTime.minute.get() < 1)
      Thread.sleep(5000)
      // generate new data to store
      randNumb = (Random.nextInt(), Random.nextInt())
      println("DATA GENERATED: " + randNumb)

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! DataCamera(1, DateTime.now(), randNumb)
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      if (DateTime.now().minute.get() - storeDataSensor.currentDateTime.minute.get()) == minute + delta then
        testProbe.expectMessage(DataOutput(storeDataSensor.currentDateTime.toString("yyyy-MM-dd HH:mm"), storeDataSensor.data))

  def testUpdateStoredDataInSensorAfterAck(): Unit =
    val testProbe = testKit.createTestProbe[Message]()

    val startTime = DateTime.now()
    val minute = 1
    val delta = 0.1
    val condition: Int => Boolean = _ == minute
    val storeDataSensor = new StoreDataSensor[(Int, Int)]("storage", List(testProbe.ref), x => x, condition, testProbe.ref)
    val storeDataSensorActor = testKit.spawn(storeDataSensor.behavior())

    var randNumb = (0, 0)
    var listRandNumb: List[(Int, Int)] = List.empty
    var keyList: List[String] = List.empty

    while (DateTime.now().minute.get() - startTime.minute.get() < 1)
      Thread.sleep(5000)
      // generate new data to store
      randNumb = (Random.nextInt(), Random.nextInt())
      println("DATA GENERATED: " + randNumb)

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! DataCamera(1, DateTime.now(), randNumb)
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      if (DateTime.now().minute.get() - storeDataSensor.currentDateTime.minute.get()) == minute + delta then
        testProbe.expectMessage(DataOutput(storeDataSensor.currentDateTime.toString("yyyy-MM-dd HH:mm"), storeDataSensor.data))

    println("Stored values: " + storeDataSensor.data)

    // decide to remove the first detected data
    keyList = storeDataSensor.data.keys.toList.slice(0, 1)
    println("Elements to remove: " + keyList)

    // send the ack message from the database including the data to remove
    storeDataSensorActor ! ReceivedAck(keyList.toString())
    Thread.sleep(500)

    println("Stored values after the ack message: " + storeDataSensor.data)

  def testStoreDataSensorBehavior(): Unit =
    val testProbe = testKit.createTestProbe[Message]()

    var startTime = DateTime.now()
    val minute = 1
    val delta = 0.1
    val condition: Int => Boolean = _ == minute
    val storeDataSensor = new StoreDataSensor[(Int, Int)]("storage", List(testProbe.ref), x => x, condition, testProbe.ref)
    val storeDataSensorActor = testKit.spawn(storeDataSensor.behavior())

    var randNumb = (0, 0)
    var listRandNumb: List[(Int, Int)] = List.empty
    var keyList: List[String] = List.empty

    while (DateTime.now().minute.get() - startTime.minute.get() < 1)
      Thread.sleep(5000)
      // generate new data to store
      randNumb = (Random.nextInt(), Random.nextInt())
      println("DATA GENERATED: " + randNumb)

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! DataCamera(1, DateTime.now(), randNumb)
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      if (DateTime.now().minute.get() - storeDataSensor.currentDateTime.minute.get()) == minute + delta then
        testProbe.expectMessage(DataOutput(storeDataSensor.currentDateTime.toString("yyyy-MM-dd HH:mm"), storeDataSensor.data))

    println("Stored values: " + storeDataSensor.data)

    // decide to remove the first detected data
    keyList = storeDataSensor.data.keys.toList.slice(0, 1)
    println("Elements to remove: " + keyList)

    // send the ack message from the database including the data to remove
    storeDataSensorActor ! ReceivedAck(keyList.toString())
    Thread.sleep(500)

    println("Stored values after the ack message: " + storeDataSensor.data)

    startTime = DateTime.now()

    while (DateTime.now().minute.get() - startTime.minute.get() < 1)
      Thread.sleep(5000)
      // generate new data to store
      randNumb = (Random.nextInt(), Random.nextInt())
      println("DATA GENERATED: " + randNumb)

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! DataCamera(1, DateTime.now(), randNumb)
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      if (DateTime.now().minute.get() - storeDataSensor.currentDateTime.minute.get()) == minute + delta then
        testProbe.expectMessage(DataOutput(storeDataSensor.currentDateTime.toString("yyyy-MM-dd HH:mm"), storeDataSensor.data))

    println("Stored values: " + storeDataSensor.data)

    // decide to remove the first detected data
    keyList = storeDataSensor.data.keys.toList.slice(0, 1)
    println("Elements to remove: " + keyList)

    // send the ack message from the database including the data to remove
    storeDataSensorActor ! ReceivedAck(keyList.toString())
    Thread.sleep(500)

    println("Stored values after the ack message: " + storeDataSensor.data)