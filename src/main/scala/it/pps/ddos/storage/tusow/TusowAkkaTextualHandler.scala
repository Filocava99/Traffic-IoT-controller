package it.pps.ddos.storage.tusow

import akka.NotUsed
import akka.stream.scaladsl.Source
import it.unibo.coordination.linda.core.TupleSpace
import it.unibo.coordination.linda.text.{RegexTemplate, RegularMatch, StringTuple, TextualSpace}
import it.unibo.coordination.tusow.grpc.*
import it.unibo.coordination.tusow.grpc.ReadOrTakeRequest.Template.TextualTemplate

import java.util.concurrent.{CompletableFuture, TimeUnit}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.compat.java8.FutureConverters.*

object TusowAkkaTextualHandler:
    def apply(): TusowAkkaTextualHandler = new TusowAkkaTextualHandler()

class TusowAkkaTextualHandler extends TusowService:

    type TextualSpace = TupleSpace[StringTuple, RegexTemplate, Object, String, RegularMatch]
    private val timeout = 10
    private val textualSpaces = new scala.collection.mutable.HashMap[String, TextualSpace]

    override def validateTupleSpace(in: TupleSpaceID): Future[IOResponse] = Future.successful(IOResponse(textualSpaces.contains(in.id)))

    override def createTupleSpace(in: TupleSpaceID): Future[IOResponse] =
        textualSpaces(in.id) = TextualSpace.local(in.id)
        Future.successful(IOResponse(response = true))

    override def write(in: WriteRequest): Future[IOResponse] =
        val space = textualSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id)
        handleFutureRequest(space)(() => Future.successful(IOResponse(response = false, message = "Tuple space not found")))(() => space.write(in.tuple.get.value).toScala.map(f => IOResponse(response = true, message = f.toString)))

    override def read(in: ReadOrTakeRequest): Future[Tuple] =
       handleReadOrTakeRequest(in)((space, template, timeout) => space.read(RegexTemplate.of(template)).orTimeout(timeout, TimeUnit.SECONDS).toScala.map(t => Tuple(t.getTemplate.toString, t.getTuple.orElse(StringTuple.of("")).getValue)))

    override def take(in: ReadOrTakeRequest): Future[Tuple] =
        handleReadOrTakeRequest(in)((space, template, timeout) => space.take(template).orTimeout(timeout, TimeUnit.SECONDS).toScala.map(t => Tuple(t.getTemplate.toString, t.getTuple.orElse(StringTuple.of("")).getValue)))


    override def writeAll(in: WriteAllRequest): Future[IOResponseList] =
        val space = textualSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id)
        handleFutureRequest(space)(() => Future.failed(new IllegalArgumentException("Tuple space does not exist")))(() =>
            val futures = in.tuplesList.get.tuples.map(t => space.write(t.value))
            TusowGRPCCommons.processWriteAllFutures(futures)(f => f.getValue)
        )

    override def readAll(in: ReadOrTakeAllRequest): Future[TuplesList] =
        handleReadOrTakeAllRequest(in)((space, templates, timeout) =>
            val futures = templates.map(space.read(_).orTimeout(timeout, TimeUnit.SECONDS))
            processReadOrTakeAllFutures(futures)
        )

    override def takeAll(in: ReadOrTakeAllRequest): Future[TuplesList] =
        handleReadOrTakeAllRequest(in)((space, templates, timeout) =>
            val futures = templates.map(space.take(_).orTimeout(timeout, TimeUnit.SECONDS))
            processReadOrTakeAllFutures(futures)
        )

    override def writeAllAsStream(in: WriteAllRequest): Source[IOResponse, NotUsed] =
        textualSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id) match
            case null => Source.empty
            case space@_ =>
                val futures = in.tuplesList.get.tuples.map(t => space.write(t.value))
                TusowGRPCCommons.joinFutures(futures)
                Source(futures.map(f => IOResponse(response = true, message = f.get.toString)).toList)

    override def readAllAsStream(in: ReadOrTakeAllRequest): Source[Tuple, NotUsed] =
        textualSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id) match
            case null => Source.empty
            case space@_ =>
                val futures = in.templates.textualTemplateList.get.regexes.map(t => space.read(t.regex))
                processStreamFutures(futures)

    override def takeAllAsStream(in: ReadOrTakeAllRequest): Source[Tuple, NotUsed] =
        textualSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id) match
            case null => Source.empty
            case space@_ =>
                processStreamFutures(in.templates.textualTemplateList.get.regexes.map(t => space.take(t.regex)))

    private def processStreamFutures(futures: Seq[CompletableFuture[RegularMatch]]): Source[Tuple, NotUsed] =
        TusowGRPCCommons.joinFutures(futures)
        Source(futures.map(f => Tuple(f.get().getTemplate.toString, f.get().getTuple.get().getValue)).toList)

    private def handleReadOrTakeAllRequest[A](in: ReadOrTakeAllRequest)(readOrTake: (TextualSpace, Seq[String], Long) => Future[A]): Future[A] =
        val space = textualSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id)
        handleFutureRequest(space)(() => Future.failed(new IllegalArgumentException("Tuple space does not exist")))(() =>
            readOrTake(space, in.templates.textualTemplateList.get.regexes.map(regex => regex.regex), timeout)
        )

    private def processReadOrTakeAllFutures(futures: Seq[CompletableFuture[RegularMatch]]): Future[TuplesList] =
        TusowGRPCCommons.processReadOrTakeAllFutures(futures)(f => Tuple(f.getTemplate.toString, f.getTuple.get().getValue))

    private def handleFutureRequest[A](space: TextualSpace)(failureHandler: () => Future[A])(successHandler: () => Future[A]): Future[A] =
        if (space == null)
            failureHandler()
        else
            successHandler()

    private def handleReadOrTakeRequest[A](in: ReadOrTakeRequest)(readOrTake: (TextualSpace, String, Long) => Future[A]): Future[A] =
        val space = textualSpaces(in.tupleSpaceID.getOrElse(TupleSpaceID("")).id)
        handleFutureRequest(space)(() => Future.failed(new IllegalArgumentException("Tuple space not found")))(() => readOrTake(space, in.template.textualTemplate.get.regex, timeout))