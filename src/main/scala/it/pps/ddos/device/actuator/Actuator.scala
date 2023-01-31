package it.pps.ddos.device.actuator

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import it.pps.ddos.device.DeviceBehavior.Tick
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.device.actuator.Actuator.TimedActuatorKey
import it.pps.ddos.device.{Device, DeviceBehavior}

import scala.annotation.targetName
import scala.collection.immutable.ArraySeq
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.postfixOps

object Actuator:
    private case object TimedActuatorKey
    def apply[T](id: String, fsm: FSM[T]): Actuator[T] = new Actuator[T](id, fsm)

class Actuator[T](id: String, val FSM: FSM[T], destinations: ActorRef[DeviceMessage]*) extends Device[String](id, destinations.toList):
    private var currentState: State[T] = FSM.getInitialState
    this.status = Some(currentState.name)
    private var pendingState: Option[State[T]] = None

    private var utilityActor: ActorRef[DeviceMessage] = null
    println(s"Initial state ${FSM.getInitialState.name}")

    override def behavior(): Behavior[DeviceMessage] = Behaviors.setup[DeviceMessage] { context =>
      utilityActor = spawnUtilityActor(context)
      if (currentState.isInstanceOf[LateInit]) utilityActor ! SetActuatorRef(context.self)
      Behaviors.receiveMessagePartial(basicActuatorBehavior(context)
        .orElse(DeviceBehavior.getBasicBehavior(this, context)))
    }

    def behaviorWithTimer(duration: FiniteDuration): Behavior[DeviceMessage] =
        Behaviors.setup[DeviceMessage] { context =>
          Behaviors.withTimers { timer =>
            timer.startTimerWithFixedDelay(TimedActuatorKey, Tick, duration)
            Behaviors.receiveMessagePartial(basicActuatorBehavior(context)
            .orElse(DeviceBehavior.getBasicBehavior(this, context))
            .orElse(DeviceBehavior.getTimedBehavior(this, context)))
          }
        }

    private def basicActuatorBehavior(context: ActorContext[DeviceMessage]): PartialFunction[DeviceMessage, Behavior[DeviceMessage]] = { message =>
      println(message)
      message match
        case MessageWithoutReply(msg: T, args: _*) =>
          messageWithoutReply(msg, context, args.asInstanceOf[Seq[T]])
          Behaviors.same
        case Approved() =>
          utilityActor = approved(context)
          Behaviors.same
        case Denied() =>
          denied()
          Behaviors.same
        case ForceStateChange(transition: T) =>
          utilityActor = forceStateChange(context, transition)
          Behaviors.same
        case Subscribe(requester: ActorRef[DeviceMessage]) =>
          subscribe(context.self, requester)
          Behaviors.same
        case PropagateStatus(requester: ActorRef[DeviceMessage]) =>
          propagate(context.self, requester)
          Behaviors.same
    }

    private def approved(context: ActorContext[DeviceMessage]): ActorRef[DeviceMessage] =
        if (pendingState.isDefined)
            currentState = pendingState.get
            this.status = Some(currentState.name)
            println("propagation")
            propagate(context.self, context.self)
            pendingState = None
            utilityActor ! Stop()
            spawnUtilityActor(context)
        else
            println("No pending state")
            utilityActor

    private def denied(): Unit =
        pendingState = None

    private def messageWithoutReply(msg: T, context: ActorContext[_ <: Message], args: Seq[T]): Behavior[DeviceMessage] =
        println(FSM.map.contains((currentState, msg)) && !currentState.isInstanceOf[LateInit])
        if (FSM.map.contains((currentState, msg)) && !currentState.isInstanceOf[LateInit])
            FSM.map((currentState, msg)) match
                case state =>
                    pendingState = Some(state)
                    utilityActor ! MessageWithReply(msg, context.self.asInstanceOf[ActorRef[DeviceMessage]], args*)
                case null =>
        else println("No action found for this actuator message")
        Behaviors.same

    private def forceStateChange(context: ActorContext[DeviceMessage], transition: T): ActorRef[DeviceMessage] =
        currentState = FSM.map((currentState, transition))
        this.status = Some(currentState.name)
        utilityActor ! Stop()
        propagate(context.self, context.self)
        spawnUtilityActor(context)

    private def spawnUtilityActor(context: ActorContext[DeviceMessage]) = context.spawn(currentState.getBehavior, s"utilityActor-${java.util.UUID.randomUUID.toString}")
