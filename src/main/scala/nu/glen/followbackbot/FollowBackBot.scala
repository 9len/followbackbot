package nu.glen.followbackbot

import java.util.{Timer, TimerTask}

import twitter4j._
import twitter4j.conf.ConfigurationBuilder

import scala.concurrent.duration._
import scala.language.postfixOps

case class Credentials(screenName: String,
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
class FollowBackBot(responder: Responder,
                    blacklist: Seq[String] = Nil,
                    timer: Timer = new Timer)
  extends SimpleLogger {

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
    log.info("STARTING UP")
  }

  def startReciprocation() {
    timer.schedule(new TimerTask {
      def run() {
        // don't unfollow back; the Listener will unfollow if necessary instead of replying
        socialGraph.reciprocate(followBack = true, unfollowBack = false)
      }
    }, 5.seconds.toMillis, 10.minutes.toMillis)
  }

  def start() {
    initLogger()
    stream.addListener(listener)
    stream.user()
    startReciprocation()
  }

  start()
}