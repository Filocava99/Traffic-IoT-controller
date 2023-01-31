package it.pps.ddos.grouping.tagging

import akka.actor.typed.Behavior
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message}
import it.pps.ddos.grouping.{BlockingGroup, Group, NonBlockingGroup}

/**
 * Trait that overrides Group behavior method to guarantee deployment compatibility.
 * @tparam I is the input type of the Group.
 * @tparam O is the output type of the Group.
 */
trait Deployable[I, O](tm: TriggerMode) extends Group[I, O] :
  override def behavior(): Behavior[DeviceMessage] = tm match
    case TriggerMode.BLOCKING => BlockingGroup(this)
    case TriggerMode.NONBLOCKING => NonBlockingGroup(this)

  override def hashCode(): Int = super.hashCode()

/**
 * Is the enum that represents currently implemented activation modes usable in the factory pattern for Group generation.
 */
enum TriggerMode:
  case BLOCKING extends TriggerMode
  case NONBLOCKING extends TriggerMode
