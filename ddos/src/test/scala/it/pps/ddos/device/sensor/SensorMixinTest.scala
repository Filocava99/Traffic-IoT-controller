package it.pps.ddos.device.sensor

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory
import it.pps.ddos.device.Device
import it.pps.ddos.device.Timer
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message, PropagateStatus, Status, Subscribe, SubscribeAck, Unsubscribe, UnsubscribeAck, UpdateStatus}
import it.pps.ddos.device.sensor.{BasicSensor, Sensor}
import it.pps.ddos.device.Public
import it.pps.ddos.utils.GivenDataType.given
import org.scalatest.flatspec.AnyFlatSpec

import java.io.File
import scala.concurrent.duration.FiniteDuration

class SensorMixinTest extends AnyFlatSpec:
  /*
  * Sensor testing with a basic mixin
  * */
  "A BasicSensor with the Condition module" should "be able to update and send its own status when the condition is verified" in testBasicSensorActorWithConditionModule()
  "A BasicSensor with the Public module" should "be able to propagate its status in broadcast" in testBasicSensorActorBroadcastWithPublicModule()
  "A BasicSensor with the Public module" should "be able to add destinations in its own list and then to send back an ack" in testBasicSensorActorSubscribeWithPublicModule()
  "A BasicSensor with the Public module" should "be able to remove destinations from its own list and then to send back an ack" in testBasicSensorActorUnsubscribeWithPublicModule()
  "A BasicSensor with the Timer module" should "be able to have the behavior of a timed SensorActor" in testBasicSensorActorWithTimerModule()
  "A ProcessedDataSensor with the Condition module" should
    "be able to both process the data and update and send its own status when the condition is verified" in testProcessedDataSensorActorWithConditionModule()
  "A ProcessedDataSensor with the Public module" should
    "be able to propagate its status in broadcast" in testProcessedDataSensorActorBroadcastWithPublicModule()
  "A ProcessedDataSensor with the Public module" should
    "be able to add destinations in its own list and then to send back an ack" in testProcessedDataSensorActorSubscribeWithPublicModule()
  "A ProcessedDataSensor with the Public module" should
    "be able to remove destinations from its own list and then to send back an ack" in testProcessedDataSensorActorUnsubscribeWithPublicModule()
  "A ProcessedDataSensor with the Timer module" should
    "be able to have the behavior of a timed SensorActor" in testProcessedDataSensorActorWithTimerModule()

  /*
    * Sensor testing with a complete mixin
    * */
  "A BasicSensor with the Condition, Public and Timer modules" should "be able to:" +
    "update and send its own status when the condition is verified;" +
    "propagate its status in broadcast;" +
    "add destinations in its own list and then to send back an ack;" +
    "remove destinations from its own list and then to send back an ack;" +
    "have the behavior of a timed SensorActor." in testBasicSensorActorWithAllModules()

  "A ProcessedDataSensor with the Condition, Public and Timer modules" should "be able to:" +
    "update and send its own status when the condition is verified;" +
    "propagate its status in broadcast;" +
    "add destinations in its own list and then to send back an ack;" +
    "remove destinations from its own list and then to send back an ack;" +
    "have the behavior of a timed SensorActor." in testProcessedDataSensorWithAllModules()

  val testKit = ActorTestKit()

  private def sendUpdateStatusMessage[A](sensor: ActorRef[DeviceMessage], value: A): Unit =
    sensor ! UpdateStatus(value)
    Thread.sleep(800)

  private def testBasicSensorActorWithAllModules(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val interval: FiniteDuration = FiniteDuration(2, "seconds")
    val exampleClass = new BasicSensor[String]("1", List.empty) with Condition[String, String](_ contains "test", testProbe.ref) with Public[String] with Timer(interval)
    val sensorBehavior: Behavior[DeviceMessage] = exampleClass.behavior()
    val sensor = testKit.spawn(sensorBehavior)

    // status update: the status is going to be set "test"
    sendUpdateStatusMessage(sensor, "test") // this value sets true the condition
    // when the condition is verified it's expected a Status message
    testProbe.expectMessage(Status(sensor, "test"))

    for (_ <- 1 to 3) {
      Thread.sleep(interval.toMillis)
      testProbe.expectNoMessage()

      // sending the Subscribe message, so it's expected the SubscribeAck message
      sensor ! Subscribe(testProbe.ref)
      testProbe.expectMessage(SubscribeAck(sensor))
      // sending the Propagate message after the Subscribe one, so it's expected a Status message
      sensor ! PropagateStatus(sensor)
      testProbe.expectMessage(Status(sensor, "test"))
      // sending the Unsubscribe message, so it's expected UnsubscribeAck message
      sensor ! Unsubscribe(testProbe.ref)
      testProbe.expectMessage(UnsubscribeAck(sensor))
    }

    testKit.stop(sensor)

  private def testProcessedDataSensorWithAllModules(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val interval: FiniteDuration = FiniteDuration(2, "seconds")
    val exampleClass = new ProcessedDataSensor[Int, Int]("1", List(testProbe.ref), _ + 20) with Condition[Int, Int](_ > 10, testProbe.ref) with Public[Int] with Timer(interval)
    val sensorBehavior: Behavior[DeviceMessage] = exampleClass.behavior()
    val sensor = testKit.spawn(sensorBehavior)

    // status update: the status is going to be set '20'
    sendUpdateStatusMessage(sensor, 20)
    testProbe.expectMessage(Status(sensor, 40))

    for (_ <- 1 to 3) {
      Thread.sleep(interval.toMillis)
      testProbe.expectMessage(Status(sensor, 40))

      // sending the Unsubscribe message, so it's expected UnsubscribeAck message
      sensor ! Unsubscribe(testProbe.ref)
      testProbe.expectMessage(UnsubscribeAck(sensor))
      // sending the Subscribe message, so it's expected the SubscribeAck message
      sensor ! Subscribe(testProbe.ref)
      testProbe.expectMessage(SubscribeAck(sensor))
      // sending the Propagate message after the Subscribe one, so it's expected a Status message
      sensor ! PropagateStatus(sensor)
      testProbe.expectMessage(Status(sensor, 40))
    }

    testKit.stop(sensor)

  private def testBasicSensorActorWithConditionModule(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val destinations: List[ActorRef[Message]] = List(testProbe.ref)
    val exampleClass = new BasicSensor[String]("1", destinations) with Condition[String, String](_ contains "test", destinations.head)
    val sensorBehavior: Behavior[DeviceMessage] = exampleClass.behavior()
    val sensor = testKit.spawn(sensorBehavior)

    // status update: the status is going to be set "test"
    sendUpdateStatusMessage(sensor, String("test")) // this value sets true the condition
    // when the condition is verified it's expected a Status message
    testProbe.expectMessage(Status(sensor, String("test")))

  private def testBasicSensorActorBroadcastWithPublicModule(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val exampleClass = new BasicSensor[String]("1", List.empty) with Public[String]
    val sensorBehavior: Behavior[DeviceMessage] = exampleClass.behavior()
    val sensor = testKit.spawn(sensorBehavior)

    // status update: the status is going to be set "test"
    sendUpdateStatusMessage(sensor, "test")
    testProbe.expectNoMessage()
    // sending the Subscribe message, so it's expected the SubscribeAck message
    sensor ! Subscribe(testProbe.ref)
    Thread.sleep(800)
    testProbe.expectMessage(SubscribeAck(sensor))
    // sending the Propagate message after the Subscribe one, so it's expected a Status message
    sensor ! PropagateStatus(sensor)
    Thread.sleep(800)
    testProbe.expectMessage(Status(sensor, "test"))

  private def testBasicSensorActorSubscribeWithPublicModule(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val exampleClass = new BasicSensor[String]("1", List.empty) with Public[String]
    val sensorBehavior: Behavior[DeviceMessage] = exampleClass.behavior()
    val sensor = testKit.spawn(sensorBehavior)

    // status update: the status is going to be set "test"
    sendUpdateStatusMessage(sensor, "test")
    testProbe.expectNoMessage()
    // sending the Subscribe message, so it's expected the SubscribeAck message
    sensor ! Subscribe(testProbe.ref)
    Thread.sleep(800)
    testProbe.expectMessage(SubscribeAck(sensor))

  private def testBasicSensorActorUnsubscribeWithPublicModule(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val exampleClass = new BasicSensor[String]("1", List.empty) with Public[String]
    val sensorBehavior: Behavior[DeviceMessage] = exampleClass.behavior()
    val sensor = testKit.spawn(sensorBehavior)

    // status update: the status is going to be set "test"
    sendUpdateStatusMessage(sensor, "test")
    testProbe.expectNoMessage()
    // sending the Subscribe message, so it's expected the SubscribeAck message
    sensor ! Subscribe(testProbe.ref)
    Thread.sleep(800)
    testProbe.expectMessage(SubscribeAck(sensor))
    // sending the Unsubscribe message, so it's expected UnsubscribeAck message
    sensor ! Unsubscribe(testProbe.ref)
    Thread.sleep(800)
    testProbe.expectMessage(UnsubscribeAck(sensor))

  private def testBasicSensorActorWithTimerModule(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val interval: FiniteDuration = FiniteDuration(2, "seconds")
    val exampleClass = new BasicSensor[String]("1", List.empty) with Timer(interval)
    val sensorBehavior: Behavior[DeviceMessage] = exampleClass.behavior()
    val sensor = testKit.spawn(sensorBehavior)

    // sending the Subscribe message, so it's expected the SubscribeAck message
    sensor ! Subscribe(testProbe.ref)
    Thread.sleep(800)
    testProbe.expectNoMessage()
    // sending the Propagate message, so it's expected a Status message
    sensor ! PropagateStatus(sensor)

    for (_ <- 1 to 3) {
      Thread.sleep(interval.toMillis)
      testProbe.expectNoMessage()
    }
    // sending the Unsubscribe message, so it's expected UnsubscribeAck message
    sensor ! Unsubscribe(testProbe.ref)
    Thread.sleep(800)
    testProbe.expectNoMessage()

    testKit.stop(sensor)

  private def testProcessedDataSensorActorWithConditionModule(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val exampleClass = new ProcessedDataSensor[Int, Int]("1", List(testProbe.ref), _ + 20) with Condition[Int, Int](x => x.isInstanceOf[Int], testProbe.ref)
    val sensorBehavior: Behavior[DeviceMessage] = exampleClass.behavior()
    val sensor = testKit.spawn(sensorBehavior)

    // status update: the status is going to be set '20'
    sendUpdateStatusMessage(sensor, 20)
    // then it's expected a Status message
    testProbe.expectMessage(Status(sensor, 40))

  private def testProcessedDataSensorActorBroadcastWithPublicModule(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val exampleClass = new ProcessedDataSensor[Int, Int]("1", List.empty, _ + 20) with Public[Int]
    val sensorBehavior: Behavior[DeviceMessage] = exampleClass.behavior()
    val sensor = testKit.spawn(sensorBehavior)

    // status update: the status is going to be set "test"
    sendUpdateStatusMessage(sensor, 20)
    testProbe.expectNoMessage()
    // sending the Subscribe message, so it's expected the SubscribeAck message
    sensor ! Subscribe(testProbe.ref)
    Thread.sleep(800)
    testProbe.expectMessage(SubscribeAck(sensor))
    // sending the Propagate message after the Subscribe one, so it's expected a Status message
    sensor ! PropagateStatus(sensor)
    Thread.sleep(800)
    testProbe.expectMessage(Status(sensor, 40))

  private def testProcessedDataSensorActorSubscribeWithPublicModule(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val exampleClass = new ProcessedDataSensor[Int, Int]("1", List.empty, _ + 20) with Public[Int]
    val sensorBehavior: Behavior[DeviceMessage] = exampleClass.behavior()
    val sensor = testKit.spawn(sensorBehavior)

    // status update: the status is going to be set "test"
    sendUpdateStatusMessage(sensor, 20)
    testProbe.expectNoMessage()
    // sending the Subscribe message, so it's expected the SubscribeAck message
    sensor ! Subscribe(testProbe.ref)
    Thread.sleep(800)
    testProbe.expectMessage(SubscribeAck(sensor))

  private def testProcessedDataSensorActorUnsubscribeWithPublicModule(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val exampleClass = new ProcessedDataSensor[Int, Int]("1", List.empty, _ + 20) with Public[Int]
    val sensorBehavior: Behavior[DeviceMessage] = exampleClass.behavior()
    val sensor = testKit.spawn(sensorBehavior)

    // status update: the status is going to be set "test"
    sendUpdateStatusMessage(sensor, 20)
    testProbe.expectNoMessage()
    // sending the Subscribe message, so it's expected the SubscribeAck message
    sensor ! Subscribe(testProbe.ref)
    Thread.sleep(800)
    testProbe.expectMessage(SubscribeAck(sensor))
    // sending the Unsubscribe message, so it's expected UnsubscribeAck message
    sensor ! Unsubscribe(testProbe.ref)
    Thread.sleep(800)
    testProbe.expectMessage(UnsubscribeAck(sensor))


  private def testProcessedDataSensorActorWithTimerModule(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val interval: FiniteDuration = FiniteDuration(3, "seconds")
    val exampleClass = new ProcessedDataSensor[Int, Int]("1", List(testProbe.ref), _ + 20) with Timer(interval)
    val sensorBehavior: Behavior[DeviceMessage] = exampleClass.behavior()
    val sensor = testKit.spawn(sensorBehavior)

    sendUpdateStatusMessage(sensor, 20)
    testProbe.expectNoMessage()

    for (_ <- 1 to 3) {
      Thread.sleep(interval.toMillis)
      testProbe.expectMessage(Status(sensor, 40))

      // sending the Unsubscribe message, so it's expected UnsubscribeAck message
      sensor ! Unsubscribe(testProbe.ref)
      testProbe.expectNoMessage()

      // sending the Subscribe message, so it's expected the SubscribeAck message
      sensor ! Subscribe(testProbe.ref)
      testProbe.expectNoMessage()
    }

    testKit.stop(sensor)



