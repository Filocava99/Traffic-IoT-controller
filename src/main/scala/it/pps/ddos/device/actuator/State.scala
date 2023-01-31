package it.pps.ddos.device.actuator

import akka.actor.typed.Behavior
import it.pps.ddos.device.DeviceProtocol.*

import scala.collection.immutable.ListMap

trait State[T](val name: String):
    def getBehavior: Behavior[DeviceMessage]

    def --(message: T) = new FSM[T](Option(this), Option(message), ListMap[(State[T], T), State[T]]())

    def copy(): State[T]