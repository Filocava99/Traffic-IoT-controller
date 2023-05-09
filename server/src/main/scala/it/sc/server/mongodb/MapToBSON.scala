package it.sc.server.mongodb

import org.bson.{BsonReader, BsonType, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}

class MapToBSON extends Codec[Map.Map2[Int, Int]]:
    override def decode(reader: BsonReader, decoderContext: DecoderContext): Map.Map2[Int, Int] =
        var map = Map[Int, Int]()
        reader.readStartDocument()
        while reader.readBsonType() != BsonType.END_OF_DOCUMENT do
            val key = reader.readName().toInt
            val value = reader.readInt32()
            map = map + (key -> value)
        reader.readEndDocument()
        map.asInstanceOf[Map.Map2[Int, Int]]

    override def encode(writer: BsonWriter, value: Map.Map2[Int, Int], encoderContext: EncoderContext): Unit =
        writer.writeStartDocument()
        value.foreach { case (key, value) =>
            writer.writeName(key.toString)
            writer.writeInt32(value)
        }
        writer.writeEndDocument()

    override def getEncoderClass: Class[Map.Map2[Int, Int]] = classOf[Map.Map2[Int, Int]]