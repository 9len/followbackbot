package nu.glen

import twitter4j.{Status, StatusUpdate, User}

package object followbackbot {
  /**
   * @param status the status
   * @return the optional status update response
   */
  type Responder = Status => Option[StatusUpdate]

  /**
   * @param statusText the text of a status
   * @return the optional status update response text
   */
  type SimpleResponder = String => Option[String]

  /**
   * @param user the user to potentially be rate limited
   * @return true if rate limited, false if not
   */
  type RateLimiter = User => Boolean
}