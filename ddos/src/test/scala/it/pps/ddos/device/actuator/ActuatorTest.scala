package it.pps.ddos.device.actuator

import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import akka.actor.typed.Behavior
import it.pps.ddos.device.actuator.BasicState
import org.scalatest.flatspec.AnyFlatSpec
import akka.actor.testkit.typed.scaladsl.ActorTestKit

import scala.collection.immutable.ListMap
import scala.concurrent.duration.Duration
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.device.Public
import it.pps.ddos.utils.GivenDataType.given

class ActuatorTest extends AnyFlatSpec:

    "An Actuator with basic states" should "be able to change state" in testBasicStateCorrectTransition()
    it should "not be able to change state if the transition is not defined" in testBasicStateWrongTransition()

    "An Actuator with conditional states" should "be able to change state" in testConditionalStateCorrectTransition()
    it should "not be able to change state if the transition is not defined" in testConditionalStateWrongTransition()
    it should "not be able to change state if the condition is not met" in testConditionalStateWrongCondition()

    "An Actuator with timed states" should "be able to change state" in testTimedStateCorrectTransition()
    it should "not change state if the timer has not finished" in testTimedStateWrongTransition()
    it should "should not change state if receives a MessageWithoutReply" in testTimedStateMessageWithoutReply()
    it should "should change state if receives a ForceStateChange" in testTimedStateForceStateChange()

    private def createTestBasicStateFSM(): FSM[String] =
        val stateA = BasicState[String]("A")
        val stateB = BasicState[String]("B")
        val stateC = BasicState[String]("C")
        var fsm = FSM[String](Option.empty, Option.empty, ListMap.empty)
        fsm = stateA -- "toB" -> stateB _U stateA -- "toC" -> stateC _U stateB -- "toC" -> stateC _U stateC -- "toA" -> stateA
        fsm

    private def createTestConditionalStateFSM(firstCond: ConditionalFunction[String]): FSM[String] =
        val stateA = ConditionalState[String]("A", firstCond)
        val stateB = ConditionalState[String]("B", (s: String, args: Seq[String]) => true)
        val stateC = ConditionalState[String]("C", (s: String, args: Seq[String]) => true)
        var fsm = FSM[String](Option.empty, Option.empty, ListMap.empty)
        fsm = stateA -- "toB" -> stateB _U stateA -- "toC" -> stateC _U stateB -- "toC" -> stateC _U stateC -- "toA" -> stateA
        fsm

    private def createTestTimedStateFSM(): FSM[String] =
        val stateA = TimedState[String]("A", Duration.create(3, "seconds"), () => ("toB", Seq.empty))
        val stateB = TimedState[String]("B", Duration.create(10, "seconds"), () => ("toC", Seq.empty))
        val stateC = TimedState[String]("C", Duration.create(10, "seconds"), () => ("toA", Seq.empty))
        var fsm = FSM[String](Option.empty, Option.empty, ListMap.empty)
        fsm = stateA -- "toB" -> stateB _U stateA -- "toC" -> stateC _U stateB -- "toC" -> stateC _U stateC -- "toA" -> stateA
        fsm

    val testKit: ActorTestKit = ActorTestKit()

    private def testBasicStateCorrectTransition(): Unit =
        val fsm = createTestBasicStateFSM()
        val actuator = new Actuator[String]("actuatorTest", fsm) with Public[String]
        val actuatorBehavior: Behavior[DeviceMessage] = actuator.behavior()
        val actuatorActor = testKit.spawn(actuatorBehavior)
        val testProbe = testKit.createTestProbe[Message]()
        actuatorActor ! Subscribe(testProbe.ref)
        Thread.sleep(200)
        testProbe.expectMessage(SubscribeAck(actuatorActor))
        Thread.sleep(200)
        actuatorActor ! MessageWithoutReply("toB", "test", "test")
        Thread.sleep(800)
        testProbe.expectMessage(Status(actuatorActor, "B"))

    private def testBasicStateWrongTransition(): Unit =
        val fsm = createTestBasicStateFSM()
        val actuator = new Actuator[String]("actuatorTest", fsm) with Public[String]
        val actuatorBehavior: Behavior[DeviceMessage] = actuator.behavior()
        val actuatorActor = testKit.spawn(actuatorBehavior)
        val testProbe = testKit.createTestProbe[Message]()
        actuatorActor ! Subscribe(testProbe.ref)
        Thread.sleep(200)
        testProbe.expectMessage(SubscribeAck(actuatorActor))
        Thread.sleep(200)
        actuatorActor ! MessageWithoutReply("toA")
        Thread.sleep(800)
        actuatorActor ! PropagateStatus(actuatorActor)
        Thread.sleep(800)
        testProbe.expectMessage(Status(actuatorActor, "A"))

    private def testConditionalStateCorrectTransition(): Unit =
        val fsm = createTestConditionalStateFSM((s: String, args: Seq[String]) => args.size == 2)
        val actuator = new Actuator[String]("actuatorTest", fsm) with Public[String]
        val actuatorBehavior: Behavior[DeviceMessage] = actuator.behavior()
        val actuatorActor = testKit.spawn(actuatorBehavior)
        val testProbe = testKit.createTestProbe[Message]()
        actuatorActor ! Subscribe(testProbe.ref)
        Thread.sleep(200)
        testProbe.expectMessage(SubscribeAck(actuatorActor))
        Thread.sleep(200)
        actuatorActor ! MessageWithoutReply("toB", "a", "b")
        Thread.sleep(800)
        testProbe.expectMessage(Status(actuatorActor, "B"))

    private def testConditionalStateWrongTransition(): Unit =
        val fsm = createTestConditionalStateFSM((s: String, args: Seq[String]) => args.size == 2)
        val actuator = new Actuator[String]("actuatorTest", fsm) with Public[String]
        val actuatorBehavior: Behavior[DeviceMessage] = actuator.behavior()
        val actuatorActor = testKit.spawn(actuatorBehavior)
        val testProbe = testKit.createTestProbe[Message]()
        actuatorActor ! Subscribe(testProbe.ref)
        Thread.sleep(200)
        testProbe.expectMessage(SubscribeAck(actuatorActor))
        Thread.sleep(200)
        actuatorActor ! MessageWithoutReply("toA", "a", "b")
        Thread.sleep(800)
        actuatorActor ! PropagateStatus(actuatorActor)
        Thread.sleep(800)
        testProbe.expectMessage(Status(actuatorActor, "A"))

    private def testConditionalStateWrongCondition(): Unit =
        val fsm = createTestConditionalStateFSM((s: String, args: Seq[String]) => args.size == 2)
        val actuator = new Actuator[String]("actuatorTest", fsm) with Public[String]
        val actuatorBehavior: Behavior[DeviceMessage] = actuator.behavior()
        val actuatorActor = testKit.spawn(actuatorBehavior)
        val testProbe = testKit.createTestProbe[Message]()
        actuatorActor ! Subscribe(testProbe.ref)
        Thread.sleep(200)
        testProbe.expectMessage(SubscribeAck(actuatorActor))
        Thread.sleep(200)
        actuatorActor ! MessageWithoutReply("toB", "a")
        Thread.sleep(800)
        actuatorActor ! PropagateStatus(actuatorActor)
        Thread.sleep(800)
        testProbe.expectMessage(Status(actuatorActor, "A"))

    private def testTimedStateCorrectTransition(): Unit =
        val fsm = createTestTimedStateFSM()
        val actuator = new Actuator[String]("actuatorTest", fsm) with Public[String]
        val actuatorBehavior: Behavior[DeviceMessage] = actuator.behavior()
        val actuatorActor = testKit.spawn(actuatorBehavior)
        val testProbe = testKit.createTestProbe[Message]()
        actuatorActor ! Subscribe(testProbe.ref)
        Thread.sleep(200)
        testProbe.expectMessage(SubscribeAck(actuatorActor))
        Thread.sleep(200)
        actuatorActor ! PropagateStatus(actuatorActor)
        Thread.sleep(800)
        testProbe.expectMessage(Status(actuatorActor, "A"))
        Thread.sleep(3000)
        testProbe.expectMessage(Status(actuatorActor, "B"))

    private def testTimedStateWrongTransition(): Unit =
        val fsm = createTestTimedStateFSM()
        val actuator = new Actuator[String]("actuatorTest", fsm) with Public[String]
        val actuatorBehavior: Behavior[DeviceMessage] = actuator.behavior()
        val actuatorActor = testKit.spawn(actuatorBehavior)
        val testProbe = testKit.createTestProbe[Message]()
        actuatorActor ! Subscribe(testProbe.ref)
        Thread.sleep(200)
        testProbe.expectMessage(SubscribeAck(actuatorActor))
        Thread.sleep(200)
        actuatorActor ! PropagateStatus(actuatorActor)
        Thread.sleep(800)
        testProbe.expectMessage(Status(actuatorActor, "A"))
        Thread.sleep(800)
        actuatorActor ! PropagateStatus(actuatorActor)
        Thread.sleep(800)
        testProbe.expectMessage(Status(actuatorActor, "A"))

    private def testTimedStateMessageWithoutReply(): Unit =
        val fsm = createTestTimedStateFSM()
        val actuator = new Actuator[String]("actuatorTest", fsm) with Public[String]
        val actuatorBehavior: Behavior[DeviceMessage] = actuator.behavior()
        val actuatorActor = testKit.spawn(actuatorBehavior)
        val testProbe = testKit.createTestProbe[Message]()
        actuatorActor ! Subscribe(testProbe.ref)
        Thread.sleep(200)
        testProbe.expectMessage(SubscribeAck(actuatorActor))
        Thread.sleep(200)
        actuatorActor ! PropagateStatus(actuatorActor)
        Thread.sleep(500)
        testProbe.expectMessage(Status(actuatorActor, "A"))
        Thread.sleep(500)
        actuatorActor ! MessageWithoutReply("toB")
        Thread.sleep(500)
        actuatorActor ! PropagateStatus(actuatorActor)
        Thread.sleep(500)
        testProbe.expectMessage(Status(actuatorActor, "A"))

    private def testTimedStateForceStateChange(): Unit =
        val fsm = createTestTimedStateFSM()
        val actuator = new Actuator[String]("actuatorTest", fsm) with Public[String]
        val actuatorBehavior: Behavior[DeviceMessage] = actuator.behavior()
        val actuatorActor = testKit.spawn(actuatorBehavior)
        val testProbe = testKit.createTestProbe[Message]()
        actuatorActor ! Subscribe(testProbe.ref)
        Thread.sleep(200)
        testProbe.expectMessage(SubscribeAck(actuatorActor))
        Thread.sleep(200)
        actuatorActor ! PropagateStatus(actuatorActor)
        Thread.sleep(500)
        testProbe.expectMessage(Status(actuatorActor, "A"))
        Thread.sleep(500)
        actuatorActor ! ForceStateChange("toB")
        Thread.sleep(500)
        actuatorActor ! PropagateStatus(actuatorActor)
        Thread.sleep(500)
        testProbe.expectMessage(Status(actuatorActor, "B"))