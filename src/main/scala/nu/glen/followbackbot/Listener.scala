package nu.glen.followbackbot

import twitter4j._

class Listener(
    screenName: String,
    responder: Responder,
    follow: Follow,
    twitter: Twitter)
  extends UserStreamListener
  with SimpleLogger
{
  def isMe(sn: String) = sn.toLowerCase == screenName.toLowerCase

  def isMyOwnRetweet(status: Status) =
    status.getText.toLowerCase.startsWith("rt @" + screenName.toLowerCase)

  override def onStatus(status: Status) {
    val statusScreenName = status.getUser.getScreenName
    log.info("Got Status: @%s: %s", statusScreenName, status.getText)

    if (isMe(statusScreenName)) {
      log.info(" Ignoring my own status")
    } else if (isMyOwnRetweet(status)) {
      log.info(" Ignoring a retweet of my own status")
    } else {
      responder(status) match {
        case Some(statusUpdate) =>
          tryAndLogResult(" Replying with %s", statusUpdate.getStatus) {
            val statusScreenName = status.getUser.getScreenName

            log.info(" Making sure @%s still follows me", statusScreenName)
            if (twitter.existsFriendship(statusScreenName, screenName)) {
              // only send the reply if the tweeter still follows us
              log.info(" Tweeting: %s", statusUpdate.getStatus)
              twitter.updateStatus(statusUpdate)
            } else {
              // otherwise, destroy the mutual follow
              log.info(" No longer following me, unfollowing: %s", statusScreenName)
              twitter.destroyFriendship(statusScreenName)
            }
          }

        case None => log.info(" Ignoring ineligible status")
      }
    }
  }

  override def onFollow(source: User, followedUser: User) {
    val sourceScreenName = source.getScreenName
    log.info("Got follow notification: %s -> %s", sourceScreenName, followedUser.getScreenName)

    if (isMe(sourceScreenName)) {
      log.info(" Ignoring notification of my own actions")
    } else {
      follow(screenName, source)
    }
  }

  override def onException(ex: Exception) = ()

  override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) = ()
  override def onScrubGeo(userId: Long, upToStatusId: Long) = ()
  override def onTrackLimitationNotice(numberOfLimitedStatuses: Int) = ()

  override def onBlock(source: User, blockedUser: User) = ()
  override def onDeletionNotice(directMessageId: Long, userId: Long) = ()
  override def onDirectMessage(directMessage: DirectMessage) = ()
  override def onFavorite(source: User, target: User, favoritedStatus: Status) = ()
  override def onFriendList(friendIds: Array[Long]) = ()
  override def onRetweet(source: User, target: User, retweetedStatus: Status) = ()
  override def onUnblock(source: User, unblockedUser: User) = ()
  override def onUnfavorite(source: User, target: User, unfavoritedStatus: Status) = ()
  override def onUserListCreation(listOwner: User, list: UserList) = ()
  override def onUserListDeletion(listOwner: User, list: UserList) = ()
  override def onUserListMemberAddition(addedMember: User, listOwner: User, list: UserList) = ()
  override def onUserListMemberDeletion(deletedMember: User, listOwner: User, list: UserList) = ()
  override def onUserListSubscription(subscriber: User, listOwner: User, list: UserList) = ()
  override def onUserListUnsubscription(subscriber: User, listOwner: User, list: UserList) = ()
  override def onUserListUpdate(listOwner: User, list: UserList) = ()
  override def onUserProfileUpdate(updatedUser: User) = ()
}
