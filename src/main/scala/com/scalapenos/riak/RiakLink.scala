package com.scalapenos.riak

import scala.util.parsing.combinator.RegexParsers
import scala.util.Try

case class RiakLink(bucket: String, key: Option[String], attribute: Option[(String, String)]) {
  def this(bucket: String, key: String) = this(bucket, Some(key), None)
  def this(bucket: String, key: String, tag: String) = this(bucket, Some(key), Some("riaktag" → tag))
  def this(bucket: String, key: Option[String], tag: String) = this(bucket, key, Some("riaktag" → tag))
  def this(bucket: String, key: Option[String]) = this(bucket, key, None)

  lazy val tag = attribute match {
    case Some(("riaktag", value)) ⇒ Some(value)
    case _ ⇒ None
  }

  lazy val rel = attribute match {
    case Some(("rel", value)) ⇒ Some(value)
    case _ ⇒ None
  }

  lazy val valueString = {
    val builder = new StringBuilder
    key match {
      case Some(k) ⇒ builder.append(s"</buckets/$bucket/keys/$k>")
      case _ ⇒ builder.append(s"</buckets/$bucket>")
    }
    for (t ← tag) builder.append("; riaktag=\"").append(t).append('"')
    builder.toString
  }
}

trait RiakLinker[T] {
  def links(value: T): Set[RiakLink]
}

object RiakLinker extends LowPriorityDefaultRiakLinkerImplicits

private[riak] trait LowPriorityDefaultRiakLinkerImplicits {
  implicit def emptyLinker[T] = new RiakLinker[T] {
    def links(value: T) = Set.empty
  }
}

object RiakLinkParser extends RegexParsers {
  /*
  Link: </riak/bucket/key>; attr1="value1", </riak/bucket/key>; attr2="value2"
  */
  def word: Parser[String] = """[a-zA-Z0-9%\-\.~_]+""".r ^^ {
    case word ⇒ java.net.URLDecoder.decode(word, "UTF-8")
  }

  def qWord: Parser[String] = "\"" ~> word <~ "\""

  def attr: Parser[(String, String)] = (word ~ ("=" ~> qWord)) ^^ {
    case name ~ value ⇒ (name, value)
  }

  def riakPath: Parser[List[String]] = ("</" ~> repsep(word, "/") <~ ">")

  def riakLink: Parser[RiakLink] = riakPath ~ opt(";" ~> attr) ^^ {
    case path ~ attr ⇒ path match {
      case List("riak", bucket, key) ⇒ RiakLink(bucket, Some(key), attr)
      case List("buckets", bucket, "keys", key) ⇒ RiakLink(bucket, Some(key), attr)
      case List("buckets", bucket) ⇒ RiakLink(bucket, None, attr)
    }
  }

  def expr: Parser[List[RiakLink]] = repsep(riakLink, ",")

  def apply(input: String): Try[List[RiakLink]] = {
    parseAll(expr, input) match {
      case Success(result, _) ⇒ util.Success(result)
      case failure: NoSuccess ⇒ util.Failure(new RuntimeException(failure.msg))
    }
  }
}