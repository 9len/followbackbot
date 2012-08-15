package nu.glen.yourmombot

import nu.glen.followbackbot.{FollowBackBot, RateLimiter, Responder}

object YourMomBot
  extends FollowBackBot(
    Responder.rateLimited(
      Responder.merged(
        Responder.simple(YourMomPastTenseResponder),
        Responder.simple(YourMomGerrundResponder)
      ),
      RateLimiter.merged(
        RateLimiter.perHour(2),
        RateLimiter.perDay(10)
      )
    )
  )
  with App
{
  override def name = "YourMomBot"
}