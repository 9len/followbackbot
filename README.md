FollowBackBot
=============

FollowBackBot is a scala library for twitter API bots that follow/unfollow back and @reply based on custom criterea. Because they only respond to people who follow them, these bots adhere to Twitter's terms of service for automated @replying.

FollowBackBot uses [Twitter4j](http://twitter4j.org) to access both the REST and streaming Twitter APIs. The UserStream is used to monitor follows and statuses, and a periodic "reciprocity" process runs to make sure follow/unfollows are respected.

A `Responder` class defines what statuses are responded to, and is simply a type alias for `twitter4j.Status => Option[twitter4j.StatusUpdate]`. A `SimpleResponder` class, which is a type alias for `String => Option[String]`, can also be used, with the `Responder.simple(_)` method, which takes care of truncation, proper handling of retweeted text, and setting `in_reply_to_status_id`.

An example is provided, the source code for the [@YourMomBot](http://twitter.com/yourmombot) account. Creating a new bot is as simple as defining a single function:

    package nu.glen.yourmombot

    import nu.glen.followbackbot.{FollowBackBot, Responder, SimpleResponder}

    object YourMomResponder extends SimpleResponder {
      private[this] val Regex = ".*?\\b([a-z\\-A-Z]+[iI][nN][gG])\\b(.*)".r

      /**
       * extremely naive for now. just extracts tweets with words ending in "ing"
       */
      def apply(status: String): Option[String] = status match {
        case Regex(gerrand, rest) => Some("Your mom's %s%s".format(gerrand.toLowerCase, rest))
        case _ => None
      }
    }

    object YourMomBot extends FollowBackBot(Responder.simple(YourMomResponder)) with App

FollowBackBot authenticates with OAuth, and requires the presense of the following system properties or environment variables:

    TWITTER_SCREEN_NAME
    TWITTER_ACCESS_TOKEN
    TWITTER_TOKEN_SECRET
    TWITTER_CONSUMER_KEY
    TWITTER_CONSUMER_SECRET