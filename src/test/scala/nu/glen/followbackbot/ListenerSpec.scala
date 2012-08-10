package nu.glen.followbackbot

import org.mockito.Matchers._
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.FunSpec
import org.scalatest.mock.MockitoSugar
import twitter4j._

class ListenerSpec extends FunSpec with MockitoSugar {
  val socialGraph = mock[SocialGraph]
  val twitter = mock[Twitter]
  val responder = mock[Responder]

  val listener = new Listener(100, "foo", responder, socialGraph, twitter)

  val me = mock[User]
  when(me.getId).thenReturn(100)
  when(me.getScreenName).thenReturn("foo")

  val them = mock[User]
  when(them.getId).thenReturn(200)
  when(them.getScreenName).thenReturn("bar")

  describe("TweetAction") {
    it("should call twitter") {
      val statusUpdate = new StatusUpdate("foobar")
      TweetAction(twitter, statusUpdate)()
      verify(twitter).updateStatus(statusUpdate)
    }
  }

  describe("Listener.onStatus") {
    it("should ignore my status") {
      val status = mock[Status]
      when(status.getUser).thenReturn(me)
      when(status.getText).thenReturn("foobar")
      listener.onStatus(status)
      verify(socialGraph, never).ifFollowing(anyLong, any[Action], anyString, any[Seq[Any]]: _*)
    }

    it("should ignore retweet of my status") {
      val status = mock[Status]
      when(status.getUser).thenReturn(them)
      when(status.getText).thenReturn("RT @foo: foobarbaz")
      listener.onStatus(status)
      verify(socialGraph, never).ifFollowing(anyLong, any[Action], anyString, any[Seq[Any]]: _*)
    }

    it("should ignore status with no response") {
      val status = mock[Status]
      when(status.getUser).thenReturn(them)
      when(status.getText).thenReturn("foobarbaz")
      when(responder(status)).thenReturn(None)
      listener.onStatus(status)
      verify(responder)(status)
      verify(socialGraph, never).ifFollowing(anyLong, any[Action], anyString, any[Seq[Any]]: _*)
    }

    it("call ifFollowing for good status") {
      val status = mock[Status]
      when(status.getUser).thenReturn(them)
      when(status.getText).thenReturn("foobarbaz")
      val statusUpdate = new StatusUpdate("meh")
      val tweetAction = TweetAction(twitter, statusUpdate)
      when(responder(status)).thenReturn(Some(statusUpdate))
      listener.onStatus(status)
      verify(responder)(status)
      verify(socialGraph).ifFollowing(them.getId, tweetAction, " Replying with %s", "meh")
    }
  }

  describe("Listener.onFollow") {
    it("should ignore my follows") {
      listener.onFollow(me, them)
      verify(socialGraph, never).follow(anyLong, any[Option[Boolean]])
    }

    it("should follow back, pass protected = false") {
      when(them.isProtected).thenReturn(false)
      listener.onFollow(them, me)
      verify(socialGraph).follow(200, Some(false))
    }

    it("should follow back, pass protected = true") {
      when(them.isProtected).thenReturn(true)
      listener.onFollow(them, me)
      verify(socialGraph).follow(200, Some(true))
    }
  }
}