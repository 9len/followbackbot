package nu.glen.yourmombot

import nu.glen.followbackbot.SimpleResponder

object YourMomResponder extends SimpleResponder {
  private[this] val Regex = ".*?\\b([a-z\\-A-Z]+[iI][nN][gG])\\b(.*)".r

  /**
   * extremely naive for now. just extracts tweets with words ending in "ing"
   */
  def apply(status: String): Option[String] = status match {
    case Regex(gerrand, rest) => Some("Your mom's %s%s".format(gerrand.toLowerCase, rest))
    case _ => None
  }
}