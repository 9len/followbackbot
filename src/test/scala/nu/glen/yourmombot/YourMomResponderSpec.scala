package nu.glen.yourmombot

import org.scalatest.FunSpec

class YourMomResponderSpec extends FunSpec {
  // most of the unit tests for this are in ResponderSpec, since the only
  // thing this class does is prefix "Your Mom's"
  describe("YourMomGerundResponder") {
    it("should extract a simple sentance") {
      val result = YourMomGerundResponder("I'm hoping this works")
      assert(result == Some("Your mom's hoping this works"))
    }

    it("should return None for no ing") {
      val result = YourMomGerundResponder("foobar")
      assert(result == None)
    }

    it("should only take the first sentence") {
      val result = YourMomGerundResponder("I'm hoping this works! I've tried hard!")
      assert(result == Some("Your mom's hoping this works"))
    }
  }

  // most of the unit tests for this are in ResponderSpec, since the only
  // thing this class does is prefix "Your Mom's"
  describe("YourMomPastTenseResponder") {
    it("should extract a simple sentance") {
      val result = YourMomPastTenseResponder("I hoped this worked")
      assert(result == Some("Your mom hoped this worked"))
    }

    it("should return None for no ing") {
      val result = YourMomPastTenseResponder("foobar")
      assert(result == None)
    }
  }
}
