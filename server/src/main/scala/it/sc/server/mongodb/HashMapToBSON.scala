package it.sc.server.mongodb
import org.bson.{BsonReader, BsonType, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}

import scala.collection.immutable.HashMap

class HashMapToBSON extends Codec[HashMap[Int, Int]] {

    override def decode(reader: BsonReader, decoderContext: DecoderContext): HashMap[Int, Int] =
        var map = Map[Int, Int]()
        reader.readStartDocument()
        while reader.readBsonType() != BsonType.END_OF_DOCUMENT do
            val key = reader.readName().toInt
            val value = reader.readInt32()
            map = map + (key -> value)
        reader.readEndDocument()
        map.asInstanceOf[HashMap[Int, Int]]

    override def encode(writer: BsonWriter, value: HashMap[Int, Int], encoderContext: EncoderContext): Unit =
        writer.writeStartDocument()
        if(value.isInstanceOf[HashMap[String, Int]]){
            val map = value.asInstanceOf[HashMap[String, Int]]
            map.foreach { case (key, value) =>
                writer.writeName(key)
                writer.writeInt32(value)
            }
        }else{
            value.foreach { case (key, value) =>
                writer.writeName(key.toString)
                writer.writeInt32(value)
            }
        }
        writer.writeEndDocument()

    override def getEncoderClass: Class[HashMap[Int, Int]] = classOf[HashMap[Int, Int]]
}
