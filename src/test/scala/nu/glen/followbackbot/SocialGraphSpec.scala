package nu.glen.followbackbot

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FunSpec
import org.scalatest.mock.MockitoSugar
import twitter4j._

class SocialGraphSpec extends FunSpec with MockitoSugar {
  val userId = 100

  val twitter = mock[Twitter]
  val socialGraph = new SocialGraph(userId, twitter)

  val trueFriendship = mock[Relationship]
  when(trueFriendship.isSourceFollowingTarget).thenReturn(true)

  val falseFriendship = mock[Relationship]
  when(falseFriendship.isSourceFollowingTarget).thenReturn(false)

  object NextTarget {
    private[this] var i = 0

    def apply() = {
      i += 1
      userId + i
    }
  }

  def mkIds(userIds: Array[Long], nextCursor: Option[Int]) = {
    val ids = mock[IDs]
    when(ids.getIDs).thenReturn(userIds)

    nextCursor match {
      case Some(cursor) =>
        when(ids.hasNext).thenReturn(true)
        when(ids.getNextCursor).thenReturn(cursor)

      case None =>
        when(ids.hasNext).thenReturn(false)
    }

    ids
  }

  val ids1 = mkIds(Array(1, 2, 3, 4), Some(4))
  val ids2 = mkIds(Array(5, 6), None)

  def mkUser(isProtected: Boolean, isFollowRequestSent: Boolean) = {
    val user = mock[User]
    when(user.isProtected).thenReturn(isProtected)
    when(user.isFollowRequestSent).thenReturn(isFollowRequestSent)
    user
  }

  describe("SocialGraph.reciprocate") {
    it("should follow and unfollow") {
      val followingIds = mkIds(Array(2, 3, 4), None)
      val followersIds = mkIds(Array(3, 4, 6), None)

      when(twitter.getFollowersIDs(CursorSupport.START)).thenReturn(followersIds)
      when(twitter.getFriendsIDs(CursorSupport.START)).thenReturn(followingIds)

      val user = mkUser(false, false)
      when(twitter.showUser(6)).thenReturn(user)

      socialGraph.reciprocate()

      verify(twitter).getFollowersIDs(CursorSupport.START)
      verify(twitter).getFriendsIDs(CursorSupport.START)
      verify(twitter, never).showFriendship(userId, 6)
      verify(twitter).showUser(6)
      verify(twitter).createFriendship(6)
      verify(twitter).destroyFriendship(2)
    }
  }

  describe("SocialGraph.isFollowing") {
    it("should return true if following and false if not") {
      val target = NextTarget()
      when(twitter.showFriendship(userId, target)).thenReturn(trueFriendship)
      assert(socialGraph.isFollowing(target))
      verify(twitter).showFriendship(userId, target)
    }

    it("should return false if not following") {
      val target = NextTarget()
      when(twitter.showFriendship(userId, target)).thenReturn(falseFriendship)
      assert(!socialGraph.isFollowing(target))
      verify(twitter).showFriendship(userId, target)
    }
  }

  describe("SocialGraph.isFollowedBy") {
    it("should return true if followed by") {
      val target = NextTarget()
      when(twitter.showFriendship(target, userId)).thenReturn(trueFriendship)
      assert(socialGraph.isFollowedBy(target))
      verify(twitter).showFriendship(target, userId)
    }

    it("should return false if not followed by") {
      val target = NextTarget()
      when(twitter.showFriendship(target, userId)).thenReturn(falseFriendship)
      assert(!socialGraph.isFollowedBy(target))
      verify(twitter).showFriendship(target, userId)
    }
  }

  describe("SocialGraph.follow") {
    for (isProtected <- List(Some(true), Some(false), None)) {
      it("should not follow if already following if isProtected == " + isProtected) {
        val target = NextTarget()
        when(twitter.showFriendship(userId, target)).thenReturn(trueFriendship)
        socialGraph.follow(target, isProtected, true)
        verify(twitter).showFriendship(userId, target)
        verify(twitter, never).showUser(target)
        verify(twitter, never).createFriendship(target)
      }
    }

    it("should not lookup user if isProtected == Some(false) and skip follow check if checkAlreadyFollowed = false") {
      val target = NextTarget()
      val user = mkUser(false, false)
      when(twitter.showUser(target)).thenReturn(user)
      socialGraph.follow(target, Some(false), false)
      verify(user, never).isProtected
      verify(user, never).isFollowRequestSent
      verify(twitter, never).showFriendship(userId, target)
      verify(twitter, never).showUser(target)
      verify(twitter).createFriendship(target)
    }

    for (isProtected <- List(Some(true), None)) {
      it("should lookup user if isProtected == %s and not follow if protected and follow request sent".format(isProtected)) {
        val target = NextTarget()
        val user = mkUser(true, true)
        when(twitter.showFriendship(userId, target)).thenReturn(falseFriendship)
        when(twitter.showUser(target)).thenReturn(user)
        socialGraph.follow(target, isProtected, true)
        verify(twitter).showFriendship(userId, target)
        verify(twitter).showUser(target)
        verify(twitter, never).createFriendship(target)
      }

      it("should lookup user if isProtected == %s and follow if protected and follow request not sent".format(isProtected)) {
        val target = NextTarget()
        val user = mkUser(true, false)
        when(twitter.showFriendship(userId, target)).thenReturn(falseFriendship)
        when(twitter.showUser(target)).thenReturn(user)
        socialGraph.follow(target, isProtected, true)
        verify(twitter).showFriendship(userId, target)
        verify(twitter).showUser(target)
        verify(twitter).createFriendship(target)
      }

      it("should lookup user if isProtected == %s and follow if not actually protected".format(isProtected)) {
        val target = NextTarget()
        val user = mkUser(false, false)
        when(twitter.showFriendship(userId, target)).thenReturn(falseFriendship)
        when(twitter.showUser(target)).thenReturn(user)
        socialGraph.follow(target, isProtected, true)
        verify(user, never).isFollowRequestSent
        verify(twitter).showFriendship(userId, target)
        verify(twitter).showUser(target)
        verify(twitter).createFriendship(target)
      }
    }

    it("should not lookup user if isProtected == Some(false) and follow") {
      val target = NextTarget()
      val user = mkUser(false, false)
      when(twitter.showFriendship(userId, target)).thenReturn(falseFriendship)
      when(twitter.showUser(target)).thenReturn(user)
      socialGraph.follow(target, Some(false), true)
      verify(user, never).isProtected
      verify(user, never).isFollowRequestSent
      verify(twitter).showFriendship(userId, target)
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
      when(twitter.showFriendship(target, userId)).thenReturn(trueFriendship)
      socialGraph.ifFollowedBy(target, action("bar"), "blegga")
      assert(latch == "bar")
      verify(twitter).showFriendship(target, userId)
    }

    it("should not invoke action if following") {
      val target = NextTarget()
      assert(latch == "bar")
      when(twitter.showFriendship(target, userId)).thenReturn(falseFriendship)
      socialGraph.ifFollowedBy(target, action("baz"), "blegga")
      assert(latch == "bar")
      verify(twitter).showFriendship(target, userId)
    }
  }

  describe("SocialGraph.followers") {
    it("should return all followers") {
      when(twitter.getFollowersIDs(CursorSupport.START)).thenReturn(ids1)
      when(twitter.getFollowersIDs(4)).thenReturn(ids2)
      assert(socialGraph.followers == Set(1L, 2L, 3L, 4L, 5L, 6L))
      // twice because of the reciprocate test
      verify(twitter, times(2)).getFollowersIDs(CursorSupport.START)
      verify(twitter).getFollowersIDs(4)
    }
  }

  describe("SocialGraph.following") {
    it("should return all following") {
      when(twitter.getFriendsIDs(CursorSupport.START)).thenReturn(ids1)
      when(twitter.getFriendsIDs(4)).thenReturn(ids2)
      assert(socialGraph.following == Set(1L, 2L, 3L, 4L, 5L, 6L))
      // twice because of the reciprocate test
      verify(twitter, times(2)).getFriendsIDs(CursorSupport.START)
      verify(twitter).getFriendsIDs(4)
    }
  }
}