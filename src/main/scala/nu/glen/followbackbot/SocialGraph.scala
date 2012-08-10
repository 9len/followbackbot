package nu.glen.followbackbot

import com.twitter.util.{Return, Try}
import twitter4j._

/**
 * This is a little shim to help us unit test ifFollowing more easily
 */
trait Action extends (() => Unit)

class SocialGraph(userId: Long, twitter: Twitter) extends SimpleLogger {
  /**
   * Ensure that everyone following me is followed by me, unfollow anyone who no loner follows me
   */
  def reciprocate(): Unit = synchronized {
    val allFollowers = followers()
    val allFollowing = following()

    log.debug("Followers: %s", allFollowers)
    log.debug("Following: %s", allFollowing)

    val toFollow = allFollowers.diff(allFollowing)
    val toUnfollow = allFollowing.diff(allFollowers)

    log.info("To Follow: %s", toFollow)
    withUserIds(toFollow)(follow(_))

    log.info("To Unfollow: %s", toUnfollow)
    withUserIds(toUnfollow)(unfollow(_))
  }

  /**
   * Is the user following me?
   *
   * @param target the userId to inspect
   */
  def following(target: Long): Boolean =
    twitter.showFriendship(userId, target).isSourceFollowingTarget

  /**
   * Do I follow the user?
   *
   * @param target the userId to inspect
   */
  def followedBy(target: Long): Boolean =
    twitter.showFriendship(target, userId).isSourceFollowingTarget

  /**
   * Follow a user. Will not follow if already following, or if the user is protected
   * and a follow request has already been sent.
   *
   * @param the target the userId to follow
   * @param isProtected if known, whether or not the user is protected
   */
  def follow(target: Long, isProtected: Option[Boolean] = None): Unit = synchronized {
    if (following(target)) {
      log.info(" Already following %s", target)
    } else {
      val followRequestAlreadySent = isProtected match {
        case Some(true) | None =>
          // if the user is protected, we need to check to see if we've already asked to follow.
          val user = twitter.showUser(target)
          user.isProtected && user.isFollowRequestSent

        case Some(false) => false
      }

      if (followRequestAlreadySent) {
        log.info(" Follow request already sent.")
      } else {
        log.info(" Following %s", target)
        twitter.createFriendship(target)
      }
    }
  }

  /**
   * unfollow a user
   * @param target the userId to unfollow
   */
  def unfollow(target: Long): Unit = synchronized {
    twitter.destroyFriendship(target)
  }

  /**
   * Check to see if a user is still following me, and perform an action if so.
   * If the user is no longer following me, unfollow the user.
   *
   * @param target the userId to inspect
   * @param msg a message to log
   * @param args args for that message
   *
   * @param f the action to perform
   */
  def ifFollowing(target: Long, f: Action, msg: String, args: Any*): Unit = synchronized {
    tryAndLogResult(msg, args) {
      log.info(" Making sure user still follows me")
      if (followedBy(target)) {
        f()
      } else {
        // otherwise, destroy the mutual follow
        log.info(" No longer following me, unfollowing")
        twitter.destroyFriendship(target)
      }
    }
  }

  /**
   * Get the full Set of users who follow me
   */
  def followers(): Set[Long] = getAllIds(twitter.getFollowersIDs)

  /**
   * Get the full Set of users who I follow
   */
  def following(): Set[Long] = getAllIds(twitter.getFriendsIDs)

  /**
   * helper method for safetly operating over a list of userIds
   *
   * @param userIds the userIds on which to operate
   *
   * @param f the operation to call
   */
  protected[this] def withUserIds(userIds: Iterable[Long])(f: Long => Unit) {
    userIds foreach { userId =>
      tryAndLogResult("Acting on: %s", userId) {
        f(userId)
      }
    }
  }

  /**
   * helper method to get a full set of userIds based on a method that takes
   * a cursor and returns a twitter4j IDs class.
   *
   * @param f the cursor method
   */
  protected[this] def getAllIds(f: Long => IDs): Set[Long] = {
    def dispatch(cursor: Long, accum: Set[Long]): Try[Set[Long]] = {
      Try(f(cursor)) flatMap { ids =>
        val idSet = ids.getIDs.toSet ++ accum

        if (ids.hasNext)
          dispatch(ids.getNextCursor, idSet)
        else
          Return(idSet)
      }
    }

    dispatch(CursorSupport.START, Set.empty) onFailure { case t =>
      log.info(t, "Couldn't get follows: %s", t.getMessage)
    } getOrElse(Set.empty)
  }
}
