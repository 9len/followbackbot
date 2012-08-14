package nu.glen.followbackbot

import com.twitter.util.{Return, Throw}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FunSpec
import org.scalatest.mock.MockitoSugar
import twitter4j._

class ListenerSpec extends FunSpec with MockitoSugar {
  val socialGraph = mock[SocialGraph]
  val twitter = mock[Twitter]
  val responder = mock[Responder]

  val listener = new Listener(100, "foo", responder, socialGraph, twitter)

  object NextTarget {
    private[this] var i = 0

    def apply(screenName: String = "bar") = {
      i += 1
      val user = mock[User]
      when(user.getId).thenReturn(i)
      when(user.getScreenName).thenReturn(screenName)
      user
    }
  }

  val me = mock[User]
  when(me.getId).thenReturn(100)
  when(me.getScreenName).thenReturn("foo")

  describe("Listener.onStatus") {
    it("should ignore my status") {
      val status = mock[Status]
      when(status.getUser).thenReturn(me)
      when(status.getText).thenReturn("foobar")
      listener.onStatus(status)
      verify(socialGraph, never).checkOrUnfollow(anyLong)
    }

    it("should ignore retweet of my status") {
      val status = mock[Status]
      val target = NextTarget()
      when(status.getUser).thenReturn(target)
      when(status.getText).thenReturn("RT @foo: foobarbaz")
      listener.onStatus(status)
      verify(socialGraph, never).checkOrUnfollow(anyLong)
    }

    it("should ignore status with no response") {
      val status = mock[Status]
      val target = NextTarget()
      when(status.getUser).thenReturn(target)
      when(status.getText).thenReturn("foobarbaz")
      when(responder(status)).thenReturn(None)
      listener.onStatus(status)
      verify(responder)(status)
      verify(socialGraph, never).checkOrUnfollow(anyLong)
    }

    it("don't tweet if not still following") {
      val status = mock[Status]
      val target = NextTarget()
      when(status.getUser).thenReturn(target)
      when(status.getText).thenReturn("foobarbaz")
      val statusUpdate = new StatusUpdate("meh")
      when(responder(status)).thenReturn(Some(statusUpdate))
      when(socialGraph.checkOrUnfollow(target.getId)).thenReturn(Return(false))
      listener.onStatus(status)
      verify(responder)(status)
      verify(socialGraph).checkOrUnfollow(target.getId)
      verify(twitter, never).updateStatus(any[StatusUpdate])
    }

    it("don't tweet if checkOnFollow throws") {
      val status = mock[Status]
      val target = NextTarget()
      when(status.getUser).thenReturn(target)
      when(status.getText).thenReturn("foobarbaz")
      val statusUpdate = new StatusUpdate("meh")
      when(responder(status)).thenReturn(Some(statusUpdate))
      when(socialGraph.checkOrUnfollow(target.getId)).thenReturn(Throw(new RuntimeException))
      listener.onStatus(status)
      verify(responder)(status)
      verify(socialGraph).checkOrUnfollow(target.getId)
      verify(twitter, never).updateStatus(any[StatusUpdate])
    }

    it("tweet for good status") {
      val status = mock[Status]
      val target = NextTarget()
      when(status.getUser).thenReturn(target)
      when(status.getText).thenReturn("foobarbaz")
      val statusUpdate = new StatusUpdate("meh")
      when(responder(status)).thenReturn(Some(statusUpdate))
      when(socialGraph.checkOrUnfollow(target.getId)).thenReturn(Return(true))
      listener.onStatus(status)
      verify(responder)(status)
      verify(socialGraph).checkOrUnfollow(target.getId)
      verify(twitter).updateStatus(statusUpdate)
    }
  }

  describe("Listener.onFollow") {
    it("should ignore my follows") {
      listener.onFollow(me, NextTarget())
      verify(socialGraph, never).follow(anyLong, any[Option[Boolean]], any[Boolean])
    }

    it("should follow back, pass protected = false") {
      val target = NextTarget()
      when(target.isProtected).thenReturn(false)
      listener.onFollow(target, me)
      verify(socialGraph).follow(target.getId, Some(false), true)
    }

    it("should follow back, pass protected = true") {
      val target = NextTarget()
      when(target.isProtected).thenReturn(true)
      listener.onFollow(target, me)
      verify(socialGraph).follow(target.getId, Some(true), true)
    }
  }
}