package it.pps.ddos.deployment.graph

import org.scalatest.flatspec.AnyFlatSpec

class GraphTest extends AnyFlatSpec:

    "A graph" should "be created" in {
        val graph = Graph.apply()
        assert(graph != null)
    }

    it should "be empty" in {
        val graph = Graph.apply()
        assert(graph.isEmpty)
    }

    it should "have a node" in {
        val graph = Graph[String]("A" -> "B")
        assert(graph.size == 1)
    }

    it should "have two nodes" in {
        val graph = Graph[String]("A" -> "B")
        graph ++ ("B" -> "C")
        assert(graph.size == 2)
    }

    it should "have three nodes" in {
        val graph = Graph[String](
            "A" -> "B",
            "B" -> "C",
            "C" -> "D"
        )
        assert(graph.size == 3)
    }

    it should "should return true when using ? with an existing node" in {
        val graph = Graph[String](
            "A" -> "B",
            "B" -> "C",
            "C" -> "D",
            "D" -> "E"
        )
        assert(graph ? "A")
    }

    it should "should return false when using ? with a non existing node" in {
        val graph = Graph[String](
            "A" -> "B",
            "B" -> "C",
            "C" -> "D",
            "D" -> "E"
        )
        assert(!(graph ? "Z"))
    }

    it should "return an empty list when fetching the neighbors of a non existing node" in {
        val graph = Graph[String](
            "A" -> "B",
            "B" -> "C",
            "C" -> "D",
            "D" -> "E"
        )
        assert((graph ?-> "Z").isEmpty)
    }

    it should "return a list of nodes when fetching the neighbors of an existing node" in {
        val graph = Graph[String](
            "A" -> "B",
            "B" -> "C",
            "A" -> "D",
            "D" -> "E"
        )
        assert((graph ?-> "A").size == 2)
    }

    it should "apply a function to each node when using @-> (foreach)" in {
        val graph = Graph[String](
            "A" -> "B",
            "B" -> "C",
            "C" -> "D",
            "D" -> "E"
        )
        var counter = 0
        graph @-> ( (_, _) => counter += 1 )
        assert(counter == 4)
    }