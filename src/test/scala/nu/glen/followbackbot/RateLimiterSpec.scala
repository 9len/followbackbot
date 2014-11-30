package nu.glen.followbackbot

import org.mockito.Mockito._
import org.scalatest.FunSpec
import org.scalatest.mock.MockitoSugar
import twitter4j.User
import scala.concurrent.duration._

class RateLimiterSpec extends FunSpec with MockitoSugar {
  val user = mock[User]
  when(user.getId).thenReturn(100)
  when(user.getScreenName).thenReturn("foo")

  val size = 10

  describe("ExpiringCountersRateLimiter") {
    // TODO: explicit tests for ExpiringCounters, use mocks for ExpiringCountersRateLimiter
    it("returns false if not over limit") {
      val limiter = new ExpiringCountersRateLimiter(ExpiringCounters(100.millis, size), 5)
      assert(!limiter(user))
    }

    it("returns true if over limit") {
      val limiter = new ExpiringCountersRateLimiter(ExpiringCounters(100.millis, size), 5)
      assert(!limiter(user))
      assert(!limiter(user))
      assert(!limiter(user))
      assert(!limiter(user))
      assert(!limiter(user))
      assert(limiter(user))
    }

    it("resets after time limit") {
      val limiter = new ExpiringCountersRateLimiter(ExpiringCounters(100.millis, size), 1)
      assert(!limiter(user))
      assert(limiter(user))
      Thread.sleep(100)
      assert(!limiter(user))
    }
  }
}