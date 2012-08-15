package nu.glen.yourmombot

import com.twitter.conversions.time._
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
          RateLimiter.allow(1).per(5.minutes),
          RateLimiter.allow(2).per(1.hour),
          RateLimiter.allow(8).per(24.hours)
        )
      )
    )
  )
  with App
{
  override def name = "YourMomBot"
}