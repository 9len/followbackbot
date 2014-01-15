package nu.glen.followbackbot

import com.twitter.logging.Logger
import com.twitter.conversions.time._
import com.twitter.util.{JavaTimer, Timer}
import twitter4j._
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder

case class Credentials(
    screenName: String,
    token: String,
    secret: String,
    consumerKey: String,
    consumerSecret: String)

/**
 * initializes the Twitter, TwitterStream, Listener and SocialGraph
 *
 * @param responder the Responder for statuses
 * @param blacklist (optional) a Seq of screen names to ignore
 * @param timer (optional) the timer to use for scheduled reciprocation
 */
class FollowBackBot(
    responder: Responder,
    blacklist: Seq[String] = Nil,
    timer: Timer = new JavaTimer)
  extends SimpleLogger
{
  val credentials =
    Credentials(
      System.getenv("TWITTER_SCREEN_NAME"),
      System.getenv("TWITTER_ACCESS_TOKEN"),
      System.getenv("TWITTER_TOKEN_SECRET"),
      System.getenv("TWITTER_CONSUMER_KEY"),
      System.getenv("TWITTER_CONSUMER_SECRET")
    )

  val twitterConfig =
    (new ConfigurationBuilder)
      .setDebugEnabled(true)
      .setOAuthConsumerKey(credentials.consumerKey)
      .setOAuthConsumerSecret(credentials.consumerSecret)
      .setOAuthAccessToken(credentials.token)
      .setOAuthAccessTokenSecret(credentials.secret)
      .setUseSSL(true)
      .build()

  val twitter = new TwitterFactory(twitterConfig).getInstance()

  val userId = twitter.showUser(credentials.screenName).getId

  val socialGraph =
    new SocialGraph(
      userId,
      twitter,
      blacklist map {
        twitter.showUser(_).getId
      } toSet
    )

  val listener = new Listener(userId, credentials.screenName, responder, socialGraph, twitter)

  val stream = new TwitterStreamFactory(twitterConfig).getInstance()

  def initLogger() {
    Logger.reset()
    log.info("STARTING UP")
  }

  def startReciprocation() {
    timer.schedule(5.seconds.fromNow, 10.minutes) {
      // don't unfollow back; the Listener will unfollow if necessary instead of replying
      socialGraph.reciprocate(followBack = true, unfollowBack = false)
    }
  }

  def start() {
    initLogger()
    stream.addListener(listener)
    stream.user()
    startReciprocation()
  }

  start()
}