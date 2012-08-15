package nu.glen.yourmombot

import nu.glen.followbackbot.{FollowBackBot, RateLimiter, Responder}

object YourMomBot
  extends FollowBackBot(
    Responder.rateLimited(
      Responder.merged(
        Responder.logOnly(Responder.simple(new YourMomBeHaveDoResponder)),
        Responder.simple(new YourMomPastTenseResponder),
        Responder.simple(new YourMomGerrundResponder)
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