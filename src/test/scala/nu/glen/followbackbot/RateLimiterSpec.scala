package nu.glen.followbackbot

import com.twitter.conversions.time._
import org.mockito.Mockito._
import org.scalatest.FunSpec
import org.scalatest.mock.MockitoSugar
import twitter4j.User

class RateLimiterSpec extends FunSpec with MockitoSugar {
  val user = mock[User]
  when(user.getId).thenReturn(100)
  when(user.getScreenName).thenReturn("foo")

  val size = 10

  describe("SimpleRateLimiter") {
    it("returns false if not over limit") {
      val limiter = new SimpleRateLimiter(100.millis, 5, size)
      assert(!limiter(user))
    }

    it("returns true if over limit") {
      val limiter = new SimpleRateLimiter(100.millis, 5, size)
      assert(!limiter(user))
      assert(!limiter(user))
      assert(!limiter(user))
      assert(!limiter(user))
      assert(!limiter(user))
      assert(limiter(user))
    }

    it("resets after time limit") {
      val limiter = new SimpleRateLimiter(100.millis, 1, size)
      assert(!limiter(user))
      assert(limiter(user))
      Thread.sleep(100)
      assert(!limiter(user))
    }
  }
}