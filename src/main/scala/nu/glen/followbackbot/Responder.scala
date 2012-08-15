package nu.glen.followbackbot

import twitter4j.{Status, StatusUpdate}
import util.matching.Regex

object Responder {
  /**
   * converts a SimpleResponder to a Responder, prefixing with the @reply,
   * and truncating the response if necessary
   *
   * @param responder the SimpleResponder to convert
   */
  def simple(responder: SimpleResponder): Responder = { status =>
    // get the untruncated retweet text if status is a retweet
    val text =
      if (status.isRetweet)
        status.getRetweetedStatus.getText
      else
        status.getText

    responder(text) map { response =>
      // add @reply prefix
      val withReply = "@%s %s".format(status.getUser.getScreenName, response).trim

      // trim to 140 chars, append an elipsis if > 140
      val trimmed =
        if (withReply.size > 140)
          withReply.substring(0, 139).trim + "…"
        else
          withReply

      (new StatusUpdate(trimmed)).inReplyToStatusId(status.getId)
    }
  }

  def logOnly(responder: Responder): Responder =
    new Responder with SimpleLogger {
      override def name = responder.getClass.getName

      override def apply(status: Status): Option[StatusUpdate] = {
        responder.apply(status) flatMap { statusUpdate =>
          log.info(" Would have tweeted: %s", statusUpdate.getStatus)
          None
        }
      }
    }

  def rateLimited(responder: Responder, rateLimiter: RateLimiter): Responder =
    (status) => responder(status) filterNot { _ => rateLimiter(status.getUser) }

  def merged(responders: Responder*): Responder =
    (status) => responders.foldLeft[Option[StatusUpdate]](None)(_ orElse _(status))
}

/**
 * Base class for SimpleResponders which extract a keyword which, when processed,
 * serves as the prefix for the rest of the extracted text. keywords are filtered against
 * a Set[String] of stop words.
 */
abstract class KeywordPrefixResponder(stopWords: Set[String])
  extends SimpleResponder
  with SimpleLogger
{
  /**
   * extract the keyword and remaining text from a status text
   *
   * @param statusText the text from which to extract
   */
  def extract(statusText: String): Option[(String, String)]

  /**
   * combine the keyword (now vetted against stopwords) with the rest of the
   * string to form the tweet.
   *
   * @param filteredKeyword the keyword, filtered against stop words
   * @param rest the rest of the sentence to be combined with the keyword
   */
  def combine(filteredKeyword: String, rest: String): String

  def apply(statusText: String): Option[String] = {
    extract(statusText) flatMap { case (keyword, rest) =>
      if (stopWords.contains(keyword)) {
        log.info("  Skipping tweet with stop word: %s", keyword)
        None
      } else {
        Some(combine(keyword, rest))
      }
    }
  }
}

/**
 * A KeywordPrefixResponder that uses a regex for extraction
 */
abstract class RegexKeywordPrefixResponder(stopWords: Set[String], regex: Regex)
  extends KeywordPrefixResponder(stopWords)
{
  override def extract(statusText: String): Option[(String, String)] = statusText match {
     case regex(keyword, rest) => Some((keyword.toLowerCase, rest))
     case _ => None
  }
}

/**
 * A RegexKeywordPrefixResponder which (poorly) matches gerrunds as keywords
 */
abstract class GerrundKeywordPrefixResponder
  extends RegexKeywordPrefixResponder(
    Ing.stopWords,
    """.*?\b([a-z\-A-Z]+[iI][nN][gG])\b\"?(.*?)\"?""".r)

/**
 * A RegexKeywordPrefixResponder which (poorly) matches past-tense verbs as keywords
 */
abstract class PastTenseKeywordPrefixResponder
  extends RegexKeywordPrefixResponder(
    Ed.stopWords,
     """.*?\b([a-z\-A-Z]+[eE][dD])\b\"?(.*?)\"?""".r)
