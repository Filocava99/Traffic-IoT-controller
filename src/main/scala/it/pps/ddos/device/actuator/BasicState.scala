package it.pps.ddos.device.actuator

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import it.pps.ddos.device.DeviceProtocol.*

import scala.annotation.targetName
import scala.collection.immutable.{HashMap, ListMap}

object BasicState:
    def apply[T](name: String): BasicState[T] = new BasicState(name)

class BasicState[T](name: String) extends State[T](name):
    private val behavior: Behavior[DeviceMessage] = Behaviors.receiveMessage[DeviceMessage] { msg =>
        msg match
            case MessageWithReply(msg: T, replyTo, args: _*) =>
                replyTo ! Approved()
                Behaviors.same
            case Stop() => Behaviors.stopped
            case _ => Behaviors.same
    }

    override def getBehavior: Behavior[DeviceMessage] = behavior

    override def copy(): State[T] = BasicState[T](name)