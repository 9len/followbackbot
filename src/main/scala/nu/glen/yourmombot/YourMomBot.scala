package nu.glen.yourmombot

import nu.glen.followbackbot.{FollowBackBot, Responder}

object YourMomBot
  extends FollowBackBot(
    Responder.compose(
      Responder.logOnly(Responder.simple(YourMomPastTenseResponder)),
      Responder.simple(YourMomGerrundResponder)
    )
  )
  with App