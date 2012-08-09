package nu.glen.followbackbot

import twitter4j.{Status, StatusUpdate}

object Responder {
  def simple(responder: SimpleResponder): Responder = {
    new Responder with SimpleLogger {
      override def apply(status: Status): Option[StatusUpdate] = {
        responder(status.getText) map { text =>
          val withReply = "@%s %s".format(status.getUser.getScreenName, text).trim

          val trimmed =
            if (withReply.size > 140)
              withReply.substring(0, 139).trim + "…"
            else
              withReply

          new StatusUpdate(trimmed).inReplyToStatusId(status.getId)
        }
      }
    }
  }
}