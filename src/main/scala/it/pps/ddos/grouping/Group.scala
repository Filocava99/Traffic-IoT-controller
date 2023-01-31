package it.pps.ddos.grouping

import scala.collection.immutable.Map
import scala.collection.immutable.List
import akka.actor.typed.ActorRef
import it.pps.ddos.device.Device
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.device.Public

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

type Actor = ActorRef[DeviceMessage]
type ActorList = List[ActorRef[DeviceMessage]]

/**
 * The Group abstract class defines the computation executed for the Devices aggregation.
 * @param id is the unique identifier for the Device in a cluster.
 * @param sources is the list of deployed devices that will become the Group inputs.
 * @param destinations is the list of actors to which send the result of the computation.
 * @tparam I is the input type of the computation.
 * @tparam O is the output type of the computation.
 */
abstract class Group[I, O](id: String, private val sources: ActorList, destinations: ActorList)
  extends Device[O](id, destinations) with Public[O] :
  protected var data: Map[Actor, List[I]] = Map.empty

  override def behavior(): Behavior[DeviceMessage] = Behaviors.unhandled

  def getSources(): ActorList = sources

  /**
   * Add a source's values to the computation input.
   * @param author is the identifier of the source.
   * @param newValues are the new values to add to the computation.
   */
  def insert(author: Actor, newValues: List[I]): Unit = data = data + (author -> newValues)

  /**
   * Remove all stored input values.
   */
  def reset(): Unit = data = Map.empty

  /**
   * Start the computation.
   */
  def compute(): Unit

  /**
   * Clone this instance of Group.
   * @return a new instance of Group parametrized in the same way.
   */
  def copy(): Group[I,O]

  private def canEqual(a: Any) = a.isInstanceOf[Group[_,_]]

  override def equals(that: Any): Boolean =
    that match
      case that: Group[_,_] => that.canEqual(this) &&
        this.hashCode == that.hashCode
      case _ => false

