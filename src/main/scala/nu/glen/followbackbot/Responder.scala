package nu.glen.followbackbot

import twitter4j.{Status, StatusUpdate}

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

      new StatusUpdate(trimmed).inReplyToStatusId(status.getId)
    }
  }

  def logOnly(responder: Responder): Responder =
    new Responder with SimpleLogger {
      override def apply(status: Status): Option[StatusUpdate] = {
        responder.apply(status) flatMap { statusUpdate =>
          log.info("Would have tweeted: %s", statusUpdate.getStatus)
          None
        }
      }
    }

  def compose(thisResponder: Responder, thatResponder: Responder): Responder = { status =>
    thisResponder(status) orElse thatResponder(status)
  }
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
  def extract(statusText: String): Option[(String, String)]

  def process(keyword: String): String

  def combine(keyword: String, rest: String) = keyword + rest

  def apply(statusText: String): Option[String] = {
    extract(statusText) flatMap { case (keyword, rest) =>
      if (stopWords.contains(keyword)) {
        log.info("  Skipping tweet with stopword: %s", keyword)
        None
      } else {
        Some(combine(process(keyword), rest))
      }
    }
  }
}