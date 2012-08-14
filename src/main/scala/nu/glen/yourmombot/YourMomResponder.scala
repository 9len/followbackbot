package nu.glen.yourmombot

import nu.glen.followbackbot.KeywordPrefixResponder

object YourMomGerrundResponder
  extends KeywordPrefixResponder(
    Set(
      "during",
      "evening",
      "thing",
      "effing",
      "everything",
      "something",
      "spring",
      "nothing",
      "morning",
      "anything",
      "interesting",
      "king"
    )
  )
{
  private[this] val Regex = ".*?\\b([a-z\\-A-Z]+[iI][nN][gG])\\\"?\\b(.*)\\\"?".r

  /**
   * extremely naive for now. just extracts tweets with words ending in "ing"
   */
  override def extract(status: String): Option[(String, String)] = status match {
     case Regex(gerrund, rest) => Some((gerrund.toLowerCase, rest))
     case _ => None
  }

  override def process(gerrund: String) = "Your mom's " + gerrund
}

object YourMomPastTenseResponder extends KeywordPrefixResponder(Set.empty) {
  private[this] val Regex = ".*?\\b([a-z\\-A-Z]+[eE][dD])\\\"?\\b(.*)\\\"?".r

  /**
   * extremely naive for now. just extracts tweets with words ending in "ing"
   */
  override def extract(status: String): Option[(String, String)] = status match {
     case Regex(verb, rest) => Some((verb.toLowerCase, rest))
     case _ => None
  }

  override def process(verb: String) = "Your mom " + verb
}