package nu.glen.followbackbot

import util.matching.Regex

/**
 * Base class for SimpleResponders which extract a keyword which, when processed,
 * serves as the prefix for the rest of the extracted text. keywords are filtered against
 * a Set[String] of stop words.
 */
abstract class KeywordPrefixResponder(stopWords: Set[String])
  extends SimpleResponder
  with SimpleLogger
{
  /**
   * extract the keyword and remaining text from a status text
   *
   * @param statusText the text from which to extract
   */
  def extract(statusText: String): Option[(String, String)]

  /**
   * combine the keyword (now vetted against stopwords) with the rest of the
   * string to form the tweet.
   *
   * @param filteredKeyword the keyword, filtered against stop words
   * @param rest the rest of the sentence to be combined with the keyword
   */
  def combine(filteredKeyword: String, rest: String): String

  def apply(statusText: String): Option[String] = {
    extract(statusText) flatMap { case (keyword, rest) =>
      if (stopWords.contains(keyword)) {
        log.info(s" Skipping tweet with stop word: $keyword")
        None
      } else {
        Some(combine(keyword, rest))
      }
    }
  }
}

/**
 * for a given list of words, will try to find each in the status text, and use as
 * the keyword prefix. If more than once match, will prefer the one earliest in the
 * status text.
 */
abstract class IndexKeywordPrefixResponder extends KeywordPrefixResponder(Set.empty) {
  def words: Seq[String]

  lazy val paddedWords = words.distinct.sorted.map { " " + _ + " " }

  /**
   * will try every extraction, but prefer the longest one
   */
  override def extract(statusText: String) = {
    val earliest = paddedWords flatMap { word =>
      val index = statusText.indexOf(word)
      if (index > 0)
        Some((index, word.trim))
      else
        None
    } sortWith { case ((thisIndex, _), (thatIndex, _)) =>
      thisIndex < thatIndex
    } headOption

    earliest map { case (index, word) =>
      (word, statusText.substring(index + word.size + 1))
    }
  }
}

/**
 * Attempts to match various "to be", "to have", and "to do" verb combinations
 */
abstract class BeHaveDoKeywordPrefixResponder extends IndexKeywordPrefixResponder {
  val contractable =
    Seq(
      "could",
      "did",
      "does",
      "has",
      "is",
      "must",
      "should",
      "was",
      "would"
    )

  val uncontractable =
    Seq(
      "could",
      "did",
      "does",
      "has",
      "is",
      "shall",
      "should",
      "would"
    )

    val contractableNeedsAux =
    Seq(
      "can",
      "must"
    )

  val uncontractableNeedsAux =
    Seq(
      "may",
      "might",
      "must",
      "will"
    )

  val others =
    Seq(
      "can has",
      "cannot",
      "could've",
      "likes to",
      "ought to",
      "ought not to",
      "ought not",
      "shouldn've",
      "wants to",
      "would've"
    )

  val aux = Seq("be", "have")

  val needsAux =
    contractableNeedsAux ++ contractableNeedsAux.map { _ + "n't"} ++
    uncontractableNeedsAux ++ uncontractableNeedsAux.map { _ + " not"}

  val withAux = needsAux flatMap { word => aux.map { word + " " + _} }

  override lazy val words =
    contractable ++ contractable.map { _ + "n't" } ++
    uncontractable ++ uncontractable.map { _ + " not"} ++
    withAux ++ others
}

/**
 * A KeywordPrefixResponder that uses a regex for extraction
 */
abstract class RegexKeywordPrefixResponder(stopWords: Set[String], regex: Regex)
  extends KeywordPrefixResponder(stopWords)
{
  override def extract(statusText: String): Option[(String, String)] = statusText match {
     case regex(keyword, rest) => Some((keyword.toLowerCase, rest))
     case _ => None
  }
}

/**
 * A RegexKeywordPrefixResponder which (poorly) matches gerrunds as keywords
 */
abstract class GerrundKeywordPrefixResponder
  extends RegexKeywordPrefixResponder(
    Ing.stopWords,
    """.*?\b([a-z\-A-Z]+[iI][nN][gG])\b\"?(.*?)\"?""".r)

/**
 * A RegexKeywordPrefixResponder which (poorly) matches past-tense verbs as keywords
 */
abstract class PastTenseKeywordPrefixResponder
  extends RegexKeywordPrefixResponder(
    Ed.stopWords,
     """.*?\b([a-z\-A-Z]+[eE][dD])\b\"?(.*?)\"?""".r)
