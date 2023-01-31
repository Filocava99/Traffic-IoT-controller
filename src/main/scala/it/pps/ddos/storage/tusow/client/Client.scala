package it.pps.ddos.storage.tusow.client

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import it.unibo.coordination.tusow.TupleSpaceTypes
import it.unibo.coordination.tusow.grpc.{ReadOrTakeRequest, Template, Tuple, TupleSpaceID, TupleSpaceType, TusowServiceClient, WriteRequest}

import scala.concurrent.ExecutionContextExecutor

object Client:

    def createClient(actorSystem: ActorSystem, executionContextExecutor: ExecutionContextExecutor): TusowServiceClient =
        implicit val system: ActorSystem = actorSystem
        implicit val ec: ExecutionContextExecutor = executionContextExecutor
        val clientSettings: GrpcClientSettings = GrpcClientSettings.connectToServiceAt("127.0.0.1", 8080).withTls(false)
        TusowServiceClient(clientSettings)