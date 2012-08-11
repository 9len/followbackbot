package nu.glen.followbackbot

import com.twitter.logging.Logger
import com.twitter.util.Try

/**
 * A mix-in providing for a logger named for the class's simple name
 */
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