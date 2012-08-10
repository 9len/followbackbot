package nu.glen.followbackbot

import com.twitter.util.{Return, Try}
import twitter4j._

class SocialGraph(userId: Long, twitter: Twitter) extends SimpleLogger {
  def reciprocate() {
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

  def following(target: Long): Boolean =
    twitter.showFriendship(userId, target).isSourceFollowingTarget

  def followedBy(target: Long): Boolean =
    twitter.showFriendship(target, userId).isSourceFollowingTarget

  def follow(target: Long, isProtected: Option[Boolean] = None) {
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

  def unfollow(userId: Long) {
    twitter.destroyFriendship(userId)
  }

  def ifFollowing(target: Long, msg: String, args: Any*)(f: => Any) {
    tryAndLogResult(msg, args: _*) {
      log.info(" Making sure user still follows me")
      if (followedBy(target)) {
        f
      } else {
        // otherwise, destroy the mutual follow
        log.info(" No longer following me, unfollowing")
        twitter.destroyFriendship(target)
      }
    }
  }

  def followers(): Set[Long] = getAllFollows(twitter.getFollowersIDs)

  def following(): Set[Long] = getAllFollows(twitter.getFriendsIDs)

  private[this] def withUserIds(userIds: Iterable[Long])(f: Long => Unit) {
    userIds foreach { userId =>
      tryAndLogResult("Acting on: %s", userId) {
        f(userId)
      }
    }
  }

  private[this] def getAllFollows(f: Long => IDs): Set[Long] = {
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
