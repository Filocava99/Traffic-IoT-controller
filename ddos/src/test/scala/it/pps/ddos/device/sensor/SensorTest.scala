package it.pps.ddos.device.sensor

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory
import it.pps.ddos.device.Device
import it.pps.ddos.device.Timer
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message, PropagateStatus, Status, Subscribe, SubscribeAck, Unsubscribe, UnsubscribeAck, UpdateStatus}
import it.pps.ddos.device.sensor.{BasicSensor, Sensor, SensorActor}
import it.pps.ddos.utils.DataType
import it.pps.ddos.device.Public
import it.pps.ddos.utils.GivenDataType.{AnyDataType, DoubleDataType, IntDataType, StringDataType}
import org.scalatest.flatspec.AnyFlatSpec

import java.io.File
import scala.concurrent.duration.FiniteDuration

class SensorTest extends AnyFlatSpec:

  /*
  * SensorActor and timed SensorActor Tests
  * */
  "A SensorActor " should "be able to receive PropagateStatus, UpdateStatus, Subscribe and Unsubscribe messages" in testSensorActorReceiveMessage()
  "A timed SensorActor " should "be able to receive the same types of Message as SensorActor" in testTimedSensorActorReceiveMessage()
  "A timed SensorActor " should "be able to send a Status message at fixed rate" in testTimedSensorActorSendMessageAtFixedRate()

  /*
  * Sensors and Modules tests
  * */
  "A Public BasicSensor[T]" should "be able to send and update his T-type Status " in testPublicBasicSensorSendCorrect()
  it should "not be able to update his Status with a value that doesn't match the T-type" in testPublicBasicSensorStatusWrong()

  "A Public ProcessedDataSensor[A,B]" should " be able to process data and also send/update his Status" in testPublicProcessedDataSensorSendCorrect()
  it should "not be able to process a non B-type value in the update() function" in testPublicProcessedDataSensorStatusWrong()

  "A Public Basic-ConditionSensor" should "be able and update his Status only if the condition is True" in testBasicConditionSensorCorrect()
  it should "not be able to update his Status if the condition is False" in testBasicConditionSensorWrong()

  "A Public ProcessedData-ConditionSensor" should "be able and update his Status only if the condition is True " +
    "and if a B-Type value is pass to the update function" in testProcessedConditionSensorCorrect()
  it should "not be able to update his Status otherwise" in testProcessedConditionSensorWrong()

  "A Public Basic-TimedSensor" should "be able and update his Status only if the time is passed" in testBasicTimedSensorCorrect()
  it should "not be able to update his Status otherwise" in testBasicTimedSensorWrong()

  "A Public ProcessedData-TimedSensor" should "be able and update his Status only if the time is passed " +
    "and if a B-Type value is pass to the update function" in testProcessedTimedSensorCorrect()
  it should "not be able to update his Status otherwise" in testProcessedTimedSensorWrong()

  val testKit = ActorTestKit()

  private def sendPropagateStatusMessage(sensor: ActorRef[DeviceMessage]): Unit =
    sensor ! PropagateStatus(sensor)
    Thread.sleep(800)

  private def sendUpdateStatusMessage[A](sensor: ActorRef[DeviceMessage], value: A): Unit =
    sensor ! UpdateStatus(value)
    Thread.sleep(800)

  private def sendSubscribeMessage(sensor: ActorRef[DeviceMessage]): Unit =
    sensor ! Subscribe(sensor)
    Thread.sleep(800)

  private def sendUnsubscribeMessage(sensor: ActorRef[DeviceMessage]): Unit =
    sensor ! Unsubscribe(sensor)
    Thread.sleep(800)

  private def testPropagateStatusWithTimedSensorActor(interval: FiniteDuration): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val timedSensor = new BasicSensor[Double]("1", List(testProbe.ref)) with Timer(interval)
    val sensorActor: Behavior[DeviceMessage] = timedSensor.behavior()
    val sensor = testKit.spawn(sensorActor)

    // test of the PropagateStatus case
    sendPropagateStatusMessage(sensor)

    for (_ <- 1 to 3) {
      Thread.sleep(interval.toMillis)
      testProbe.expectMessage(Status(sensor, DoubleDataType.defaultValue))
    }
    testKit.stop(sensor)

  private def testUpdateStatusWithTimedSensorActor(interval: FiniteDuration): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val timedSensor = new BasicSensor[Double]("1", List(testProbe.ref)) with Timer(interval)
    val sensorActor: Behavior[DeviceMessage] = timedSensor.behavior()
    val sensor = testKit.spawn(sensorActor)

    // test of the UpdateStatus case
    sendUpdateStatusMessage(sensor, 0.22)
    sendPropagateStatusMessage(sensor)

    for (_ <- 1 to 3) {
      Thread.sleep(interval.toMillis)
      testProbe.expectMessage(Status(sensor, 0.22))
    }
    testKit.stop(sensor)

  private def testSubscribeWithTimedSensorActor(interval: FiniteDuration): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val timedSensor = new BasicSensor[Double]("1", List(testProbe.ref)) with Timer(interval)
    val sensorActor: Behavior[DeviceMessage] = timedSensor.behavior()
    val sensor = testKit.spawn(sensorActor)

    // test of the Subscribe case
    sendSubscribeMessage(sensor)

    for (_ <- 1 to 3) {
      Thread.sleep(interval.toMillis)
      testProbe.expectMessage(Status(sensor, DoubleDataType.defaultValue))
    }
    testKit.stop(sensor)

  private def testUnsubscribeWithTimedSensorActor(interval: FiniteDuration): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val timedSensor = new BasicSensor[Double]("1", List(testProbe.ref)) with Timer(interval)
    val sensorActor: Behavior[DeviceMessage] = timedSensor.behavior()
    val sensor = testKit.spawn(sensorActor)

    // test of the Unsubscribe case
    sendUnsubscribeMessage(sensor)

    for (_ <- 1 to 3) {
      Thread.sleep(interval.toMillis)
      testProbe.expectMessage(Status(sensor, DoubleDataType.defaultValue))
    }
    testKit.stop(sensor)

  // SensorActor Tests
  private def testSensorActorReceiveMessage(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val sensorActor: Behavior[DeviceMessage] = new BasicSensor[Double]("1", List(testProbe.ref)).behavior()
    val sensor = testKit.spawn(sensorActor)

    // test of the PropagateStatus case
    sendPropagateStatusMessage(sensor)
    testProbe.expectMessage(Status(sensor, DoubleDataType.defaultValue))

    // test of the UpdateStatus case
    sendUpdateStatusMessage(sensor, 0.22)
    sendPropagateStatusMessage(sensor)
    testProbe.expectMessage(Status(sensor, 0.22))

    // test of the Subscribe case
    sendSubscribeMessage(sensor)
    testProbe.expectNoMessage()

    // test of the Unsubscribe case
    sendUnsubscribeMessage(sensor)
    testProbe.expectNoMessage()


  // Timed SensorActor Tests
  def testTimedSensorActorReceiveMessage(): Unit =
    val interval: FiniteDuration = FiniteDuration(2, "seconds")

    testPropagateStatusWithTimedSensorActor(interval)
    testUpdateStatusWithTimedSensorActor(interval)
    testSubscribeWithTimedSensorActor(interval)
    testUnsubscribeWithTimedSensorActor(interval)

  def testTimedSensorActorSendMessageAtFixedRate(): Unit =
    val testProbe = testKit.createTestProbe[Message]()
    val interval: FiniteDuration = FiniteDuration(2, "seconds")
    val timedSensor = new BasicSensor[Double]("1", List(testProbe.ref)) with Timer(interval)
    val sensorActor: Behavior[DeviceMessage] = timedSensor.behavior()
    val sensor = testKit.spawn(sensorActor)

    sendUpdateStatusMessage(sensor, String("test"))

    for (_ <- 1 to 3) {
      Thread.sleep(interval.toMillis)
      testProbe.expectMessage(Status(sensor, String("test")))
    }
    testKit.stop(sensor)


  ///BASIC SENSOR TESTS
  val testProbeBasic: TestProbe[Message] = testKit.createTestProbe[Message]()
  val sensorBasic = new BasicSensor[String]("1", List(testProbeBasic.ref))
  val sensorBasicActor: ActorRef[DeviceMessage] = testKit.spawn(SensorActor(sensorBasic).behavior())

  private def testPublicBasicSensorSendCorrect(): Unit =
    //test empty msg
    sendPropagateStatusMessage(sensorBasicActor)
    testProbeBasic.expectMessage(Status(sensorBasicActor, StringDataType.defaultValue))
    //test non-empty msg
    sendUpdateStatusMessage(sensorBasicActor, "test")
    sendPropagateStatusMessage(sensorBasicActor)
    testProbeBasic.expectMessage(Status(sensorBasicActor, "test"))

  private def testPublicBasicSensorStatusWrong(): Unit =
    //test updating with wrong type(need to be String for working)
    assertTypeError("sensor.update(sensorBasicActor, 5)")


  ///PROCESSED DATA SENSOR TESTS
  val testProbeProcessed: TestProbe[Message] = testKit.createTestProbe[Message]()
  val sensorProcessed = new ProcessedDataSensor[String, Int]("1", List(testProbeProcessed.ref), x => x.toInt)
  val sensorProcessedActor: ActorRef[DeviceMessage] = testKit.spawn(SensorActor(sensorProcessed).behavior())

  private def testPublicProcessedDataSensorSendCorrect(): Unit =
    //test empty msg
    sendPropagateStatusMessage(sensorProcessedActor)
    testProbeProcessed.expectMessage(Status(sensorProcessedActor, IntDataType.defaultValue))
    //test non-empty msg but Int it's converted in String
    sendUpdateStatusMessage(sensorProcessedActor, "5")
    sendPropagateStatusMessage(sensorProcessedActor)
    testProbeProcessed.expectMessage(Status(sensorProcessedActor, 5))

  private def testPublicProcessedDataSensorStatusWrong(): Unit =
    //test updating with wrong type(need to be Int for working)
    assertTypeError("sensor.update(sensorProcessedActor, 0.1)")

  //BASIC-CONDITION SENSOR TESTS
  val testProbeBasicCondition: TestProbe[Message] = testKit.createTestProbe[Message]()
  val sensorCondition = new BasicSensor[Int]("1", List(testProbeBasicCondition.ref)) with Condition[Int, Int](_ > 5, testProbeBasicCondition.ref)
  val sensorConditionActor: ActorRef[DeviceMessage] = testKit.spawn(SensorActor(sensorCondition).behavior())

  private def testBasicConditionSensorCorrect(): Unit =
    sensorCondition.update(sensorConditionActor, 6) //updating trigger the automatic send of the current status to the testProbe
    Thread.sleep(800)
    testProbeBasicCondition.expectMessage(Status(sensorConditionActor, 6))

  private def testBasicConditionSensorWrong(): Unit =
    sensorCondition.update(sensorConditionActor, 0)
    Thread.sleep(800)
    testProbeBasicCondition.expectNoMessage()

  //PROCESSED DATA-CONDITION SENSOR TESTS
  val testProbeProcessedCondition: TestProbe[Message] = testKit.createTestProbe[Message]()
  val sensorProcessedCondition = new ProcessedDataSensor[String, Int]("1", List(testProbeProcessedCondition.ref), x => x.toInt) with Condition[String, Int]((_.toString.contains("5")), testProbeProcessedCondition.ref)
  val sensorProcessedConditionActor: ActorRef[DeviceMessage] = testKit.spawn(SensorActor(sensorProcessedCondition).behavior())

  private def testProcessedConditionSensorCorrect(): Unit =
    sensorProcessedCondition.update(sensorProcessedConditionActor, "5") //updating trigger the automatic send of the current status to the testProbe
    Thread.sleep(800)
    testProbeProcessedCondition.expectMessage(Status(sensorProcessedConditionActor, 5))

  private def testProcessedConditionSensorWrong(): Unit =
    sensorProcessedCondition.update(sensorProcessedConditionActor, "0")
    Thread.sleep(800)
    testProbeProcessedCondition.expectNoMessage()

  //BASIC-TIMED SENSOR TESTS

  private def testBasicTimedSensorCorrect(): Unit =
    val testProbeBasicTimed: TestProbe[Message] = testKit.createTestProbe[Message]()
    val sensorBasicTimed = new BasicSensor[Int]("1", List(testProbeBasicTimed.ref))
    val sensorBasicTimedActor: ActorRef[DeviceMessage] = testKit.spawn(SensorActor(sensorBasicTimed).behaviorWithTimer(FiniteDuration(2, "second")))

    sensorBasicTimed.update(sensorBasicTimedActor, 5)

    for (_ <- 1 to 3) {
      Thread.sleep(2000)
      testProbeBasicTimed.expectMessage(Status(sensorBasicTimedActor, 5))
    }
    testKit.stop(sensorBasicTimedActor)

  private def testBasicTimedSensorWrong(): Unit =
    val testProbeBasicTimed: TestProbe[Message] = testKit.createTestProbe[Message]()
    val sensorBasicTimed = new BasicSensor[Int]("1", List(testProbeBasicTimed.ref))
    val sensorBasicTimedActor: ActorRef[DeviceMessage] = testKit.spawn(SensorActor(sensorBasicTimed).behaviorWithTimer(FiniteDuration(2, "second")))

    assertTypeError("sensorBasicTimed.update(sensorBasicTimedActor, 0.1)")

    for (_ <- 1 to 3) {
      Thread.sleep(2000)
      testProbeBasicTimed.expectMessage(Status(sensorBasicTimedActor, IntDataType.defaultValue))
    }
    testKit.stop(sensorBasicTimedActor)

  //PROCESSED DATA-TIMED SENSOR TESTS
  private def testProcessedTimedSensorCorrect(): Unit =
    val testProbeProcessedTimed: TestProbe[Message] = testKit.createTestProbe[Message]()
    val sensorProcessedTimed = new ProcessedDataSensor[String, Int]("1", List(testProbeProcessedTimed.ref), x => x.toInt)
    val sensorProcessedTimedActor: ActorRef[DeviceMessage] = testKit.spawn(SensorActor(sensorProcessedTimed).behaviorWithTimer(FiniteDuration(2, "second")))

    sensorProcessedTimed.update(sensorProcessedTimedActor, "2")

    for (_ <- 1 to 3) {
      Thread.sleep(2000)
      testProbeProcessedTimed.expectMessage(Status(sensorProcessedTimedActor, 2))
    }
    testKit.stop(sensorProcessedTimedActor)

  private def testProcessedTimedSensorWrong(): Unit =
    val testProbeProcessedTimed: TestProbe[Message] = testKit.createTestProbe[Message]()
    val sensorProcessedTimed = new ProcessedDataSensor[String, Int]("1", List(testProbeProcessedTimed.ref), x => x.toInt)
    val sensorProcessedTimedActor: ActorRef[DeviceMessage] = testKit.spawn(SensorActor(sensorProcessedTimed).behaviorWithTimer(FiniteDuration(2, "second")))

    assertTypeError("sensorProcessedTime.update(sensorProcessedTimedActor, 0.1)")

    for (_ <- 1 to 3) {
      Thread.sleep(2000)
      testProbeProcessedTimed.expectMessage(Status(sensorProcessedTimedActor, IntDataType.defaultValue))
    }
    testKit.stop(sensorProcessedTimedActor)


