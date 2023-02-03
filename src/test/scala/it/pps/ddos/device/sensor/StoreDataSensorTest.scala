package it.pps.ddos.device.sensor

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.pps.ddos.device.DeviceBehavior
import it.pps.ddos.device.DeviceBehavior.Tick
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message, ReceivedAck, SensorMessage, Status, UpdateStatus}
import it.pps.ddos.device.{Device, DeviceProtocol, Public, Timer}
import org.scalatest.flatspec.AnyFlatSpec
import it.pps.ddos.utils.GivenDataType.given

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

class StoreDataSensorTest extends AnyFlatSpec:

  "A StoreDataSensor " should "be able to store new received data" in testStoringDataInSensor()
  "A StoreDataSensor " should "be able to send data to the database every N seconds" in testSendDataToDatabase()
  "A StoreDataSensor " should
    "be able to update its stored data after receiving the ack message from the database" in testUpdateStoredDataInSensorAfterAck()
  "A StoreDataSensor " should "be able to send multiple data to the database and receive an ack message from it in order to update data" in testStoreDataSensorBehavior()

  val testKit = ActorTestKit()

  /*class StoreDataSensor(id: String, refs: List[ActorRef[DeviceMessage]], duration: FiniteDuration)
    extends BasicSensor[Double](id, refs) with Timer(duration):
    private case object StoreDataSensorKey
    var statusList = List.empty[Double]

    override def update(selfId: ActorRef[SensorMessage], physicalInput: Double): Unit =
      super.update(selfId, physicalInput)
      statusList = preProcess(physicalInput) :: statusList

    override def behavior(): Behavior[DeviceMessage] =
      Behaviors.setup { context =>
        Behaviors.withTimers { timer =>
          timer.startTimerWithFixedDelay(StoreDataSensorKey, Tick, duration)
          Behaviors.receiveMessagePartial(
            DeviceBehavior.getBasicBehavior(this, context)
              .orElse(DeviceBehavior.getTimedBehavior(this, context))
              .orElse({
                case UpdateStatus(value: Double) =>
                  this.update(context.self, value)
                  Behaviors.same
                case ReceivedAck(values) =>
                  statusList = statusList.filter(p => !values.contains(p))
                  Behaviors.same
              })
          )
        }
      }*/

  def testStoringDataInSensor(): Unit =
    val testProbe = testKit.createTestProbe[Message]()

    val interval: FiniteDuration = FiniteDuration(3, "seconds")
    val storeDataSensor = new StoreDataSensor[Double]("storage", List(testProbe.ref), interval)
    val storeDataSensorActor = testKit.spawn(storeDataSensor.behavior())

    var randNumb = 0.0
    var listRandNumb: List[Double] = List.empty

    for (_ <- 0 to 4)
      // generate new data to store
      randNumb = Random.nextDouble() * 100

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! UpdateStatus(randNumb)
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      Thread.sleep(interval.toMillis)
      testProbe.expectMessage(Status(storeDataSensorActor, randNumb))

    assertResult(listRandNumb)(storeDataSensor.storedStatus)
    println("Stored values: " + storeDataSensor.storedStatus)

    testKit.stop(storeDataSensorActor)


  def testSendDataToDatabase(): Unit =
    val testProbe = testKit.createTestProbe[Message]()

    val interval: FiniteDuration = FiniteDuration(3, "seconds")
    val storeDataSensor = new StoreDataSensor[Double]("storage", List(testProbe.ref), interval)
    val storeDataSensorActor = testKit.spawn(storeDataSensor.behavior())

    var randNumb = 0.0

    for (_ <- 0 to 4)
      // generate new data to store
      randNumb = Random.nextDouble() * 100

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! UpdateStatus(randNumb)
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      Thread.sleep(interval.toMillis)
      testProbe.expectMessage(Status(storeDataSensorActor, randNumb))

    testKit.stop(storeDataSensorActor)

  def testUpdateStoredDataInSensorAfterAck(): Unit =
    val testProbe = testKit.createTestProbe[Message]()

    val interval: FiniteDuration = FiniteDuration(3, "seconds")
    val storeDataSensor = new StoreDataSensor[Double]("storage", List(testProbe.ref), interval)
    val storeDataSensorActor = testKit.spawn(storeDataSensor.behavior())

    var randNumb = 0.0
    var listRandNumb: List[Double] = List.empty

    for (_ <- 0 to 4)
      // generate new data to store
      randNumb = Random.nextDouble() * 100

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! UpdateStatus(randNumb)
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      Thread.sleep(interval.toMillis)
      testProbe.expectMessage(Status(storeDataSensorActor, randNumb))

    assertResult(listRandNumb)(storeDataSensor.storedStatus)
    println("Stored values: " + storeDataSensor.storedStatus)

    // decide to remove the first two detected data
    listRandNumb = listRandNumb.slice(0, 2)
    println("Values to remove " + listRandNumb)

    // send the ack message from the database including the data to remove
    storeDataSensorActor ! ReceivedAck(listRandNumb)
    Thread.sleep(500)

    println("Stored values after the ack message from the database: " + storeDataSensor.storedStatus)

    testKit.stop(storeDataSensorActor)

  def testStoreDataSensorBehavior(): Unit =
    val testProbe = testKit.createTestProbe[Message]()

    val interval: FiniteDuration = FiniteDuration(3, "seconds")
    val storeDataSensor = new StoreDataSensor[Double]("storage", List(testProbe.ref), interval)
    val storeDataSensorActor = testKit.spawn(storeDataSensor.behavior())

    var randNumb = 0.0
    var listRandNumb: List[Double] = List.empty

    for (_ <- 0 to 4)
      // generate new data to store
      randNumb = Random.nextDouble() * 100

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! UpdateStatus(randNumb)
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      Thread.sleep(interval.toMillis)
      testProbe.expectMessage(Status(storeDataSensorActor, randNumb))

    assertResult(listRandNumb)(storeDataSensor.storedStatus)
    println("Stored values: " + storeDataSensor.storedStatus)

    // decide to remove the first two detected data
    listRandNumb = listRandNumb.slice(0, 2)
    println("Values to remove " + listRandNumb)

    // send the ack message from the database including the data to remove
    storeDataSensorActor ! ReceivedAck(listRandNumb)
    Thread.sleep(500)

    println("Stored values after the ack message from the database: " + storeDataSensor.storedStatus)

    for (_ <- 0 to 4)
      // generate new data to store
      randNumb = Random.nextDouble() * 100

      // save data in another variable for test purpose
      listRandNumb = randNumb :: listRandNumb

      // update the sensor status and sending the new data to the database for storing
      storeDataSensorActor ! UpdateStatus(randNumb)
      // storeDataSensor.update(storeDataSensorActor, randNumb) // other way to do the same thing

      Thread.sleep(interval.toMillis)
      testProbe.expectMessage(Status(storeDataSensorActor, randNumb))

    //assertResult(listRandNumb)(storeDataSensor.storedStatus)
    println("Stored values: " + storeDataSensor.storedStatus)

    // decide to remove the first two detected data
    listRandNumb = listRandNumb.slice(0, 2)
    println("Values to remove " + listRandNumb)

    // send the ack message from the database including the data to remove
    storeDataSensorActor ! ReceivedAck(listRandNumb)
    Thread.sleep(500)

    println("Stored values after the ack message from the database: " + storeDataSensor.storedStatus)

    testKit.stop(storeDataSensorActor)