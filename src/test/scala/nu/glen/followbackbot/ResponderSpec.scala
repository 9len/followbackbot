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
}
