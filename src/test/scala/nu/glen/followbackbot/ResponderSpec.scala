package nu.glen.followbackbot

import org.mockito.Mockito.when
import org.scalatest.FunSpec
import org.scalatest.mock.MockitoSugar
import twitter4j._

class ResponderSpec extends FunSpec with MockitoSugar {
  val someResponder = Responder.simple(Some(_))

  val status = mock[Status]
  val user = mock[User]

  when(status.getId).thenReturn(100)
  when(status.getUser).thenReturn(user)
  when(user.getScreenName).thenReturn("bar")

  describe("Responder.simple") {
    it("should do nothing for None") {
      val responder = Responder.simple(_ => None)
      assert(responder(status) == None)
    }

    it("should respond for simple status") {
      val expected = Some(new StatusUpdate("@bar foo").inReplyToStatusId(100))
      when(status.getText).thenReturn("foo")
      when(status.isRetweet).thenReturn(false)
      assert(someResponder(status) == expected)
    }

    it("should truncate long status") {
      val longStatus = "a" * 150
      val truncated = "@bar " + "a" * 134 + "…"
      val expected = Some(new StatusUpdate(truncated).inReplyToStatusId(100))
      when(status.getText).thenReturn(longStatus)
      when(status.isRetweet).thenReturn(false)
      assert(someResponder(status) == expected)
    }

    it("should prefer retweeted text") {
      val expected = Some(new StatusUpdate("@bar baz").inReplyToStatusId(100))
      val rtStatus = mock[Status]
      when(rtStatus.getText).thenReturn("baz")
      when(status.getText).thenReturn("foo")
      when(status.isRetweet).thenReturn(true)
      when(status.getRetweetedStatus).thenReturn(rtStatus)
      assert(someResponder(status) == expected)
    }
  }

  describe("Responder.logOnly") {
    it("should not return when underlying responder returns") {
      val responder = Responder.logOnly(someResponder)
      assert(responder(status) == None)
    }
  }

  describe("Responder.compose") {
    val responder = Responder.compose(
      Responder.simple {
        case "foo" => Some("foo")
        case _ => None
      },
      Responder.simple {
        case "bar" => Some("bar")
        case _ => None
      }
    )

    it("should match first") {
      val expected = Some(new StatusUpdate("@bar foo").inReplyToStatusId(100))
      when(status.getText).thenReturn("foo")
      when(status.isRetweet).thenReturn(false)
      assert(responder(status) == expected)
    }

    it("should match second") {
      val expected = Some(new StatusUpdate("@bar bar").inReplyToStatusId(100))
      when(status.getText).thenReturn("bar")
      when(status.isRetweet).thenReturn(false)
      assert(responder(status) == expected)
    }

    it("should fall through") {
      when(status.getText).thenReturn("baz")
      when(status.isRetweet).thenReturn(false)
      assert(responder(status) == None)
    }
  }

  describe("GerrundKeywordPrefixResponder") {
    val responder = new GerrundKeywordPrefixResponder {
      override def combine(keyword: String, rest: String) = "(%s,%s)".format(keyword, rest)
    }

    val good = Some("(hoping, this works)")

    it("should extract a simple sentance") {
      val result = responder("I'm hoping this works")
      assert(result == good)
    }

    it("should handle mixed case") {
      val result = responder("I'm HOPING this works")
      assert(result == good)
    }

    it("should handle sentence beginning with a gerrand") {
      val result = responder("hoping this works")
      assert(result == good)
    }

    it("should remove weirdly-placed quotes") {
      val result = responder("she said \"I'm \"hoping\" this works\"")
      assert(result == good)
    }

    it("should return None for no ing") {
      val result = responder("foobar")
      assert(result == None)
    }

    it("should handle hyphens") {
      val result = responder("@foobar It's baby-triggered laziness related. It predates leave by about 2 weeks, but the beard-growing really progressed afterwards.")
      assert(result == Some("(beard-growing, really progressed afterwards.)"))
    }

    it("should handle stopwords") {
      val result = responder("do not capture me during this sentence.")
      assert(result == None)
    }
  }

  describe("PastTenseKeywordPrefixResponder") {
    val responder = new PastTenseKeywordPrefixResponder {
      override def combine(keyword: String, rest: String) = "(%s,%s)".format(keyword, rest)
    }

    val good = Some("(hoped, this worked)")

    it("should extract a simple sentance") {
      val result = responder("I hoped this worked")
      assert(result == good)
    }

    it("should handle mixed case") {
      val result = responder("I HoPeD this worked")
      assert(result == good)
    }

    it("should handle sentence beginning with a gerrand") {
      val result = responder("hoped this worked")
      assert(result == good)
    }

    it("should remove weirdly-placed quotes") {
      val result = responder("she said \"I \"hoped\" this worked\"")
      assert(result == good)
    }

    it("should return None for no ed") {
      val result = responder("foobar")
      assert(result == None)
    }

    it("should handle hyphens") {
      val result = responder("@foobar I bitch-slapped you pretty hard")
      assert(result == Some("(bitch-slapped, you pretty hard)"))
    }

    it("should handle stopwords") {
      val result = responder("do not capture me during this sentence.")
      assert(result == None)
    }
  }
}
