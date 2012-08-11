package nu.glen.yourmombot

import nu.glen.followbackbot.{FollowBackBot, Responder}

object YourMomBot extends FollowBackBot(Responder.simple(YourMomResponder)) with App