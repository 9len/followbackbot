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

      new StatusUpdate(trimmed)
    }
  }

  /**
   * invoke the Responder, log result (if any), but always return None
   *
   * @param responder the Responder to convert
   * @return the new Responder
   */
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

  /**
   * Avoid leaking private tweets via replies that might quote the tweet
   *
   * @param responder the Responder to convert
   * @return the new Responder
   */
  def ignoreProtectedUsers(responder: Responder): Responder =
    new Responder with SimpleLogger {
      override def name = responder.getClass.getName

      override def apply(status: Status): Option[StatusUpdate] = {
        if (status.getUser.isProtected) {
          log.info(" Ignoring status from protected user: %s", status.getUser.getScreenName)
          None
        } else {
          responder(status)
        }
      }
    }

  /**
   * rate limit responses to individual users
   *
   * @param responder the Responder to rate limit
   * @param rateLimiter the RateLimiter to use
   * @return the new Responder
   */
  def rateLimited(responder: Responder, rateLimiter: RateLimiter): Responder =
    (status) => responder(status) filterNot { _ => rateLimiter(status.getUser) }

  /**
   * merge Responders into a single Responder, preferring first response
   * @param responder the Responders to merge
   * @return the new Responder
   */
  def merged(responders: Responder*): Responder =
    mergedWithPreference(responders: _*) { (a, _) => a }

  /**
   * merge Responders into a single Responder, preferring longest response
   * @param responder the Responders to merge
   * @return the new Responder
   */
  def preferLongestResponse(responders: Responder*): Responder =
    mergedWithPreference(responders: _*) { (a, b) =>
      if (a.getStatus.length >= b.getStatus.length) a else b
    }

  /**
   * merge Responders into a single Responder
   * @param responder the Responders to merge
   * @param choose a function to choose between two StatusUpdates
   * @return the new Responder
   */
  def mergedWithPreference(
    responders: Responder*
  )(
    choose: (StatusUpdate, StatusUpdate) => StatusUpdate
  ): Responder = { status =>
    responders.foldLeft[Option[StatusUpdate]](None) { (accum, responder) =>
      (accum, responder(status)) match {
        case (Some(a), Some(b)) => Some(choose(a, b))
        case (Some(a), None) => Some(a)
        case (None, Some(b)) => Some(b)
        case (None, None) => None
      }
    }
  }
}
