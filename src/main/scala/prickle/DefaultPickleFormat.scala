package prickle

import scala.util.{Failure, Success, Try}

sealed trait PFormat
case object PNull extends PFormat
case class PBoolean(b: Boolean) extends PFormat
case class PNumber(x: Double) extends PFormat
case class PString(s: String) extends PFormat
case class PArray(elems: Seq[PFormat]) extends PFormat
case class PObject(fields: Map[String, PFormat]) extends PFormat

trait DefaultPickleFormat {

  implicit object DefaultConfig extends CyclicPConfig[PFormat] with HashCharPrefix[PFormat] with NullPermittedConfig[PFormat]
    with SimplePBuilder with SimplePReader
}

trait SimplePBuilder extends PBuilder[PFormat] {
  def makeNull(): PFormat = PNull
  def makeBoolean(b: Boolean): PFormat = PBoolean(b)
  def makeNumber(x: Double): PFormat = PNumber(x)
  def makeString(s: String): PFormat = PString(s)
  def makeArray(elems: PFormat*): PFormat = PArray(elems.toList)
  def makeObject(fields: Seq[(String, PFormat)]): PFormat = PObject(fields.toMap)
}

trait SimplePReader extends PReader[PFormat] {
  def isNull(x: PFormat): Boolean = x == PNull
  def readBoolean(x: PFormat): Try[Boolean] = x match {
    case PBoolean(b) => Success(b)
    case other => Failure(new RuntimeException(s"Expected: PBoolean  Actual: ${context(other)}"))
  }
  def readNumber(x: PFormat): Try[Double] = x match {
    case PNumber(n) => Success(n)
    case other => Failure(new RuntimeException(s"Expected: PNumber  Actual: ${context(other)}"))
  }
  def readString(x: PFormat): Try[String] = x match {
    case PString(s) => Success(s)
    case other => Failure(new RuntimeException(s"Expected: PString  Actual: ${context(other)}"))
  }
  def readArrayLength(x: PFormat): Try[Int] = x match {
    case PArray(elems) => Success(elems.size)
    case other => Failure(new RuntimeException(s"Expected: PArray  Actual: ${context(other)}"))
  }
  def readArrayElem(x: PFormat, index: Int): Try[PFormat] = x match {
    case PArray(elems) if index < elems.size => Success(elems(index))
    case other => Failure(new RuntimeException(s"Expected: PArray with size >= $index  Actual: ${context(other)}"))
  }
  def readObjectField(x: PFormat, field: String): Try[PFormat] = x match {
    case PObject(fields) if fields.contains(field) => Success(fields(field))
    case other => Failure(new RuntimeException(s"Expected: PObject containing '$field'  Actual: ${context(other)}"))
  }

  /** Should provide some diagnostic string about the pickle 'x', eg its content.
    * Used to build error messages. */
  def context(x: PFormat): String = x.toString
}
