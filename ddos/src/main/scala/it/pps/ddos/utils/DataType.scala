package it.pps.ddos.utils

import java.util.Random
import scala.collection.mutable
import com.github.nscala_time.time.Imports.*

/**
 * The typeclass: a T-generic trait defining operations over T
 *
 * @tparam T is a generic sensor data type
 */
trait DataType[T]:
  /**
   * Type-level method for having a single instance of a value for all instances of a particular type T
   *
   * @return
   */
  def defaultValue: T

/**
 * The module with algorithms using DataType type-class
 */
object DataType:
  def defaultValue[T](using data: DataType[T]): T = data.defaultValue

/**
 * Implementations of DataType "context"
 */
object GivenDataType:
  given IntDataType: DataType[Int] with
    override def defaultValue: Int = 0

  given DoubleDataType: DataType[Double] with
    override def defaultValue: Double = 0.0

  given BooleanDataType: DataType[Boolean] with
    override def defaultValue: Boolean = false

  given CharDataType: DataType[Char] with
    override def defaultValue: Char = ' '

  given StringDataType: DataType[String] with
    override def defaultValue: String = ""

  given AnyDataType: DataType[Any] with
    override def defaultValue: Any = None
    
  given HashMapDataType: DataType[mutable.HashMap[Any,Any]] with
    override def defaultValue: mutable.HashMap[Any,Any] = mutable.HashMap.empty

  given LongDataType: DataType[Long] with
    override def defaultValue: Long = 0L

  given Tuple2DataType[T: DataType]: DataType[(T, T)] with
    override def defaultValue: (T, T) = (summon[DataType[T]].defaultValue, summon[DataType[T]].defaultValue)