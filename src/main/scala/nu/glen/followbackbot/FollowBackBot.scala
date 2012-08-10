package nu.glen.followbackbot

import com.twitter.logging.Logger
import com.twitter.conversions.time._
import com.twitter.util.{JavaTimer, Timer}
import twitter4j._
import twitter4j.auth.AccessToken

case class Credentials(
    screenName: String,
    token: String,
    secret: String,
    consumerKey: String,
    consumerSecret: String)

class FollowBackBot(
    responder: Responder,
    timer: Timer = new JavaTimer)
  extends SimpleLogger
{
  // override if you have some other way to get credentials
  val credentials =
    Credentials(
      System.getenv("TWITTER_SCREEN_NAME"),
      System.getenv("TWITTER_ACCESS_TOKEN"),
      System.getenv("TWITTER_TOKEN_SECRET"),
      System.getenv("TWITTER_CONSUMER_KEY"),
      System.getenv("TWITTER_CONSUMER_SECRET")
    )

  val twitter = {
    val twitter = new TwitterFactory().getInstance()
    twitter.setOAuthConsumer(credentials.consumerKey, credentials.consumerSecret)
    twitter.setOAuthAccessToken(new AccessToken(credentials.token, credentials.secret))
    twitter
  }

  val userId = twitter.showUser(credentials.screenName).getId

  val socialGraph = new SocialGraph(userId, twitter)

  val listener = new Listener(userId, credentials.screenName, responder, socialGraph, twitter)

  val stream = {
    val stream = new TwitterStreamFactory().getInstance()
    stream.setOAuthConsumer(credentials.consumerKey, credentials.consumerSecret)
    stream.setOAuthAccessToken(new AccessToken(credentials.token, credentials.secret))
    stream
  }

  def initLogger() {
    Logger.reset()
    log.info("STARTING UP")
  }

  def startReciprocation() {
    timer.schedule(5.seconds.fromNow, 10.minutes) {
      socialGraph.reciprocate()
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