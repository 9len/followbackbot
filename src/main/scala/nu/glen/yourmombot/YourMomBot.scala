package nu.glen.yourmombot

import nu.glen.followbackbot.{FollowBackBot, Responder}

class YourMomBot extends FollowBackBot(Responder.simple(YourMomResponder))

object YourMomBotApplication extends App {
  val yourMomBot = new YourMomBot
}