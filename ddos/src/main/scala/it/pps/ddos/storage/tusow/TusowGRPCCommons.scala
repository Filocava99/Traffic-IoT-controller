package it.pps.ddos.storage.tusow

import it.unibo.coordination.linda.text.RegularMatch
import it.unibo.coordination.tusow.grpc.{IOResponse, IOResponseList, Tuple, TuplesList}

import java.util.concurrent.CompletableFuture
import scala.concurrent.{ExecutionContext, Future}
import scala.compat.java8.FutureConverters._

object TusowGRPCCommons {
    def joinFutures[T](futures: Seq[CompletableFuture[T]])(implicit executor: ExecutionContext): Unit = {
        Future.sequence(futures.map(_.toScala)).onComplete(_ => ())
        //futures.foldLeft(CompletableFuture.allOf(futures.head))(CompletableFuture.allOf(_, _)).get()
    }

    def processReadOrTakeAllFutures[A](futures: Seq[CompletableFuture[A]])(result: A => Tuple)(implicit executor: ExecutionContext): Future[TuplesList] = {
        var tuplesList = scala.Seq[Tuple]()
        futures.foldLeft(CompletableFuture.allOf(futures.head))(CompletableFuture.allOf(_, _)).toScala.map(_ => {
            futures.foreach(f => {
                tuplesList = tuplesList :+ result(f.get())
            })
            TuplesList(tuplesList)
        })
    }

    def processWriteAllFutures[A](futures: Seq[CompletableFuture[A]])(value: A => String)(implicit executor: ExecutionContext): Future[IOResponseList] = {
        var ioResponseList = scala.Seq[IOResponse]()
        Future.sequence(futures.map(f => f.toScala)).map(_ => {
            futures.foreach(f => {
                ioResponseList = ioResponseList :+ IOResponse(response = true, message = f.get().toString)
            })
            IOResponseList(ioResponseList)
        })
    }
    
    
}
