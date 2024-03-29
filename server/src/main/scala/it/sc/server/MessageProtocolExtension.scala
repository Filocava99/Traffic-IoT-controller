package it.sc.server

import akka.actor.typed.ActorRef
import com.github.nscala_time.time.Imports.DateTime
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message}

case class IdRequest(details: String, replyTo: ActorRef[DeviceMessage]) extends DeviceMessage
case class IdAnswer(id: String) extends DeviceMessage