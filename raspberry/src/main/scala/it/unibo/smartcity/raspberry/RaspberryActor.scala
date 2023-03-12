package it.unibo.smartcity.raspberry

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Timeout}
import it.pps.ddos.device.sensor.StoreDataSensor
import it.sc.server.{IdAnswer, IdRequest}
import it.sc.server.entities.{Camera, RecordedData}
import reactivemongo.api.bson.BSONObjectID
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}

object ServerActor:
  def apply(details: String): Behavior[DeviceMessage] =
    Behaviors.setup[DeviceMessage] { context =>
      Behaviors.withTimers[DeviceMessage] { timer =>
        timer.startTimerAtFixedRate("connectingStateTimer", Timeout(), FiniteDuration(3, "second"))
        Behaviors.receivePartial { (context, message) =>
          message match
            case Timeout() =>
              val serverRef = Deployer.getActorRefViaReceptionist("server")
              serverRef ! IdRequest(details, context.self)
              Behaviors.same
            case IdAnswer(id: BSONObjectID) =>
              timer.cancel("connectingStateTimer")
              Thread.sleep(3000)
              val broadcasterRef = Deployer.getActorRefViaReceptionist("broadcaster-"+id.stringify)
              Deployer.deploy(new StoreDataSensor[RecordedData]("raspberry-"+id.stringify, List(broadcasterRef)))
              Thread.sleep(3000)
              val sensorRef = Deployer.getActorRefViaReceptionist("raspberry-"+id.stringify)
              Slave(sensorRef, id)
              Behaviors.same
        }
      }
    }

