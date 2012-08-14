package nu.glen.followbackbot

import annotation.tailrec
import com.twitter.util.{Return, Throw, Try}
import twitter4j._

/**
 * takes care of various social graph-related actions, such as following and unfollowing
 *
 * @param userId the userId for the bot
 * @param twitter the twitter4j.Twitter instance
 * @param blacklist (optional) a list of userIds to never follow
 */
class SocialGraph(userId: Long, twitter: Twitter, blacklist: Set[Long]) extends SimpleLogger {
  /**
   * Ensure that everyone following me is followed by me, unfollow anyone who no loner follows me
   */
  def reciprocate(): Unit = synchronized {
    val allFollowers = followers()
    val allFollowing = following()

    log.debug("Followers: %s", allFollowers)
    log.debug("Following: %s", allFollowing)

    val toFollow = allFollowers -- allFollowing
    val toUnfollow = allFollowing -- allFollowers

    log.info("To Follow: %s", toFollow)
    withUserIds(toFollow)(follow(_, None, false))

    log.info("To Unfollow: %s", toUnfollow)
    withUserIds(toUnfollow)(unfollow(_))
  }

  /**
   * Is the user blacklisted?
   *
   * @param target the userId to inspect
   */
  def isBlacklisted(target: Long): Boolean = blacklist.contains(target)

  /**
   * Is the user following me?
   *
   * @param target the userId to inspect
   */
  def isFollowing(target: Long): Boolean =
    twitter.showFriendship(userId, target).isSourceFollowingTarget

  /**
   * Do I follow the user?
   *
   * @param target the userId to inspect
   */
  def isFollowedBy(target: Long): Boolean =
    twitter.showFriendship(target, userId).isSourceFollowingTarget

  /**
   * Follow a user. Will not follow if already following, or if the user is protected
   * and a follow request has already been sent.
   *
   * The two current use cases for this method are:
   *  - reciprocation, in which we don't need to check that the user is following
   *  - onFollow from the userStream, in which we need to check if the user is following, and
   *    we know that the user is or isn't protected, but don't know if a follow request has
   *    been sent, because the stream doesn't reliably include that field.
   *
   * @param the target the userId to follow
   * @param isProtected if known, whether or not the user is protected
   * @param checkAlreadyFollowed check to see if the user already followed, don't follow if so
   */
  def follow(
    target: Long,
    isProtected: Option[Boolean],
    checkAlreadyFollowed: Boolean
  ): Unit = synchronized {
    if (isBlacklisted(target)) {
      log.info(" userId is blacklisted: %s", target)
    } else if (checkAlreadyFollowed && isFollowing(target)) {
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
   * Check to see if a user is still following me and if not, unfollow the user.
   *
   * @param target the userId to inspect
   * @param msg a message to log
   * @param args args for that message
   *
   * @return Return(true) if still follows, Return(false) if not, Throw(_) on exception
   */
  def checkOrUnfollow(target: Long): Try[Boolean] = synchronized {
    tryAndLogResult(" Making sure user still follows me") {
      if (isBlacklisted(target)) {
        // otherwise, destroy the mutual follow
        log.info(" Blacklisted, unfollowing")
        unfollow(target)
        false
      } else if (!isFollowedBy(target)) {
        // otherwise, destroy the mutual follow
        log.info(" No longer following me, unfollowing")
        unfollow(target)
        false
      } else {
        true
      }
    }
  }

  /**
   * Get the full Set of users who follow me
   */
  def followers(): Set[Long] = getAllIds(twitter.getFollowersIDs) -- blacklist

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
    @tailrec
    def dispatch(cursor: Long, accum: Set[Long]): Try[Set[Long]] = {
      // we use an explicit match here rather than a flatMap so that
      // we can make it tail recursive
      Try(f(cursor)) match {
        case Throw(t) => Throw(t)
        case Return(ids) =>
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
