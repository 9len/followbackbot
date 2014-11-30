package nu.glen.followbackbot

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import com.google.common.cache.CacheBuilder
import twitter4j.User

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

object RateLimiter {
  val defaultMaxSize = 10000

  def allow(maxValue: Int): Allow = new Allow(maxValue)

  def any(limiters: RateLimiter*): RateLimiter =
    (id) => limiters.foldLeft(false)(_ || _(id))

  def all(limiters: RateLimiter*): RateLimiter =
    (id) => limiters.foldLeft(false)(_ && _(id))

  class Allow(maxValue: Int) {
    def per(resolution: Duration): RateLimiter =
      new ExpiringCountersRateLimiter(ExpiringCounters(resolution, defaultMaxSize), maxValue)
  }
}

/**
 * maintains a map of Ints with a TTL
 *
 * @param ttl the expiration time for a counter
 * @param size the size of the map. if map grows too large, older values are evicted
 */
case class ExpiringCounters(ttl: Duration, size: Int) {
  private[this] val cache =
    CacheBuilder.newBuilder.asInstanceOf[CacheBuilder[Long, AtomicInteger]]
      .initialCapacity(size) // no surprises
      .maximumSize(size)
      .expireAfterWrite(ttl.toMillis, TimeUnit.MILLISECONDS)
      .build[Long, AtomicInteger]()
      .asMap
      .asScala

  protected[this] def get(id: Long): AtomicInteger =
    cache.getOrElseUpdate(id, new AtomicInteger(0))

  def incrementAndGet(id: Long): Int =
    get(id).incrementAndGet()

  def compareAndSet(id: Long, expected: Int, updated: Int): Boolean =
    get(id).compareAndSet(expected, updated)
}

/**
 * A pseudo-leaky bucket; a count is incremented in a cache with a TTL.
 * If the count is incremented above the max value, the user is considered
 * rate limited, until the value expires out of cache.
 *
 * @param counters the Counters class, for book keeping
 * @param maxValue the maximum value for a counter before rate limiting
 */
class ExpiringCountersRateLimiter(counters: ExpiringCounters, maxValue: Int)
  extends RateLimiter
  with SimpleLogger {

  override lazy val name = s"RateLimiter($maxValue/${counters.ttl})".replaceAll("\\.", "_")

  override def apply(user: User): Boolean = {
    val value = counters.incrementAndGet(user.getId)
    val limited = value > maxValue
    if (limited) log.info(s"rate limited ${user.getScreenName}")
    limited
  }
}