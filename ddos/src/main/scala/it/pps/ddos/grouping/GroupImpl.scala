package it.pps.ddos.grouping

import akka.actor.typed.ActorRef
import it.pps.ddos.device.Device
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message, Statuses}

import scala.collection.immutable.List

/**
 * Concrete class that implements a way to compute the output of a list of Devices.
 * ReduceGroup will perform a reduction operation on the input values, as specified by the f field.
 * @param id is the unique identifier for the Device in a cluster.
 * @param sources is the list of deployed devices that will become the Group inputs.
 * @param destinations is the list of actors to which send the result of the computation.
 * @param f is the reduction function that will be applied th the stored input values when the compute method is called.
 * @param neutralElem is the reduction starting (and neutral) element.
 * @tparam I is the input type of the computation.
 * @tparam O is the output type of the computation.
 */
class ReduceGroup[I, O](id: String, sources: ActorList, destinations: ActorList, val f: (O, I) => O, val neutralElem: O)
  extends Group[I, O](id, sources, destinations) :
  override def compute(): Unit =
    status = Option(data.values.flatten.toList.foldLeft(neutralElem)(f))

  override def copy(): ReduceGroup[I, O] = new ReduceGroup(id, sources, destinations, f, neutralElem)

  override def hashCode(): Int =
    id.hashCode() + sources.hashCode() + destinations.hashCode() + f.hashCode() + neutralElem.hashCode()

private trait MultipleOutputs[O]:
  self: Device[List[O]] =>
  override def propagate(selfId: ActorRef[DeviceMessage], requester: ActorRef[DeviceMessage]): Unit = status match
    case Some(value) => for (actor <- destinations) actor ! Statuses[O](selfId, value)
    case None =>

/**
 * Concrete class that implements a way to compute the output of a list of Devices.
 * MapGroup will perform a transformation (mapping) on the input values, as specified by the f field.
 * @param id is the unique identifier for the Device in a cluster.
 * @param sources is the list of deployed devices that will become the Group inputs.
 * @param destinations is the list of actors to which send the result of the computation.
 * @param f is the mapping function that will be applied to each element of the input list, obtaining an output element of type O.
 * @tparam I is the input type of the computation.
 * @tparam O is the output type of the computation.
 */
class MapGroup[I, O](id: String, sources: ActorList, destinations: ActorList, val f: I => O)
  extends Group[I, List[O]](id, sources, destinations) with MultipleOutputs[O] :
  override def compute(): Unit =
    status = Option(
      for {
        list <- data.values.toList
        elem <- list
      } yield f(elem)
    )

  override def copy(): MapGroup[I,O] = new MapGroup(id, sources, destinations, f)

  override def hashCode(): Int =
    id.hashCode() + sources.hashCode() + destinations.hashCode() + f.hashCode()
