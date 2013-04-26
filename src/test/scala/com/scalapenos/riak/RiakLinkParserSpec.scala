package com.scalapenos.riak

import org.specs2.mutable.Specification
import scala.util.{Failure, Success}

class RiakLinkParserSpec extends Specification {
  "The link parser" should {
    "successfully parse a single link" in {
      RiakLinkParser("""</buckets/b/keys/k>; riaktag="v"""") must beLike {
        case Success(List(RiakLink("b", Some("k"), Some(("riaktag", "v"))))) ⇒ ok
      }
    }

    "successfully parse a single link with url encoding" in {
      RiakLinkParser("""</buckets/b/keys/%24k>; riaktag="v"""") must beLike {
        case Success(List(RiakLink("b", Some("$k"), Some(("riaktag", "v"))))) ⇒ ok
      }
    }

    "successfully parse a single link with an old-style path" in {
      RiakLinkParser("""</riak/b/k>; riaktag="v"""") must beLike {
        case Success(List(RiakLink("b", Some("k"), Some(("riaktag", "v"))))) ⇒ ok
      }
    }

    "successfully parse a list of links" in {
      RiakLinkParser("""</buckets/b1/keys/k1>; riaktag="v1", </buckets/b2/keys/k2>; riaktag="v2"""") must beLike {
        case Success(List(RiakLink("b1", Some("k1"), Some(("riaktag", "v1"))), RiakLink("b2", Some("k2"), Some(("riaktag", "v2"))))) ⇒ ok
      }
    }

    "successfully parse a single link without a tag" in {
      RiakLinkParser("</buckets/b/keys/k>") must beLike {
        case Success(List(RiakLink("b", Some("k"), None))) ⇒ ok
      }
    }

    "successfully parse a single link without a tag or key" in {
      RiakLinkParser("</buckets/b>") must beLike {
        case Success(List(RiakLink("b", None, None))) ⇒ ok
      }
    }

    "ignore extra whitespace" in {
      RiakLinkParser(""" </buckets/b/keys/k>;    riaktag="v"     """) must beLike {
        case Success(List(RiakLink("b", Some("k"), Some(("riaktag", "v"))))) ⇒ ok
      }
    }

    "ignore missing whitespace" in {
      RiakLinkParser("""</buckets/b1/keys/k1>;riaktag="v1",</buckets/b2/keys/k2>;riaktag="v2"""") must beLike {
        case Success(List(RiakLink("b1", Some("k1"), Some(("riaktag", "v1"))), RiakLink("b2", Some("k2"), Some(("riaktag", "v2"))))) ⇒ ok
      }
    }

    "fail to parse gobbledygook" in {
      RiakLinkParser("gobble gobble") must beLike {
        case Failure(_) ⇒ ok
      }
    }
  }
}
