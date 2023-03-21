import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, Behavior}
import com.github.nscala_time.time.Imports.*
import it.sc.server.entities.RecordedData
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message, Status, Subscribe, SubscribeAck, Unsubscribe, UnsubscribeAck}
import it.pps.ddos.device.Timer
import it.pps.ddos.device.sensor.BasicSensor
import org.scalatest.flatspec.AnyFlatSpec
import scalafx.scene.control.Button
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.*

class ClientActorTest extends AnyFlatSpec:
  val testKit: ActorTestKit = ActorTestKit()
  val testProbe = testKit.createTestProbe[DeviceMessage]()

  "A GUI" should " be initialized via main" in {
    assertCompiles("ClientView()")
  }

  "A ClientActor " should "receive the status " in testClientActorReceiveStatus()
  "A ClientActor " should "receive the subscribe ack " in testClientActorReceiveSubscribeAck()
  "A ClientActor " should "receive the unsubscribe ack " in testClientActorReceiveUnsubscribeAck()

  def testClientActorReceiveStatus(): Unit =
    val testProbe = testKit.createTestProbe[DeviceMessage]()
    val clientActor: Behavior[DeviceMessage] = ClientActor().behavior()
    val client = testKit.spawn(clientActor)

    // sending a specific DBEntry via the Status message
    client ! Status(testProbe.ref, RecordedData("1", DateTime.now(), Map.empty[Int, Int]))
    Thread.sleep(1000)
    // expecting a response with the Subscribe message
    testProbe.expectMessageType[Subscribe[DeviceMessage]]

    // sending a DBEntry with the same ID of the first one via the Status message
    Thread.sleep(2000)
    client ! Status(testProbe.ref, RecordedData("1", DateTime.now(), Map.empty[Int, Int]))
    // expecting no messages back because of the ID is the same
    testProbe.expectNoMessage()

    // sending a DBEntry with a different ID from the first one via the Status message
    Thread.sleep(2000)
    client ! Status(testProbe.ref, RecordedData("2", DateTime.now(), Map.empty[Int, Int]))
    // expecting a response with the Subscribe message because of the ID is different
    testProbe.expectMessageType[Subscribe[DeviceMessage]]

  def testClientActorReceiveSubscribeAck(): Unit =
    val testProbe = testKit.createTestProbe[DeviceMessage]()
    val clientActor: Behavior[DeviceMessage] = ClientActor().behavior()
    val client = testKit.spawn(clientActor)

    client ! SubscribeAck(testProbe.ref)
    Thread.sleep(1000)
    testProbe.expectNoMessage()

  def testClientActorReceiveUnsubscribeAck(): Unit =
    val testProbe = testKit.createTestProbe[DeviceMessage]()
    val clientActor: Behavior[DeviceMessage] = ClientActor().behavior()
    val client = testKit.spawn(clientActor)

    client ! UnsubscribeAck(testProbe.ref)
    Thread.sleep(1000)
    testProbe.expectNoMessage()
