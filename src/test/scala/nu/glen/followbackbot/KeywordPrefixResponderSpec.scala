package nu.glen.followbackbot

import org.scalatest.FunSpec

class KeywordPrefixResponderSpec extends FunSpec {
  describe("BeHaveDoKeywordPrefixResponder") {
    val responder = new BeHaveDoKeywordPrefixResponder {
      override def combine(keyword: String, rest: String) = "(%s,%s)".format(keyword, rest)
    }

    it("should extract a sentance") {
      val result = responder("she could eat the poop")
      assert(result == Some("(could, eat the poop)"))
    }

    it("should prefer the longer extraction") {
      val result = responder("she wants to say she could eat the poop")
      assert(result == Some("(wants to, say she could eat the poop)"))
    }

    it("should return None for no match") {
      val result = responder("no way no how no may no mow")
      assert(result == None)
    }
  }

  describe("GerrundKeywordPrefixResponder") {
    val responder = new GerrundKeywordPrefixResponder {
      override def combine(keyword: String, rest: String) = "(%s,%s)".format(keyword, rest)
    }

    val good = Some("(hoping, this works)")

    it("should extract a simple sentance") {
      val result = responder("I'm hoping this works")
      assert(result == good)
    }

    it("should handle mixed case") {
      val result = responder("I'm HOPING this works")
      assert(result == good)
    }

    it("should handle sentence beginning with a gerrand") {
      val result = responder("hoping this works")
      assert(result == good)
    }

    it("should remove weirdly-placed quotes") {
      val result = responder("she said \"I'm \"hoping\" this works\"")
      assert(result == good)
    }

    it("should return None for no ing") {
      val result = responder("foobar")
      assert(result == None)
    }

    it("should handle hyphens") {
      val result = responder("@foobar It's baby-triggered laziness related. It predates leave by about 2 weeks, but the beard-growing really progressed afterwards.")
      assert(result == Some("(beard-growing, really progressed afterwards.)"))
    }

    it("should handle stopwords") {
      val result = responder("do not capture me during this sentence.")
      assert(result == None)
    }
  }

  describe("PastTenseKeywordPrefixResponder") {
    val responder = new PastTenseKeywordPrefixResponder {
      override def combine(keyword: String, rest: String) = "(%s,%s)".format(keyword, rest)
    }

    val good = Some("(hoped, this worked)")

    it("should extract a simple sentance") {
      val result = responder("I hoped this worked")
      assert(result == good)
    }

    it("should handle mixed case") {
      val result = responder("I HoPeD this worked")
      assert(result == good)
    }

    it("should handle sentence beginning with a gerrand") {
      val result = responder("hoped this worked")
      assert(result == good)
    }

    it("should remove weirdly-placed quotes") {
      val result = responder("she said \"I \"hoped\" this worked\"")
      assert(result == good)
    }

    it("should return None for no ed") {
      val result = responder("foobar")
      assert(result == None)
    }

    it("should handle hyphens") {
      val result = responder("@foobar I bitch-slapped you pretty hard")
      assert(result == Some("(bitch-slapped, you pretty hard)"))
    }

    it("should handle stopwords") {
      val result = responder("indeed!")
      assert(result == None)
    }
  }
}
