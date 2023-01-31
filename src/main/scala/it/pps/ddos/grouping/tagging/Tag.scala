package it.pps.ddos.grouping.tagging

import akka.actor.typed.ActorRef
import it.pps.ddos.device.DeviceProtocol.Message
import it.pps.ddos.grouping.*

import scala.annotation.targetName
import scala.collection.immutable.List
import scala.util.Try

/**
 * The Tag trait is the factory Creator in group generator.
 * @param id is the unique string that will identify the Group's Device in the cluster.
 * @tparam I is the input type of the Group function.
 * @tparam O is the output type of the Group function.
 */
abstract class Tag[I,O](val id: String) extends Taggable:
  /**
   * Assign to each Taggable in input this Tag.
   * @param toTag is a Taggable element to which assign this Tag.
   */
  @targetName("addTagToDevices")
  def <--(toTag: Taggable*): Unit = for (taggable <- toTag) yield taggable ## this

  /**
   * Is the factory method that will generate the deployable Group instance.
   * @param sources is the list of akka ActorRef[Message] that will become the Group instance input sources.
   * @return a Group instance parametrized with the Tag fields and with the sources parameter actor list as source of input,
   */
  def generateGroup(sources: ActorList): Group[I,O]

/**
 * Concrete Creator in the factory pattern for the Group generation.
 */
case class MapTag[I,O](override val id: String, val dest: ActorList, val f: I => O, val tm: TriggerMode) extends Tag[I, List[O]](id):
  override def generateGroup(sources: ActorList): MapGroup[I,O] = new MapGroup(id, sources, dest, f) with Deployable[I,List[O]](tm)

/**
 * Concrete Creator in the factory pattern for the Group generation.
 */
case class ReduceTag[I,O](override val id: String, val dest: ActorList, val f: (O, I) => O, val neutralElem: O, val tm: TriggerMode) extends Tag[I,O](id):
  override def generateGroup(sources: ActorList): ReduceGroup[I,O] = new ReduceGroup(id, sources, dest, f, neutralElem) with Deployable[I,O](tm)

object Tag:
  /**
   * Creates a new Tag instance with the given parameters. A Tag is a factory generator for a particular Deployable Group instance.
   * This combination of parameters will create a Tag that will generate a MapGroup.
   * @param id is the unique id of the Group that will be generated.
   * @param dest is the initial list of destinations that the generated group will have.
   * @param f is the mapping function that will be applied to each element of the generated MapGroup.
   * @param tm is the trigger mode of the group.
   * @tparam I is the type of input of the group.
   * @tparam O is the type of output of the group.
   * @return a MapTag instance that will generate a MapGroup as result of generateGroup method.
   */
  def apply[I,O](id: String, dest: ActorList, f: I => O, tm: TriggerMode): MapTag[I,O] = new MapTag[I,O](id, dest: ActorList, f, tm)

  /**
   * Creates a new Tag instance with the given parameters. A Tag is a factory generator for a particular Deployable Group instance.
   * This combination of parameters will create a Tag that will generate a ReduceGroup.
   * @param id is the unique id of the Group that will be generated.
   * @param dest is the initial list of destinations that the generated group will have.
   * @param f is the mapping function that will be applied to aggregate the inputs.
   * @param neutralElem is the neutral element of the aggregation.
   * @param tm is the trigger mode of the group.
   * @tparam I is the type of input of the group.
   * @tparam O is the type of output of the group.
   * @return a ReduceTag instance that will generate a ReduceGroup as result of generateGroup method.
   */
  def apply[I,O](id: String, dest: ActorList, f: (O, I) => O, neutralElem: O, tm: TriggerMode): ReduceTag[I,O] = ReduceTag(id, dest: ActorList, f, neutralElem, tm)
