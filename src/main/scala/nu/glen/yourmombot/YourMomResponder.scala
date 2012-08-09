package nu.glen.yourmombot

import nu.glen.followbackbot.SimpleResponder

object YourMomResponder extends SimpleResponder {
  private[this] val Regex = ".*?\\b([a-zA-Z]+[iI][nN][gG])\\b(.*)".r

  // extracts gerand and sentence from tweet
  def apply(status: String): Option[String] = status match {
    case Regex(gerrand, rest) => Some("Your mom's %s%s".format(gerrand.toLowerCase, rest))
    case _ => None
  }
}