package it.pps.ddos.device.actuator
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.pps.ddos.device.DeviceProtocol.*

import scala.annotation.targetName
import scala.collection.immutable.{HashMap, ListMap}

type ConditionalFunction[T] = (T, Seq[T]) => Boolean

object ConditionalState {
    def apply[T](name: String, condFunc: ConditionalFunction[T]): ConditionalState[T] = new ConditionalState[T](name, condFunc)
}

class ConditionalState[T](name: String, condFunction: ConditionalFunction[T]) extends State[T](name):

    private val behavior: Behavior[DeviceMessage] = Behaviors.receiveMessage[DeviceMessage] { msg =>
        msg match
            case MessageWithReply(msg: T, replyTo, args: _*) =>
                if condFunction(msg, args.asInstanceOf[Seq[T]]) then
                    replyTo ! Approved()
                else
                    replyTo ! Denied()
            case _ =>
        Behaviors.same
    }

    override def getBehavior: Behavior[DeviceMessage] = behavior

    override def copy(): State[T] = ConditionalState(name, condFunction)