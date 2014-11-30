package nu.glen.followbackbot

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
 * A mix-in providing for a logger named for the class's simple name
 */
trait SimpleLogger {
  def name = getClass.getSimpleName

  lazy val log = Logger(LoggerFactory getLogger name)

  def tryAndLogResult[T](formatter: String)(f: => T): Try[T] = {
    log.info(formatter)
    val result = Try(f)
    result match {
      case Success(_) => log.info(" Succeded.")
      case Failure(t) => log.info(" Failed", t)
    }
    result
  }
}