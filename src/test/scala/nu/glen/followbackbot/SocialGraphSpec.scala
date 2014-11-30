package nu.glen.followbackbot

import org.mockito.Mockito._
import org.scalatest.FunSpec
import org.scalatest.mock.MockitoSugar
import twitter4j._

import scala.util.{Failure, Success}

class SocialGraphSpec extends FunSpec with MockitoSugar {
  val userId = 100L

  val blacklisted = 10000L

  val twitter = mock[Twitter]
  val socialGraph = new SocialGraph(userId, twitter, Set(blacklisted))

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

  val ids1 = mkIds(Array[Long](1, 2, 3, 4), Some(4))
  val ids2 = mkIds(Array(5, 6, blacklisted), None)

  def mkUser(isProtected: Boolean, isFollowRequestSent: Boolean) = {
    val user = mock[User]
    when(user.isProtected).thenReturn(isProtected)
    when(user.isFollowRequestSent).thenReturn(isFollowRequestSent)
    user
  }

  describe("SocialGraph.reciprocate") {
    it("should follow and unfollow") {
      val followingIds = mkIds(Array[Long](2, 3, 4), None)
      val followersIds = mkIds(Array[Long](3, 4, 6), None)

      when(twitter.getFollowersIDs(CursorSupport.START)).thenReturn(followersIds)
      when(twitter.getFriendsIDs(CursorSupport.START)).thenReturn(followingIds)

      val user = mkUser(false, false)
      when(twitter.showUser(6)).thenReturn(user)

      // TODO: test false for each arg
      socialGraph.reciprocate(true, true)

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
      it(s"should lookup user if isProtected == $isProtected and not follow if protected and follow request sent") {
        val target = NextTarget()
        val user = mkUser(true, true)
        when(twitter.showFriendship(userId, target)).thenReturn(falseFriendship)
        when(twitter.showUser(target)).thenReturn(user)
        socialGraph.follow(target, isProtected, true)
        verify(twitter).showFriendship(userId, target)
        verify(twitter).showUser(target)
        verify(twitter, never).createFriendship(target)
      }

      it(s"should lookup user if isProtected == $isProtected and follow if protected and follow request not sent") {
        val target = NextTarget()
        val user = mkUser(true, false)
        when(twitter.showFriendship(userId, target)).thenReturn(falseFriendship)
        when(twitter.showUser(target)).thenReturn(user)
        socialGraph.follow(target, isProtected, true)
        verify(twitter).showFriendship(userId, target)
        verify(twitter).showUser(target)
        verify(twitter).createFriendship(target)
      }

      it(s"should lookup user if isProtected == $isProtected and follow if not actually protected") {
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

  describe("SocialGraph.checkOrUnfollow") {
    it("should return Return(true) if following") {
      val target = NextTarget()
      when(twitter.showFriendship(target, userId)).thenReturn(trueFriendship)
      val result = socialGraph.checkOrUnfollow(target)
      assert(result == Success(true))
      verify(twitter).showFriendship(target, userId)
    }

    it("should return Return(false) if not following") {
      val target = NextTarget()
      when(twitter.showFriendship(target, userId)).thenReturn(falseFriendship)
      val result = socialGraph.checkOrUnfollow(target)
      assert(result == Success(false))
      verify(twitter).showFriendship(target, userId)
    }

    it("should return Return(false) if blacklisted") {
      val target = blacklisted
      val result = socialGraph.checkOrUnfollow(target)
      assert(result == Success(false))
      verify(twitter, never).showFriendship(blacklisted, userId)
    }

    it("should handle exception") {
      val target = NextTarget()
      val ex = new RuntimeException
      when(twitter.showFriendship(target, userId)).thenThrow(ex)
      val result = socialGraph.checkOrUnfollow(target)
      assert(result == Failure(ex))
      verify(twitter).showFriendship(target, userId)
    }
  }

  // TODO: test API throw
  describe("SocialGraph.followers") {
    it("should return all followers, except blacklist") {
      when(twitter.getFollowersIDs(CursorSupport.START)).thenReturn(ids1)
      when(twitter.getFollowersIDs(4)).thenReturn(ids2)
      assert(socialGraph.followers == Success(Set(1L, 2L, 3L, 4L, 5L, 6L)))
      // twice because of the reciprocate test
      verify(twitter, times(2)).getFollowersIDs(CursorSupport.START)
      verify(twitter).getFollowersIDs(4)
    }
  }

  // TODO: test API throw
  describe("SocialGraph.following") {
    it("should return all following") {
      when(twitter.getFriendsIDs(CursorSupport.START)).thenReturn(ids1)
      when(twitter.getFriendsIDs(4)).thenReturn(ids2)
      assert(socialGraph.following == Success(Set(1L, 2L, 3L, 4L, 5L, 6L, blacklisted)))
      // twice because of the reciprocate test
      verify(twitter, times(2)).getFriendsIDs(CursorSupport.START)
      verify(twitter).getFriendsIDs(4)
    }
  }
}