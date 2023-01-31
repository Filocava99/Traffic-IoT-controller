package it.pps.ddos.deployment

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.{Config, ConfigFactory}
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message, Subscribe}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.typed.{Cluster, Join}
import com.typesafe.config.{Config, ConfigFactory}
import it.pps.ddos.deployment.graph.Graph
import it.pps.ddos.device.Device
import it.pps.ddos.grouping.ActorList
import it.pps.ddos.grouping.tagging.Tag

import scala.annotation.tailrec
import scala.collection.{immutable, mutable}
import scala.language.postfixOps

object Deployer:

  private final val DEFAULT_PORT = "0"
  private final val HOSTNAME =  "127.0.0.1"
  private final val SEED_NODES = immutable.List[String]("2551","2552")
  private case class ActorSysWithActor(actorSystem: ActorSystem[InternSpawn], numberOfActorSpawned: Int)

  private case class InternSpawn(id: String, behavior: Behavior[_ <: Message])

  private val orderedActorSystemRefList = mutable.ListBuffer.empty[ActorSysWithActor]
  
  private var devicesActorRefMap = Map.empty[String, ActorRef[DeviceMessage]]

  private val deviceServiceKey = ServiceKey[DeviceMessage]("DeviceService")

  def getDevicesActorRefMap: Map[String, ActorRef[DeviceMessage]] =
    devicesActorRefMap

  /**
   * Initialize the seed nodes and the cluster
   */
  def initSeedNodes(): Unit =
    ActorSystem(Behaviors.empty[DeviceMessage], "ClusterSystem", setupClusterConfig("2551"))
    ActorSystem(Behaviors.empty[DeviceMessage], "ClusterSystem", setupClusterConfig("2552"))

  /**
   * Add N nodes to the cluster
   * @param numberOfNode the number of nodes to add
   */
  def addNodes(numberOfNode: Int): Unit =
    for (i <- 1 to numberOfNode)
        val as = createActorSystem("ClusterSystem")
        Thread.sleep(300)
        orderedActorSystemRefList += ActorSysWithActor(as, 0)

  private def createActorSystem(id: String): ActorSystem[InternSpawn] =
    ActorSystem(Behaviors.setup(
      context =>
        Behaviors.receiveMessage { msg =>
          msg match
            case InternSpawn(id, behavior: Behavior[DeviceMessage]) =>
              val ar: ActorRef[DeviceMessage] = context.spawn(behavior, id)
              devicesActorRefMap = Map((id, ar)) ++ devicesActorRefMap
              context.system.receptionist ! Receptionist.Register(deviceServiceKey, ar)
              Behaviors.same
        }
    ), id, setupClusterConfig(DEFAULT_PORT))

  /**
   * Deploy the graph on the cluster
   * @param graph the graph to deploy
   */
  def deploy[T](devices: Device[T]*): Unit =
    devices.foreach(dev =>
      val actorRefWithInt = orderedActorSystemRefList.filter(_.actorSystem.ref == getMinSpawnActorNode).head
      actorRefWithInt.actorSystem.ref ! InternSpawn(dev.id, dev.behavior())
      actorRefWithInt.numberOfActorSpawned + 1
    )

  /**
   * Deploys a graph of devices into the cluster.
   * Every edge of the graph represents a device that subscribes to another device.
   * Each device is deployed on the node with the minimum number of deployed devices.
   * @param devicesGraph The graph of devices to deploy
   * @tparam T The type of messages exchanged between devices
   */
  def deploy[T](devicesGraph: Graph[Device[T]]): Unit =
    var alreadyDeployed = mutable.Set[Device[T]]()
    devicesGraph @-> ((k,edges) =>
      if(!alreadyDeployed.contains(k)){
        deploy(k)
        alreadyDeployed += k
      }
      edges.filter(!alreadyDeployed.contains(_)).foreach { e =>
        deploy(e)
        alreadyDeployed += e
      }
    )
    devicesGraph @-> ((k, v) => v.map(it => devicesActorRefMap.get(it.id)).filter(_.isDefined).foreach(device => devicesActorRefMap(k.id).ref ! Subscribe(device.get.ref)))
    val tagList = retrieveTagSet(devicesGraph.getNodes())
    deployGroups(tagList.groupMap((tag, id) => tag)((tag, id) => id))

  private def setupClusterConfig(port: String): Config =
    val hostname = HOSTNAME
    ConfigFactory.parseString(String.format("akka.remote.artery.canonical.hostname = \"%s\"%n", hostname)
      + String.format("akka.remote.artery.canonical.port=" + port + "%n")
      + String.format("akka.management.http.hostname=\"%s\"%n",hostname)
      + String.format("akka.management.http.port=" + port.replace("255", "855") + "%n")
      + String.format("akka.management.http.route-providers-read-only=%s%n", "false")
      + String.format("akka.remote.artery.advanced.tcp.outbound-client-hostname=%s%n", hostname)
      + String.format("akka.cluster.jmx.multi-mbeans-in-same-jvm = on"))
      .withFallback(ConfigFactory.load("application.conf"))

  private def getMinSpawnActorNode: ActorRef[InternSpawn] =
    orderedActorSystemRefList.minBy(x => x.numberOfActorSpawned).actorSystem

  @tailrec
  private def deployGroups[T](groups: Map[Tag[_,_], Set[String]] = Map.empty): Unit =
    var deployedTags = Set.empty[Tag[_,_]]
    groups.isEmpty match
      case false =>
        for {
          (tag, sourceSet) <- groups.map((tag, sources) => (tag, sources.map(id => devicesActorRefMap get id))) if !sourceSet.contains(None)
        } yield {
          deploy(tag.generateGroup(sourceSet.toList.map(opt => opt.get)))
          deployedTags = deployedTags + tag
        }
        deployGroups(groups -- deployedTags)
      case true =>

  private def retrieveTagSet[T](devicesList: Set[Device[T]]): Set[(Tag[_,_], String)] =
    val nodeTags = for {
      n <- devicesList
      t <- n.getTags()
    } yield (t -> n.id)
    nodeTags ++ exploreInnerTags(nodeTags.map((t,id) => t).toSet, Set.empty)

  @tailrec
  private def exploreInnerTags(newTags: Set[Tag[_,_]], accumulator: Set[(Tag[_,_], String)] = Set.empty): Set[(Tag[_,_], String)] =
    newTags.isEmpty match
      case true => accumulator
      case false =>
        val tagTags = for {
          t <- newTags
          nestedTag <- t.getTags() if !accumulator.contains((nestedTag -> t.id))
        } yield (nestedTag -> t.id)
        exploreInnerTags(
          tagTags.map((t, id) => t).diff(accumulator.map((tag, id) => tag)),
          accumulator ++ tagTags
        )