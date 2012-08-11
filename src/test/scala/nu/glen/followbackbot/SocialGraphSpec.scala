package nu.glen.followbackbot

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FunSpec
import org.scalatest.mock.MockitoSugar
import twitter4j._

class SocialGraphSpec extends FunSpec with MockitoSugar {
  val twitter = mock[Twitter]
  val socialGraph = new SocialGraph(100, twitter)

  val trueFriendship = mock[Relationship]
  when(trueFriendship.isSourceFollowingTarget).thenReturn(true)

  val falseFriendship = mock[Relationship]
  when(falseFriendship.isSourceFollowingTarget).thenReturn(false)

  val ids1 = mock[IDs]
  when(ids1.hasNext).thenReturn(true)
  when(ids1.getNextCursor).thenReturn(4)
  when(ids1.getIDs).thenReturn(Array(1L, 2L, 3L, 4L))

  val ids2 = mock[IDs]
  when(ids2.hasNext).thenReturn(false)
  when(ids2.getIDs).thenReturn(Array(5L, 6L))

  object NextTarget {
    private[this] var i = 0

    def apply() = {
      i += 1
      100 + i
    }
  }

  describe("SocialGraph.reciprocate") {

  }

  describe("SocialGraph.isFollowing") {
    it("should return true if following and false if not") {
      val target = NextTarget()
      when(twitter.showFriendship(100, target)).thenReturn(trueFriendship)
      assert(socialGraph.isFollowing(target))
      verify(twitter).showFriendship(100, target)
    }

    it("should return false if not following") {
      val target = NextTarget()
      when(twitter.showFriendship(100, target)).thenReturn(falseFriendship)
      assert(!socialGraph.isFollowing(target))
      verify(twitter).showFriendship(100, target)
    }
  }

  describe("SocialGraph.isFollowedBy") {
    it("should return true if followed by") {
      val target = NextTarget()
      when(twitter.showFriendship(target, 100)).thenReturn(trueFriendship)
      assert(socialGraph.isFollowedBy(target))
      verify(twitter).showFriendship(target, 100)
    }

    it("should return false if not followed by") {
      val target = NextTarget()
      when(twitter.showFriendship(target, 100)).thenReturn(falseFriendship)
      assert(!socialGraph.isFollowedBy(target))
      verify(twitter).showFriendship(target, 100)
    }
  }

  describe("SocialGraph.follow") {
    def mkUser(isProtected: Boolean, isFollowRequestSent: Boolean) = {
      val user = mock[User]
      when(user.isProtected).thenReturn(isProtected)
      when(user.isFollowRequestSent).thenReturn(isFollowRequestSent)
      user
    }

    for (isProtected <- List(Some(true), Some(false), None)) {
      it("should not follow if already following if isProtected == " + isProtected) {
        val target = NextTarget()
        when(twitter.showFriendship(100, target)).thenReturn(trueFriendship)
        socialGraph.follow(target, isProtected)
        verify(twitter).showFriendship(100, target)
        verify(twitter, never).showUser(target)
        verify(twitter, never).createFriendship(target)
      }
    }

    for (isProtected <- List(Some(true), None)) {
      it("should lookup user if isProtected == %s and not follow if protected and follow request sent".format(isProtected)) {
        val target = NextTarget()
        val user = mkUser(true, true)
        when(twitter.showFriendship(100, target)).thenReturn(falseFriendship)
        when(twitter.showUser(target)).thenReturn(user)
        socialGraph.follow(target, isProtected)
        verify(twitter).showFriendship(100, target)
        verify(twitter).showUser(target)
        verify(twitter, never).createFriendship(target)
      }

      it("should lookup user if isProtected == %s and follow if protected and follow request not sent".format(isProtected)) {
        val target = NextTarget()
        val user = mkUser(true, false)
        when(twitter.showFriendship(100, target)).thenReturn(falseFriendship)
        when(twitter.showUser(target)).thenReturn(user)
        socialGraph.follow(target, isProtected)
        verify(twitter).showFriendship(100, target)
        verify(twitter).showUser(target)
        verify(twitter).createFriendship(target)
      }

      it("should lookup user if isProtected == %s and follow if not actually protected".format(isProtected)) {
        val target = NextTarget()
        val user = mkUser(false, false)
        when(twitter.showFriendship(100, target)).thenReturn(falseFriendship)
        when(twitter.showUser(target)).thenReturn(user)
        socialGraph.follow(target, isProtected)
        verify(user, never).isFollowRequestSent
        verify(twitter).showFriendship(100, target)
        verify(twitter).showUser(target)
        verify(twitter).createFriendship(target)
      }
    }

    it("should not lookup user if isProtected == Some(false) and follow") {
      val target = NextTarget()
      val user = mkUser(false, false)
      when(twitter.showFriendship(100, target)).thenReturn(falseFriendship)
      when(twitter.showUser(target)).thenReturn(user)
      socialGraph.follow(target, Some(false))
      verify(user, never).isProtected
      verify(user, never).isFollowRequestSent
      verify(twitter).showFriendship(100, target)
      verify(twitter, never).showUser(target)
      verify(twitter).createFriendship(target)
    }
  }

  describe("SocialGraph.unfollow") {
    it("should call destroyFriendship") {
      socialGraph.unfollow(200)
      verify(twitter).destroyFriendship(200)
    }
  }

  describe("SocialGraph.ifFollowedBy") {
    var latch = "foo"
    def action(str: String) = new Action {
      override def apply() {
        latch = str
      }
    }

    it("should invoke action if following") {
      val target = NextTarget()
      assert(latch == "foo")
      when(twitter.showFriendship(target, 100)).thenReturn(trueFriendship)
      socialGraph.ifFollowedBy(target, action("bar"), "blegga")
      assert(latch == "bar")
      verify(twitter).showFriendship(target, 100)
    }

    it("should not invoke action if following") {
      val target = NextTarget()
      assert(latch == "bar")
      when(twitter.showFriendship(target, 100)).thenReturn(falseFriendship)
      socialGraph.ifFollowedBy(target, action("baz"), "blegga")
      assert(latch == "bar")
      verify(twitter).showFriendship(target, 100)
    }
  }

  describe("SocialGraph.followers") {
    it("should return all followers") {
      when(twitter.getFollowersIDs(CursorSupport.START)).thenReturn(ids1)
      when(twitter.getFollowersIDs(4)).thenReturn(ids2)
      assert(socialGraph.followers == Set(1L, 2L, 3L, 4L, 5L, 6L))
      verify(twitter).getFollowersIDs(CursorSupport.START)
      verify(twitter).getFollowersIDs(4)
    }
  }

  describe("SocialGraph.following") {
    it("should return all following") {
      when(twitter.getFriendsIDs(CursorSupport.START)).thenReturn(ids1)
      when(twitter.getFriendsIDs(4)).thenReturn(ids2)
      assert(socialGraph.following == Set(1L, 2L, 3L, 4L, 5L, 6L))
      verify(twitter).getFriendsIDs(CursorSupport.START)
      verify(twitter).getFriendsIDs(4)
    }
  }
}