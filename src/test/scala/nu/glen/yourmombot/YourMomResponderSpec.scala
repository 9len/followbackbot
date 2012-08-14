package nu.glen.yourmombot

import org.scalatest.FunSpec

class YourMomResponderSpec extends FunSpec {
  val good = Some("Your mom's hoping this works")

  describe("YourMomGerrundResponder") {
    it("should extract a simple sentance") {
      val result = YourMomGerrundResponder("I'm hoping this works")
      assert(result == good)
    }

    it("should handle mixed case") {
      val result = YourMomGerrundResponder("I'm HOPING this works")
      assert(result == good)
    }

    it("should handle sentence beginning with a gerrand") {
      val result = YourMomGerrundResponder("hoping this works")
      assert(result == good)
    }

    it("should return None for no ing") {
      val result = YourMomGerrundResponder("foobar")
      assert(result == None)
    }

    it("should handle hyphens") {
      val result = YourMomGerrundResponder("@foobar It's baby-triggered laziness related. It predates leave by about 2 weeks, but the beard-growing really progressed afterwards.")
      assert(result == Some("Your mom's beard-growing really progressed afterwards."))
    }

    it("should handle stopwords") {
      val result = YourMomGerrundResponder("do not capture me during this sentence.")
      assert(result == None)
    }
  }
}