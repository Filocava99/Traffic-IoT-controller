package it.pps.ddos.storage.tusow

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.{Config, ConfigFactory}
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.deployment.graph.Graph
import it.pps.ddos.device.{Device, DeviceProtocol, Public, Timer}
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message, PropagateStatus, Status, Statuses, Subscribe}
import it.pps.ddos.device.sensor.BasicSensor
import it.pps.ddos.storage.tusow.client.Client
import it.unibo.coordination.tusow.grpc.{IOResponse, ReadOrTakeRequest, Template, Tuple, TupleSpaceID, TupleSpaceType, TusowServiceClient, WriteRequest}
import it.pps.ddos.utils.GivenDataType.given

import java.io.File
import java.util.concurrent.TimeUnit
import scala.::
import scala.collection.mutable.*
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration.Duration

/**
 * Actor that allows sensor data of an ActorSystem to be stored in a distributed manner via tuple spaces
 */
object TusowBinder:
  private final val TUSOW_SYSTEM_NAME = "ddos-tusow-storage"
  private final val key = ServiceKey[DeviceMessage]("DeviceService")
  private final val STANDARD_WAIT = 2500
  private var latestResponse: Option[IOResponse] = None
  /**
   * This method is used to create a new TusowBinder actor
   * @param sys the configuration of the actor system
   */
  def apply(sys: ActorSystem[DeviceMessage]): Unit =
    implicit val ec: ExecutionContextExecutor = sys.classicSystem.dispatcher

    val tupleSpace = new TupleSpaceID(TUSOW_SYSTEM_NAME, TupleSpaceType.LOGIC)
    Thread.sleep(STANDARD_WAIT)
    var actorList = List.empty[ActorRef[DeviceMessage]]
    val client = Client.createClient(sys.classicSystem, ec)

    client.createTupleSpace(tupleSpace).onComplete(response =>
      sys.systemActorOf(Behaviors.setup({context =>
        context.system.receptionist ! Receptionist.Subscribe(key, context.self)
        Behaviors.receiveMessage { msg =>
          msg match
            case Status(ref, value) =>
              val tuple = new Tuple(TUSOW_SYSTEM_NAME, s"data('${ref.toString}', '${value.toString}').")
              write(client,tuple,tupleSpace)
              Behaviors.same
            case Statuses(ref, value) =>
              val tuple = new Tuple(TUSOW_SYSTEM_NAME, s"data('${ref.toString}', '${value.foreach(_.toString)}').")
              write(client,tuple,tupleSpace)
              Behaviors.same
            case key.Listing(listings) =>
              for {act <- listings} yield {
                actorList.contains(act) match
                  case false =>
                    act ! DeviceProtocol.Subscribe(context.self.asInstanceOf[ActorRef[DeviceProtocol.DeviceMessage]])
                    actorList = act :: actorList
                  case _ =>
              }
              Behaviors.same
            case _ =>
              Behaviors.same
        }
      }), "TusowActor")
    )
  
  private def write(client: TusowServiceClient,tuple: Tuple,tupleSpace: TupleSpaceID): IOResponse =
    val writeResponse = Await.result[IOResponse](client.write(new WriteRequest(Some(tupleSpace), Some(tuple))), Duration(STANDARD_WAIT, TimeUnit.MILLISECONDS))
    latestResponse = Option.apply(writeResponse)
    writeResponse
  
  def getLatestResponse: Option[IOResponse] = latestResponse
  
/* Runnable TusowBinder actor for debug */
//object TusowBinderMain extends App:
//TusowBinder()








