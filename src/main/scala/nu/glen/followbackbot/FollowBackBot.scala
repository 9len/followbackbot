package nu.glen.followbackbot

import com.twitter.logging.Logger
import com.twitter.conversions.time._
import com.twitter.util.{JavaTimer, Timer}
import org.yaml.snakeyaml.Yaml
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
  Logger.reset()
  log.info("STARTING UP")

  val credentialFilename = "credentials.yml"

  val credentials = {
    val credentialYaml = io.Source.fromFile(credentialFilename).mkString
    val yaml = new Yaml

    val credentialMap = yaml.load(credentialYaml).asInstanceOf[java.util.Map[String, String]]

    Credentials(
      credentialMap.get("screen_name"),
      credentialMap.get("token"),
      credentialMap.get("secret"),
      credentialMap.get("consumer_key"),
      credentialMap.get("consumer_secret")
    )
  }

  val twitter = new TwitterFactory().getInstance()
  twitter.setOAuthConsumer(credentials.consumerKey, credentials.consumerSecret)
  twitter.setOAuthAccessToken(new AccessToken(credentials.token, credentials.secret))

  val fixer = new Fixer(twitter)
  val listener = new Listener(credentials.screenName, responder, fixer.follow, twitter)

  val stream = new TwitterStreamFactory().getInstance()
  stream.setOAuthConsumer(credentials.consumerKey, credentials.consumerSecret)
  stream.setOAuthAccessToken(new AccessToken(credentials.token, credentials.secret))
  stream.addListener(listener)
  stream.user()

  timer.schedule(5.seconds.fromNow, 10.minutes) {
    fixer()
  }
}