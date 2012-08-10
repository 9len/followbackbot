package nu.glen.followbackbot

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FunSpec
import org.scalatest.mock.MockitoSugar
import twitter4j._

class SocialGraphSpec extends FunSpec with MockitoSugar {
  val twitter = mock[Twitter]
  val socialGraph = new SocialGraph(100, twitter)

  describe("SocialGraph.reciprocate") {

  }

  describe("SocialGraph.isFollowing") {

  }

  describe("SocialGraph.isFollowedBy") {

  }

  describe("SocialGraph.follow") {

  }

  describe("SocialGraph.unfollow") {

  }

  describe("SocialGraph.ifFollowing") {

  }

  describe("SocialGraph.followers") {

  }

  describe("SocialGraph.following") {

  }
}