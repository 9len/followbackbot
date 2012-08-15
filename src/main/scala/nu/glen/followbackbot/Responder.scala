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
      Option(
        if (status.isRetweet)
          status.getRetweetedStatus.getText
        else
          status.getText
      ).getOrElse("").trim

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
