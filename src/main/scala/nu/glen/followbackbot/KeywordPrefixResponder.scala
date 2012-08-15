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
        log.info("  Skipping tweet with stop word: %s", keyword)
        None
      } else {
        Some(combine(keyword, rest))
      }
    }
  }
}

abstract class BeHaveDoKeywordPrefixResponder extends KeywordPrefixResponder(Set.empty) {
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

  val words =
    contractable ++ contractable.map { _ + "n't" } ++
    uncontractable ++ uncontractable.map { _ + " not"} ++
    withAux ++ others

  val paddedWords = words.map { " " + _ + " " }

  paddedWords.foreach(println(_))

  override def extract(statusText: String) = {
    paddedWords.collectFirst {
      case word if statusText.indexOf(word) > 0 =>
        val index = statusText.indexOf(word)
        (word.trim, statusText.substring(index + word.size - 1))
    }
  }
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
