package it.pps.ddos.grouping

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import it.pps.ddos.device.DeviceBehavior
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message, Subscribe, SubscribeAck, Timeout}

import scala.collection.immutable.List
import scala.concurrent.duration.FiniteDuration

/**
 * This trait represents the reactive part of the Group, defining how the Group manage input gathering and when its computation is triggered.
 * This actor acts as a finite state machine, with "connecting" as initial state and "active" as final state once the Group has
 * successfully subscribed to all its input sources.
 */
trait GroupActor:
  /**
   * Return a new group actor with the given Group instance as actor state.
   * @param g is the Group instance that will define the computation of this group.
   * @return a new group actor behavior in the "connecting" state.
   */
  def apply(g: Group[_,_]): Behavior[DeviceMessage] =
    Behaviors.setup[DeviceMessage] {
      context =>
        g.getSources().foreach(_ ! Subscribe(context.self))
        connecting(g.getSources(), g.copy())
    }

  /**
   * is the state in which the group actor is trying to subscribe to all its sources, waiting for all their SubscribeAcks.
   * @param sources are the sources that still don't has responded.
   * @param g is the actor state.
   * @return the corresponding behavior.
   */
  protected def connecting(sources: ActorList, g: Group[_,_]): Behavior[DeviceMessage] =
    Behaviors.withTimers[DeviceMessage] { timer =>
      timer.startTimerAtFixedRate("connectingStateTimer", Timeout(), FiniteDuration(3, "second"))
      Behaviors.receivePartial { (context, message) =>
        (message, sources) match
          case (Timeout(), _) =>
            sources.foreach(_ ! Subscribe(context.self))
            Behaviors.same
          case (SubscribeAck(author), sources) if sources.length > 1 =>
            connecting(sources.filter(_ != author), g)
          case (SubscribeAck(author), sources) if sources.contains(author) =>
            timer.cancel("connectingStateTimer")
            active(g.getSources(), g, context)
          case _ =>
            Behaviors.same
      }
    }

  /**
   * is the state in wich the group actor is waiting for sources inputs to compute.
   * @param sources is the list of sources of this group. This list determines the Devices which messages are evaluated.
   * @param g is the group actor state.
   * @param context is the context of the group actor.
   * @return the corresponding behavior.
   */
  protected def active(sources: ActorList, g: Group[_,_], context: ActorContext[DeviceMessage]): Behavior[DeviceMessage] =
    Behaviors.receiveMessagePartial(getTriggerBehavior(context, g, sources).orElse(DeviceBehavior.getBasicBehavior(g, context)))

  /**
   * is the method to override to provide a trigger strategy for the inpur gathering and computation timing.
   * @param context is the context of the group actor.
   * @param g is the group actor state that specify what to do when the compute method is called.
   * @param sources is the list of sources of this group.
   * @tparam I is the input type of the computation.
   * @tparam O is the output type of the computation.
   * @return a partial function that defines a behavior that will be prepended to the standard Device communication protocol.
   */
  protected def getTriggerBehavior[I,O](context: ActorContext[DeviceMessage],
                              g: Group[I,O],
                              sources: ActorList): PartialFunction[DeviceMessage, Behavior[DeviceMessage]]
