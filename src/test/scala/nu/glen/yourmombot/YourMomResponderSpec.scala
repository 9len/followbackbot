package nu.glen.yourmombot

import org.scalatest.FunSpec

class YourMomResponderSpec extends FunSpec {
  val good = Some("Your mom's hoping this works")

  describe("YourMomResponder") {
    it("should extract a simple sentance") {
      val result = YourMomResponder("I'm hoping this works")
      assert(result == good)
    }

    it("should handle mixed case") {
      val result = YourMomResponder("I'm HOPING this works")
      println(result)
      assert(result == good)
    }

    it("should handle sentence beginning with a gerrand") {
      val result = YourMomResponder("hoping this works")
      assert(result == good)
    }

    it("should return None for no ing") {
      val result = YourMomResponder("foobar")
      assert(result == None)
    }

    it("should handle hyphens") {
      val result = YourMomResponder("@foobar It's baby-triggered laziness related. It predates leave by about 2 weeks, but the beard-growing really progressed afterwards.")
      assert(result == Some("Your mom's beard-growing really progressed afterwards."))
    }
  }
}