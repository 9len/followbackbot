package nu.glen

import com.twitter.logging.Logger
import com.twitter.util.Try
import twitter4j.{Status, StatusUpdate}

package object followbackbot {
  type Responder = Status => Option[StatusUpdate]
  type SimpleResponder = String => Option[String]

  trait SimpleLogger {
    val log = Logger.get(getClass.getSimpleName)

    def tryAndLogResult[T](formatter: String, args: Any*)(f: => T): Try[T] = {
      log.info(formatter, args: _*)
      Try(f) onSuccess { _ =>
        log.info(" Succeded.")
      } onFailure { case t =>
        log.info(t, " Failed: %s", t.getMessage)
      }
    }
  }
}