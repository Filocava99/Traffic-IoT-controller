package it.pps.ddos.device.actuator

import scala.annotation.targetName
import scala.collection.immutable.ListMap

/**
 * Class that models a final state machine in which the subclasses of Message are the transitions and the subclasses of State are the states.
 * @param fromState the state from which start the transition
 * @param fromMessage the transition to add to the state
 * @param map the current map of (state -> transition)
 * @tparam T the generic type of the state
 */
case class FSM[T](val fromState: Option[State[T]], val fromMessage: Option[T], val map: ListMap[(State[T], T), State[T]]) :
  /**
   * define the destination for this transition.
   * @param s is the destination state for this transition.
   * @return a new FSM with the completed current transition and the final state of that transition as the initial state for a new transition.
   */
  @targetName("addTransition")
  def ->(s: State[T]): FSM[T] = fromMessage match
    case None => FSM(Option(s), fromMessage, map)
    case _ => FSM(Option(s), Option.empty, map ++ ListMap((fromState.get, fromMessage.get) -> s))

  /**
   * Define a transition from the current state.
   * @param m is the new transition from the current state.
   * @return a new FSM with a partial transition that miss the destination state.
   */
  @targetName("addMessage")
  def --(m: T): FSM[T] = FSM(fromState, Option(m), map)

  /**
   * Right-precedence joins two FSMs together, keeping the right-er partial defined transition.
   * @param fsm is the FSM to join.
   * @return a new FSM with the union of all transitions and the right FSM partial transition.
   */
  @targetName("joinWith")
  def _U(fsm: FSM[T]): FSM[T] = FSM(fsm.fromState, fsm.fromMessage, map ++ fsm.map)
  
  def apply(s: State[T], m: T): State[T] = map.getOrElse((s,m), s)
  
  override def toString: String = map.toString
  
  def getInitialState: State[T] = map.head._1._1
