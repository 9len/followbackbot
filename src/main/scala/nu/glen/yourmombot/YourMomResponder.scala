package nu.glen.yourmombot

import nu.glen.followbackbot.{BeHaveDoKeywordPrefixResponder, GerrundKeywordPrefixResponder, PastTenseKeywordPrefixResponder}

class YourMomGerrundResponder extends GerrundKeywordPrefixResponder {
  override def combine(gerrund: String, rest: String) = s"Your mom's $gerrund$rest"
}

class YourMomPastTenseResponder extends PastTenseKeywordPrefixResponder {
  override def combine(verb: String, rest: String) = s"Your mom $verb$rest"
}

class YourMomBeHaveDoResponder extends BeHaveDoKeywordPrefixResponder {
  override def combine(verb: String, rest: String) = s"Your mom $verb$rest"
}