package nu.glen.followbackbot

import com.twitter.util.{Return, Try}
import twitter4j._
import scala.annotation.tailrec

case class Fixer(
    getFollowers: GetFollowers,
    getFollowing: GetFollowing,
    follow: Follow,
    unfollow: Unfollow)
  extends SimpleLogger
{
  def this(twitter: Twitter) =
    this(
      new GetFollowers(twitter),
      new GetFollowing(twitter),
      new Follow(twitter),
      new Unfollow(twitter)
    )

  def apply() {
    val followers = getFollowers()
    val following = getFollowing()

    log.info("Followers: %s", following)
    log.info("Following: %s", followers)

    val toFollow = followers.diff(following)
    val toUnfollow = following.diff(followers)

    log.info("To Follow: %s", toFollow)
    log.info("To Unfollow: %s", toUnfollow)

    unfollow(toUnfollow)
    follow(toFollow)
  }
}

object SetFollows {
  type Method = Long => User
}

abstract class SetFollows extends SimpleLogger {
  def set(target: Long): User

  def apply(targets: Set[Long]) {
    targets foreach { target =>
      tryAndLogResult("Acting on: %s", target) {
        set(target)
      }
    }
  }
}

class Follow(twitter: Twitter) extends SetFollows with SimpleLogger {
  def apply(screenName: String, target: User, targetIsHydrated: Boolean = false) {
    tryAndLogResult(" Following %s", target.getScreenName) {
      dispatch(Some(screenName), target, targetIsHydrated)
    }
  }

  protected[this] def dispatch(
    screenName: Option[String], target: User, targetIsHydrated: Boolean
  ): User = synchronized {
    val targetScreenName = target.getScreenName

    val following = screenName map { name =>
      val friendship = twitter.showFriendship(name, targetScreenName)
      friendship.isSourceFollowingTarget
    } getOrElse(false)

    if (following) {
      log.info(" Already following %s", targetScreenName)
      target
    } else {
      // if the user is protected, we need to check to see if we've already asked to follow.
      // if the user isn't fully hydrated, we'll need to make a showUser call to fill this in
      val user =
        if (target.isProtected && ! targetIsHydrated)
          twitter.showUser(targetScreenName)
        else
          target

      if (user.isFollowRequestSent) {
        log.info(" Follow request already sent.")
        target
      } else {
        log.info(" Following %s", targetScreenName)
        twitter.createFriendship(targetScreenName)
      }
    }
  }

  override def set(target: Long) = dispatch(None, twitter.showUser(target), true)
}

class Unfollow(twitter: Twitter) extends SetFollows {
  override def set(target: Long) = twitter.destroyFriendship(target)
}

object GetFollows {
  type Method = Long => IDs
}

class GetFollows(f: GetFollows.Method) extends SimpleLogger {
  def apply(): Set[Long] = {
    dispatch(CursorSupport.START, Set.empty) onFailure { case t =>
      log.info(t, "Couldn't get follows: %s", t.getMessage)
    } getOrElse(Set.empty)
  }

  private[this] def dispatch(cursor: Long, accum: Set[Long]): Try[Set[Long]] = {
    Try(f(cursor)) flatMap { ids =>
      val idSet = ids.getIDs.toSet ++ accum

      if (ids.hasNext)
        dispatch(ids.getNextCursor, idSet)
      else
        Return(idSet)
    }
  }
}

class GetFollowers(twitter: Twitter) extends GetFollows(twitter.getFollowersIDs)
class GetFollowing(twitter: Twitter) extends GetFollows(twitter.getFriendsIDs)