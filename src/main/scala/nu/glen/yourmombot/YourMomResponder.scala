package nu.glen.yourmombot

import nu.glen.followbackbot.{GerundKeywordPrefixResponder, PastTenseKeywordPrefixResponder}

object YourMomGerundResponder extends GerundKeywordPrefixResponder {
  override def combine(gerund: String, rest: String) = "Your mom's %s%s".format(gerund, firstSentence(rest))
}

object YourMomPastTenseResponder extends PastTenseKeywordPrefixResponder {
  override def combine(verb: String, rest: String) = "Your mom %s%s".format(verb, firstSentence(rest))
}
