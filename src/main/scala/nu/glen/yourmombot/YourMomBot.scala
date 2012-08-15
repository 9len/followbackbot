package nu.glen.yourmombot

import nu.glen.followbackbot.{FollowBackBot, RateLimiter, Responder}

object YourMomBot
  extends FollowBackBot(
    Responder.ignoreProtectedUsers(
      Responder.rateLimited(
        Responder.preferLongestResponse(
          Responder.simple(new YourMomBeHaveDoResponder),
          Responder.simple(new YourMomGerrundResponder),
          Responder.simple(new YourMomPastTenseResponder)
        ),
        RateLimiter.merged(
          RateLimiter.perHour(2),
          RateLimiter.perDay(8)
        )
      )
    )
  )
  with App
{
  override def name = "YourMomBot"
}