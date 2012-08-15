package nu.glen.yourmombot

import nu.glen.followbackbot.{
  BeHaveDoKeywordPrefixResponder,
  GerrundKeywordPrefixResponder,
  PastTenseKeywordPrefixResponder
}

class YourMomGerrundResponder extends GerrundKeywordPrefixResponder {
  override def combine(gerrund: String, rest: String) = "Your mom's %s%s".format(gerrund, rest)
}

class YourMomPastTenseResponder extends PastTenseKeywordPrefixResponder {
  override def combine(verb: String, rest: String) = "Your mom %s%s".format(verb, rest)
}

class YourMomBeHaveDoResponder extends BeHaveDoKeywordPrefixResponder {
  override def combine(verb: String, rest: String) = "Your mom %s%s".format(verb, rest)
}