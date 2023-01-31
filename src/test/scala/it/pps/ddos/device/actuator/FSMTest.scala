package it.pps.ddos.device.actuator

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.flatspec.AnyFlatSpec
import it.pps.ddos.device.DeviceProtocol.*

import scala.collection.immutable.ListMap

class FSMTest extends AnyFlatSpec:
  "A Finite State Machine" should "be the result of the -- operator between an instance of State[T] and T" in testDSLPartialCreation()
  "The syntax 'A: State[T] -- t:T -> B: State[T]' " should "return a Finite State Machine that returns the state B given FSM(A, t)" in testDSLCreation()
  "The user" should "be able to reuse a state as many times as they want" in testReusableState()
  "The user" should "not be able to define more than one destination for a specific tuple of (State, T), as showed" in testTupleOverride()
  "The result of the right-precedence _U operator" should "be the union of the two Finite State Machines" in testDSLUnion()

  private class FSMState(name: String) extends State[String](name):
    override def copy(): FSMState = new FSMState(name)
    override def getBehavior: Behavior[DeviceMessage] = Behaviors.empty

  private def testDSLPartialCreation(): Unit =
    val A: FSMState = new FSMState("A")
    assert((A -- "transition") == (new FSM[String](Option(A), Option("transition"), ListMap.empty)))

  private def testDSLCreation(): Unit =
    val A: FSMState = new FSMState("A")
    val B: FSMState = new FSMState("B")
    val fsm: FSM[String] = A -- "toB" -> B
    assert(fsm(A, "toB") == B)

  private def testReusableState(): Unit =
    val A: FSMState = new FSMState("A")
    val B: FSMState = new FSMState("B")
    val fsm: FSM[String] = A -- "fromAtoB" -> B -- "fromBtoA" -> A -- "fromAtoA" -> A
    assert(fsm(A, "fromAtoA") == A && fsm(B, "fromBtoA") == A)

  private def testTupleOverride(): Unit =
    val A: FSMState = new FSMState("A")
    val B: FSMState = new FSMState("B")
    val fsm: FSM[String] = A -- "overridedTransition" -> B -- "fromBtoA" -> A -- "overridedTransition" -> A
    assert(fsm(A, "overridedTransition") == A && fsm(A, "overridedTransition") != B)

  private def testDSLUnion(): Unit =
    val A: FSMState = new FSMState("A")
    val B: FSMState = new FSMState("B")
    val partialFsm: FSM[String] = A -- "transition" -> B
    assert((partialFsm _U B -- "transition" -> A) == A -- "transition" -> B -- "transition" -> A)