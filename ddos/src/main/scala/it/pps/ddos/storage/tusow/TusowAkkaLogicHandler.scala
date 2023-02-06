package it.pps.ddos.storage.tusow

import akka.NotUsed
import akka.stream.scaladsl.Source
import it.unibo.coordination.linda.core.TupleSpace
import it.unibo.coordination.linda.logic.{LogicMatch, LogicSpace, LogicTemplate, LogicTuple}
import it.unibo.coordination.tusow.grpc
import it.unibo.coordination.tusow.grpc.{IOResponse, IOResponseList, ReadOrTakeAllRequest, ReadOrTakeRequest, Template, Tuple, TupleSpaceID, TuplesList, TusowService, WriteAllRequest, WriteRequest}
import it.unibo.tuprolog.core.Term

import scala.compat.java8.FutureConverters.*
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, TimeUnit}

import java.util.concurrent.{CompletableFuture, TimeUnit}

object TusowAkkaLogicHandler:
    def apply(): TusowAkkaLogicHandler = new TusowAkkaLogicHandler()


//noinspection DuplicatedCode
class TusowAkkaLogicHandler extends TusowService:

    type LogicSpace = TupleSpace[LogicTuple, LogicTemplate, String, Term, LogicMatch]
    private val logicSpaces = new scala.collection.mutable.HashMap[String, LogicSpace]
    private val timeout = 10

    override def validateTupleSpace(in: TupleSpaceID): Future[IOResponse] = Future.successful(IOResponse(logicSpaces.contains(in.id)))

    override def createTupleSpace(in: TupleSpaceID): Future[IOResponse] =
        logicSpaces(in.id) = LogicSpace.local(in.id)
        Future.successful(IOResponse(response = true))

    override def write(in: WriteRequest): Future[IOResponse] =
        val space = logicSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id)
        handleFutureRequest(space)(() => Future.successful(IOResponse(response = false, message = "Tuple space not found")))(() => space.write(in.tuple.get.value).toScala.map(f => IOResponse(response = true, message = f.toString)))

    override def read(in: ReadOrTakeRequest): Future[Tuple] =
        val space = logicSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id)
        handleReadOrTakeRequest(in)((space, template, timeout) => space.read(in.template.logicTemplate.getOrElse(Template.Logic()).query)
          .toScala.map(logicMatch => Tuple(key = logicMatch.getTemplate.toString, value = logicMatch.getTuple.orElse(LogicTuple.of("")).getValue.toString)))

    override def take(in: ReadOrTakeRequest): Future[Tuple] =
        val space = logicSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id)
        if (space == null)
            Future.successful(null)
        else
            space.take(in.template.logicTemplate.getOrElse(Template.Logic()).query).toScala.map(logicMatch => Tuple(key = logicMatch.getTemplate.toString, value = logicMatch.getTuple.orElse(LogicTuple.of("")).getValue.toString))

    override def writeAll(in: WriteAllRequest): Future[IOResponseList] =
        val space = logicSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id)
        handleFutureRequest(space)(() => Future.failed(new IllegalArgumentException("Tuple space does not exist")))(() =>
            val futures = in.tuplesList.get.tuples.map(t => space.write(t.value))
            TusowGRPCCommons.processWriteAllFutures(futures)(f => f.getValue.toString)
        )

    override def readAll(in: ReadOrTakeAllRequest): Future[TuplesList] =
        val space = logicSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id)
        handleReadOrTakeAllRequest(in)((space, templates, timeout) =>
            val futures = templates.map(space.read(_).orTimeout(timeout, TimeUnit.SECONDS))
            processReadOrTakeAllFutures(futures)
        )

    override def takeAll(in: ReadOrTakeAllRequest): Future[TuplesList] =
        val space = logicSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id)
        handleReadOrTakeAllRequest(in)((space, templates, timeout) =>
            val futures = templates.map(space.take(_).orTimeout(timeout, TimeUnit.SECONDS))
            processReadOrTakeAllFutures(futures)
        )

    override def writeAllAsStream(in: WriteAllRequest): Source[IOResponse, NotUsed] =
        logicSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id) match
            case null => Source.empty
            case space@_ =>
                val futures = in.tuplesList.get.tuples.map(t => space.write(t.value))
                TusowGRPCCommons.joinFutures(futures)
                Source(futures.map(f => IOResponse(response = true, message = f.get.toString)).toList)

    override def readAllAsStream(in: ReadOrTakeAllRequest): Source[Tuple, NotUsed] =
        logicSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id) match
            case null => Source.empty
            case space@_ =>
                val futures = in.templates.logicTemplateList.get.queries.map(t => space.read(t.query))
                processStreamFutures(futures)

    override def takeAllAsStream(in: ReadOrTakeAllRequest): Source[Tuple, NotUsed] =
        logicSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id) match
            case null => Source.empty
            case space@_ =>
                val futures = in.templates.logicTemplateList.get.queries.map(t => space.take(t.query))
                processStreamFutures(futures)

    private def processStreamFutures(futures: Seq[CompletableFuture[LogicMatch]]): Source[Tuple, NotUsed] =
        TusowGRPCCommons.joinFutures(futures)
        Source(futures.map(f => Tuple(f.get().getTemplate.toString, f.get().getTuple.get().toString)).toList)

    private def handleReadOrTakeAllRequest[A](in: ReadOrTakeAllRequest)(readOrTake: (LogicSpace, Seq[String], Long) => Future[A]): Future[A] =
        val space = logicSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id)
        handleFutureRequest(space)(() => Future.failed(new IllegalArgumentException("Tuple space does not exist")))(() =>
            readOrTake(space, in.templates.logicTemplateList.get.queries.map(query => query.query), timeout)
        )

    private def processReadOrTakeAllFutures(futures: Seq[CompletableFuture[LogicMatch]]): Future[TuplesList] =
        TusowGRPCCommons.processReadOrTakeAllFutures(futures)(f => Tuple(f.getTemplate.toString, f.getTuple.get().toString))

    private def handleFutureRequest[A](space: LogicSpace)(failureHandler: () => Future[A])(successHandler: () => Future[A]): Future[A] =
        if (space == null)
            failureHandler()
        else
            successHandler()

    private def handleReadOrTakeRequest[A](in: ReadOrTakeRequest)(readOrTake: (LogicSpace, String, Long) => Future[A]): Future[A] =
        val space = logicSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id)
        handleFutureRequest(space)(() => Future.failed(new IllegalArgumentException("Tuple space not found")))(() => readOrTake(space, in.template.logicTemplate.getOrElse(Template.Logic.of("")).query, timeout))