package nu.glen.yourmombot

import nu.glen.followbackbot.{GerrundKeywordPrefixResponder, PastTenseKeywordPrefixResponder}

object YourMomGerrundResponder extends GerrundKeywordPrefixResponder {
  override def combine(gerrund: String, rest: String) = "Your mom's %s%s".format(gerrund, rest)
}

object YourMomPastTenseResponder extends PastTenseKeywordPrefixResponder {
  override def combine(verb: String, rest: String) = "Your mom %s%s".format(verb, rest)
}