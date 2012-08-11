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

  describe("SocialGraph.reciprocate") {

  }

  describe("SocialGraph.isFollowing") {
    it("should return true if following and false if not") {
      when(twitter.showFriendship(100, 200)).thenReturn(trueFriendship)
      assert(socialGraph.isFollowing(200))
      verify(twitter).showFriendship(100, 200)
    }

    it("should return false if not following") {
      when(twitter.showFriendship(100, 300)).thenReturn(falseFriendship)
      assert(!socialGraph.isFollowing(300))
      verify(twitter).showFriendship(100, 300)
    }
  }

  describe("SocialGraph.isFollowedBy") {
    it("should return true if followed by") {
      when(twitter.showFriendship(200, 100)).thenReturn(trueFriendship)
      assert(socialGraph.isFollowedBy(200))
      verify(twitter).showFriendship(200, 100)
    }

    it("should return false if not followed by") {
      when(twitter.showFriendship(300, 100)).thenReturn(falseFriendship)
      assert(!socialGraph.isFollowedBy(300))
      verify(twitter).showFriendship(300, 100)
    }
  }

  describe("SocialGraph.follow") {
    for (isProtected <- List(Some(true), Some(false), None)) {
      it("should not follow if already following if isProtected == " + isProtected) {

      }
    }
    for (isProtected <- List(Some(true), None)) {
      it("should lookup user if isProtected == %s and follow if not protected".format(isProtected)) {

      }

      it("should lookup user if isProtected == %s and follow if protected and follow request not sent".format(isProtected)) {

      }

      it("should lookup user if isProtected == %s and not follow if protected and follow request sent".format(isProtected)) {

      }
    }

    it("should not lookup user if isProtected == Some(false) and follow") {

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
      assert(latch == "foo")
      when(twitter.showFriendship(400, 100)).thenReturn(trueFriendship)
      socialGraph.ifFollowedBy(400, action("bar"), "blegga")
      assert(latch == "bar")
      verify(twitter).showFriendship(400, 100)
    }

    it("should not invoke action if following") {
      assert(latch == "bar")
      when(twitter.showFriendship(500, 100)).thenReturn(falseFriendship)
      socialGraph.ifFollowedBy(500, action("baz"), "blegga")
      assert(latch == "bar")
      verify(twitter).showFriendship(500, 100)
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