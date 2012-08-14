package nu.glen.yourmombot

import nu.glen.followbackbot.KeywordPrefixResponder

object YourMomResponder
  extends KeywordPrefixResponder(
    Set(
      "during"
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