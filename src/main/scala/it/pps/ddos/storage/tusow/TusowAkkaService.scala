package it.pps.ddos.storage.tusow

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import it.unibo.coordination.linda.core.TupleSpace
import it.unibo.coordination.linda.logic.{LogicMatch, LogicTemplate, LogicTuple}
import it.unibo.coordination.linda.text.{RegexTemplate, RegularMatch, StringTuple}
import it.unibo.coordination.tusow.grpc.{IOResponse, IOResponseList, ReadOrTakeAllRequest, ReadOrTakeRequest, Tuple, TupleSpaceID, TupleSpaceType, TuplesList, TusowService, WriteAllRequest, WriteRequest}
import it.unibo.tuprolog.core.Term

import scala.concurrent.Future
import scala.language.implicitConversions

object TusowAkkaService:
    def apply(system: ActorSystem[_]): TusowAkkaService = new TusowAkkaService(system)

class TusowAkkaService(val system: ActorSystem[_]) extends TusowService:
    private val logicHandler: TusowAkkaLogicHandler = TusowAkkaLogicHandler()
    private val textualHandler: TusowAkkaTextualHandler = TusowAkkaTextualHandler()

    override def validateTupleSpace(in: TupleSpaceID): Future[IOResponse] = in.`type` match
        case TupleSpaceType.LOGIC => logicHandler.validateTupleSpace(in)
        case TupleSpaceType.TEXTUAL => textualHandler.validateTupleSpace(in)

    override def createTupleSpace(in: TupleSpaceID): Future[IOResponse] = in.`type` match
        case TupleSpaceType.LOGIC => logicHandler.createTupleSpace(in)
        case TupleSpaceType.TEXTUAL => textualHandler.createTupleSpace(in)

    override def write(in: WriteRequest): Future[IOResponse] = in.tupleSpaceID.get.`type` match
        case TupleSpaceType.LOGIC => logicHandler.write(in)
        case TupleSpaceType.TEXTUAL => textualHandler.write(in)

    override def read(in: ReadOrTakeRequest): Future[Tuple] = in.tupleSpaceID.get.`type` match
        case TupleSpaceType.LOGIC => logicHandler.read(in)
        case TupleSpaceType.TEXTUAL => textualHandler.read(in)

    override def take(in: ReadOrTakeRequest): Future[Tuple] = in.tupleSpaceID.get.`type` match
        case TupleSpaceType.LOGIC => logicHandler.take(in)
        case TupleSpaceType.TEXTUAL => textualHandler.take(in)

    override def writeAll(in: WriteAllRequest): Future[IOResponseList] = in.tupleSpaceID.get.`type` match
        case TupleSpaceType.LOGIC => logicHandler.writeAll(in)
        case TupleSpaceType.TEXTUAL => textualHandler.writeAll(in)

    override def readAll(in: ReadOrTakeAllRequest): Future[TuplesList] = in.tupleSpaceID.get.`type` match
        case TupleSpaceType.LOGIC => logicHandler.readAll(in)
        case TupleSpaceType.TEXTUAL => textualHandler.readAll(in)

    override def takeAll(in: ReadOrTakeAllRequest): Future[TuplesList] = in.tupleSpaceID.get.`type` match
        case TupleSpaceType.LOGIC => logicHandler.takeAll(in)
        case TupleSpaceType.TEXTUAL => textualHandler.takeAll(in)

    override def writeAllAsStream(in: WriteAllRequest): Source[IOResponse, NotUsed] = in.tupleSpaceID.get.`type` match
        case TupleSpaceType.LOGIC => logicHandler.writeAllAsStream(in)
        case TupleSpaceType.TEXTUAL => textualHandler.writeAllAsStream(in)

    override def readAllAsStream(in: ReadOrTakeAllRequest): Source[Tuple, NotUsed] = in.tupleSpaceID.get.`type` match
        case TupleSpaceType.LOGIC => logicHandler.readAllAsStream(in)
        case TupleSpaceType.TEXTUAL => textualHandler.readAllAsStream(in)

    override def takeAllAsStream(in: ReadOrTakeAllRequest): Source[Tuple, NotUsed] = in.tupleSpaceID.get.`type` match
        case TupleSpaceType.LOGIC => logicHandler.takeAllAsStream(in)
        case TupleSpaceType.TEXTUAL => textualHandler.takeAllAsStream(in)