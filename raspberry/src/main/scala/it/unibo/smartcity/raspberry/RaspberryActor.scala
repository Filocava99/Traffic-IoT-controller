package it.unibo.smartcity.raspberry

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Timeout, AddSource}
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.utils.DataType
import it.sc.server.{IdAnswer, IdRequest}
import it.sc.server.entities.{Camera, RecordedData}
import com.github.nscala_time.time.Imports.DateTime
import org.bson.types.ObjectId

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}

given RecordedDataType: DataType[RecordedData] with
  override def defaultValue: RecordedData = RecordedData(new ObjectId().toHexString, DateTime.now().getMillis, Map.empty[Int, Int])

object RaspberryActor:
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
            case IdAnswer(id: String) =>
              timer.cancel("connectingStateTimer")
              Thread.sleep(5000)
              val broadcasterRef = Deployer.getActorRefViaReceptionist("broadcaster-" + id)
              Thread.sleep(5000)
              val storingRef = Deployer.getActorRefViaReceptionist("storing")
              Thread.sleep(5000)
              Deployer.deploy(new StoreDataSensor[RecordedData]("raspberry-" + id, List(broadcasterRef, storingRef), x => x))
              Thread.sleep(5000)
              val sensorRef = Deployer.getActorRefViaReceptionist("raspberry-" + id)
              Thread.sleep(5000)
              broadcasterRef ! AddSource(sensorRef)
              Thread.sleep(5000)
              Slave(sensorRef, id)
              Thread.sleep(5000)
              Behaviors.same
        }
      }
    }

