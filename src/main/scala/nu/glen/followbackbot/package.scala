package nu.glen

import twitter4j.{Status, StatusUpdate}

package object followbackbot {
  type Responder = Status => Option[StatusUpdate]
  type SimpleResponder = String => Option[String]
}