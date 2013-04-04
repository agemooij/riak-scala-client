package com.scalapenos.riak

trait RiakMapReduceQueryBuilder {
  import spray.json.RootJsonReader
  import RiakMapReduce._
  import scala.concurrent.Future

  def map(phase: QueryPhase): RiakMapReduceQueryBuilder
  def reduce(phase: QueryPhase): RiakMapReduceQueryBuilder
  def result[R: RootJsonReader]: Future[R]
}

trait RiakMapReduce extends RiakMapReduceQueryBuilder {
  import spray.json.RootJsonReader
  import scala.concurrent.Future
  import RiakMapReduce._

  def query[R: RootJsonReader](queryPhases: Seq[(QueryPhase.Value, QueryPhase)]): Future[R]

  case class QueryBuilderImpl(queryPhases: Seq[(QueryPhase.Value, QueryPhase)]) extends RiakMapReduceQueryBuilder {
    def map(phase: QueryPhase) = QueryBuilderImpl(queryPhases :+ (QueryPhase.Map → phase))
    def reduce(phase: QueryPhase) = QueryBuilderImpl(queryPhases :+ (QueryPhase.Reduce → phase))
    def result[R: RootJsonReader] = query(queryPhases)
  }

  def map(phase: QueryPhase) = QueryBuilderImpl(List(QueryPhase.Map → phase))
  def reduce(phase: QueryPhase) = QueryBuilderImpl(List(QueryPhase.Reduce → phase))
  def result[R: RootJsonReader] = query(Nil)
}

object RiakMapReduce {

  sealed trait Input

  case class InputKeys(keys: Seq[(String, String)]) extends Input

  case class InputKeyData(keys: Seq[(String, String, String)]) extends Input

  case class InputBucket(bucket: String, keyFilters: Seq[Seq[String]] = Nil) extends Input

  case class Input2i(bucket: String, index: String, value: Either[String, (Int, Int)]) extends Input {
    value.right.map {
      case (start, end) ⇒ assert(start < end, "start must be < end")
    }
  }

  case class InputSearch(bucket: String, query: String, filter: Option[String] = None) extends Input

  sealed trait QueryPhase {
    val keep: Boolean
    val language: String
    val sourceFields: Seq[(String, String)]
  }

  object QueryPhase extends Enumeration {
    val Map, Reduce = Value
  }

  sealed trait JsPhase extends QueryPhase {
    val language = "javascript"
  }

  case class JsSource(source: String, keep: Boolean = false) extends JsPhase {
    val sourceFields = List("source" → source)
  }

  case class JsBuiltin(name: String, keep: Boolean = false) extends JsPhase {
    val sourceFields = List("name" → name)
  }

  case class JsStored(bucket: String, key: String, keep: Boolean = false) extends JsPhase {
    val sourceFields = List(
      "bucket"  → bucket,
      "key"     → key
    )
  }

  sealed trait ErlangPhase extends QueryPhase {
    val language = "erlang"
  }

  case class ErlangFunction(module: String, function: String, keep: Boolean = false) extends ErlangPhase {
    val sourceFields = List(
      "module"    → module,
      "function"  → function
    )
  }
}

