package nu.glen.yourmombot

import nu.glen.followbackbot.KeywordPrefixResponder

object YourMomGerrundResponder
  extends KeywordPrefixResponder(
    Set(
      "anything",
      "beijing",
      "during",
      "effing",
      "evening",
      "everything",
      "interesting",
      "king",
      "morning",
      "nothing",
      "something",
      "spring",
      "thing"
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

object YourMomPastTenseResponder
  extends KeywordPrefixResponder(
    Set(
      "peed",
      "need"
    )
  )
{
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