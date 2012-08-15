package nu.glen.yourmombot

import org.scalatest.FunSpec

class YourMomResponderSpec extends FunSpec {
  // most of the unit tests for this are in ResponderSpec, since the only
  // thing this class does is prefix "Your Mom's"
  describe("YourMomGerrundResponder") {
    val responder = new YourMomGerrundResponder

    it("should extract a simple sentance") {
      val result = responder("I'm hoping this works")
      assert(result == Some("Your mom's hoping this works"))
    }

    it("should return None for no ing") {
      val result = responder("foobar")
      assert(result == None)
    }
  }

  // most of the unit tests for this are in ResponderSpec, since the only
  // thing this class does is prefix "Your Mom's"
  describe("YourMomPastTenseResponder") {
    val responder = new YourMomPastTenseResponder

    it("should extract a simple sentance") {
      val result = responder("I hoped this worked")
      assert(result == Some("Your mom hoped this worked"))
    }

    it("should return None for no ed") {
      val result = responder("foobar")
      assert(result == None)
    }
  }

  describe("YourMomBeHaveResponder") {
    val responder = new YourMomBeHaveDoResponder

    it("should extract") {
      val result = responder("I didn't much care for that")
      println(result)
      assert(result == Some("Your mom didn't much care for that"))
    }

    it("should return None") {
      val result = responder("foobar")
      assert(result == None)
    }
  }
}