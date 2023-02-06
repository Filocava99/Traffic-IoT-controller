package it.pps.ddos.deployment.graph

import akka.actor.typed
import akka.actor.typed.Behavior
import it.pps.ddos.device.actuator.Actuator

import scala.::
import scala.annotation.targetName
import scala.collection.immutable
import scala.language.postfixOps

object Graph:

    def apply[T](): Graph[T] = new Graph[T](None, Map.empty)

    def apply[T](edges: (T, T)*): Graph[T] =
        new Graph(edges.head.getFirst, edges.foldLeft(Map[T, List[T]]())((map, edge) => addEdge(map, edge)))

    private def addEdge[T](edges: Map[T, List[T]], edge: (T, T)): Map[T, List[T]] = edges.filter(_._1 != edge._1) ++ Map[T, List[T]]((edge._1, edges.getOrElse(edge._1, List.empty[T]) :+ edge._2))

    extension [T](t: (T, T))
        def getFirst: Option[T] = if(t != null && t._1 != null) Some(t._1) else None
        def getSecond: Option[T] = if(t != null && t._2 != null) Some(t._2) else None

/**
 * Usage example:
 * Graph[String](
 *   "A" -> "B",
 *   "A" -> "C",
 *   "B" -> "D",
 *   "C" -> "D",
 *   "D" -> "A"
 *  )
 *
 * @param initialNode the entry point of the graph
 * @param edges all the edges from a Node to a list of nodes
 * @tparam T
 */
case class Graph[T](private val initialNode: Option[T], private var edges: Map[T, List[T]]):
    /**
     * Shortcut for edges.getOrElse
     * @param node the node from which you want to get edges
     * @return a list of edges from the node. Empty list in case of no edges
     */
    def ?-> (node: T): List[T] = edges.getOrElse(node, List.empty)

    /**
     * Foreach-like method. It applies a function f to every tuple of the edges map
     * @param f the function to apply to every tuple of the edges
     */
    def @-> (f: (T, List[T]) => Unit): Unit = edges foreach (x => f(x._1, x._2))

    def isEmpty: Boolean = edges.isEmpty

    def nonEmpty: Boolean = edges.nonEmpty

    def size: Int = edges.size

    def ? (node: T): Boolean = edges.contains(node)

    def ++ (edge: (T, T)): Unit = edges = Graph.addEdge(edges, edge)

    def getNodes(): Set[T] = edges.keys.toSet ++ edges.values.flatten.toSet