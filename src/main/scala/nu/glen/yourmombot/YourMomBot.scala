package nu.glen.yourmombot

import nu.glen.followbackbot.{FollowBackBot, RateLimiter, Responder}

object YourMomBot
  extends FollowBackBot(
    Responder.rateLimited(
      Responder.merged(
        // for testing
        Responder.logOnly(
          Responder.preferLongestResponse(
            Responder.simple(new YourMomBeHaveDoResponder),
            Responder.simple(new YourMomGerrundResponder),
            Responder.simple(new YourMomPastTenseResponder)
          )
        ),
        Responder.simple(new YourMomPastTenseResponder),
        Responder.simple(new YourMomGerrundResponder)
      ),
      RateLimiter.merged(
        RateLimiter.perHour(2),
        RateLimiter.perDay(8)
      )
    )
  )
  with App
{
  override def name = "YourMomBot"
}