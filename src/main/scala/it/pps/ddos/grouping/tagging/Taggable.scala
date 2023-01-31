package it.pps.ddos.grouping.tagging

import scala.annotation.{tailrec, targetName}
import scala.collection.immutable.List
import scala.util.Try

/**
 * Trait that allows an object to be marked to become the source of input in a Group instance generated via Tag factory method.
 */
trait Taggable:
  private var tags: List[Tag[_,_]] = List.empty

  private def checkCircularNesting(t: Tag[_,_]): Unit =
    t.getTags().contains(this) match
      case false => for innerTag <- t.getTags() yield checkCircularNesting(innerTag)
      case true => throw IllegalArgumentException("circular tag nesting detected")

  private def addTag(t: Tag[_,_]): Unit =
    try
      checkCircularNesting(t)
      tags = tags ++ List(t)
    catch case e: IllegalArgumentException => throw e

  /**
   * Add input tags to this object, making it a future source for the Group instances that they will generate.
   * @param newTags are the tags to which mark this object.
   * @return Success or Failure for debug pourposes.
   */
  @targetName("markWithTags")
  def ##(newTags: Tag[_,_]*): Try[Unit] = Try { for (t <- newTags) yield addTag(t) }

  /**
   * Get this object current tags.
   * @return the list of tags that has marked this object.
   */
  def getTags(): List[Tag[_,_]] = tags
