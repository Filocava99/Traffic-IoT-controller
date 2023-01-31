package it.pps.ddos

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import it.pps.ddos.device.DeviceProtocol.DeviceMessage
import it.pps.ddos.storage.tusow.{Server, TusowBinder}

object LaunchTusowService:

  private final val sys = ActorSystem(Behaviors.empty[DeviceMessage], "ClusterSystem")

  def main(args: Array[String]): Unit =
    Server.start()
    TusowBinder(sys)
  


