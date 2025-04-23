/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import base.FakePBIKApplication
import play.api.Logging
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.Helpers.baseApplicationBuilder.injector

import scala.util.matching.Regex

class MessagesSpec extends FakePBIKApplication with Logging {

  private lazy val displayLine = "\n" + ("@" * 42) + "\n"

  implicit lazy val messages: Messages                  = messagesApi.preferred(Seq(lang, cyLang))
  private lazy val defaultMessages: Map[String, String] = getExpectedMessages("default")
  private lazy val welshMessages: Map[String, String]   = getExpectedMessages("cy")

  "Application" should {
    "have the correct message configs" in {
      messagesApi.messages.size mustBe 4
      messagesApi.messages.keys must contain theSameElementsAs Vector("en", "cy", "default", "default.play")
    }

    "have messages for default and cy only" in {
      messagesApi.messages("en").size mustBe 0
      val englishMessageCount = messagesApi.messages("default").size

      messagesApi.messages("cy").size mustBe englishMessageCount
    }
  }

  "All message files" should {
    "have the same set of keys" in
      withClue(mismatchingKeys(defaultMessages.keySet, welshMessages.keySet)) {
        assert(welshMessages.keySet equals defaultMessages.keySet)
      }
    "not have the same messages" in {
      val same = defaultMessages.keys.collect {
        case messageKey
            if defaultMessages.get(messageKey) == welshMessages.get(messageKey) && !messageKey.contains(".url") =>
          (messageKey, defaultMessages.get(messageKey))
      }

      // 94% of app needs to be translated into Welsh. 94% allows for:
      //   - Messages which just can't be different from English
      //     E.g. addresses, acronyms, numbers, etc.
      //   - Content which is pending translation to Welsh
      same.size.toDouble / defaultMessages.size.toDouble < 0.06 mustBe true
    }
    "have a non-empty message for each key" in {
      assertNonEmptyValuesForDefaultMessages()
      assertNonEmptyValuesForWelshMessages()
    }
    "have no unescaped single quotes in value" in {
      assertCorrectUseOfQuotesForDefaultMessages()
      assertCorrectUseOfQuotesForWelshMessages()
    }
    "have a resolvable message for keys which take args" in {
      val englishWithArgsMsgKeys = defaultMessages collect {
        case (messagesKey, messagesValue) if countArgs(messagesValue) > 0 => messagesKey
      }
      val welshWithArgsMsgKeys   = welshMessages collect {
        case (messagesKey, messagesValue) if countArgs(messagesValue) > 0 => messagesKey
      }
      val missingFromEnglish     = englishWithArgsMsgKeys.toList diff welshWithArgsMsgKeys.toList
      val missingFromWelsh       = welshWithArgsMsgKeys.toList diff englishWithArgsMsgKeys.toList
      missingFromEnglish foreach { messagesKey =>
        logger.error(s"Key which has arguments in English but not in Welsh: $messagesKey")
      }
      missingFromWelsh foreach { messagesKey =>
        logger.error(s"Key which has arguments in Welsh but not in English: $messagesKey")
      }
      englishWithArgsMsgKeys.size mustBe welshWithArgsMsgKeys.size
    }
    "have the same args in the same order for all keys which take args" in {
      val englishWithArgsMsgKeysAndArgList = defaultMessages collect {
        case (messageKey, messageValue) if countArgs(messageValue) > 0 => (messageKey, listArgs(messageValue))
      }
      val welshWithArgsMsgKeysAndArgList   = welshMessages collect {
        case (messageKey, messageValue) if countArgs(messageValue) > 0 => (messageKey, listArgs(messageValue))
      }
      val mismatchedArgSequences           = englishWithArgsMsgKeysAndArgList collect {
        case (messageKey, engArgSeq) if engArgSeq != welshWithArgsMsgKeysAndArgList(messageKey) =>
          (messageKey, engArgSeq, welshWithArgsMsgKeysAndArgList(messageKey))
      }
      mismatchedArgSequences foreach { case (key, engArgSeq, welshArgSeq) =>
        logger.error(
          s"key which has different arguments or order of arguments between English and Welsh:" +
            s" $key -- English arg seq=$engArgSeq and Welsh arg seq=$welshArgSeq"
        )
      }

      mismatchedArgSequences must be(empty)
    }
  }
  val MatchSingleQuoteOnly: Regex = """\w+'{1}\w+""".r
  val MatchBacktickQuoteOnly: Regex = """`+""".r

  private def countArgs(msg: String) = toArgArray(msg).length

  private def toArgArray(msg: String) = msg.split("[{}]").map(_.trim()).filter(isInteger)

  private def isInteger(s: String): Boolean = s forall Character.isDigit

  private def listArgs(msg: String) = toArgArray(msg).mkString

  private def assertNonEmptyValuesForDefaultMessages(): Unit =
    assertNonEmptyNonTemporaryValues("Default", defaultMessages)

  private def assertNonEmptyValuesForWelshMessages(): Unit = assertNonEmptyNonTemporaryValues("Welsh", welshMessages)

  private def assertNonEmptyNonTemporaryValues(label: String, messages: Map[String, String]): Unit = messages.foreach {
    case (key: String, value: String) =>
      withClue(s"In $label, there is an empty value for the key:[$key][$value]") {
        value.trim.isEmpty mustBe false
      }
  }

  private def assertCorrectUseOfQuotesForDefaultMessages(): Unit = assertCorrectUseOfQuotes("Default", defaultMessages)

  private def assertCorrectUseOfQuotes(label: String, messages: Map[String, String]): Unit = messages.foreach {
    case (key: String, value: String) =>
      withClue(s"In $label, there is an unescaped or invalid quote:[$key][$value]") {
        MatchSingleQuoteOnly.findFirstIn(value).isDefined mustBe false
        MatchBacktickQuoteOnly.findFirstIn(value).isDefined mustBe false
      }
  }

  private def assertCorrectUseOfQuotesForWelshMessages(): Unit = assertCorrectUseOfQuotes("Welsh", welshMessages)

  private def getExpectedMessages(languageCode: String) =
    messagesApi.messages.getOrElse(languageCode, throw new Exception(s"Missing messages for $languageCode"))

  def messagesApi: MessagesApi = injector().instanceOf[MessagesApi]

  private def mismatchingKeys(defaultKeySet: Set[String], welshKeySet: Set[String]) = {
    val test1 =
      listMissingMessageKeys("The following message keys are missing from Welsh Set:", defaultKeySet.diff(welshKeySet))
    val test2 = listMissingMessageKeys(
      "The following message keys are missing from English Set:",
      welshKeySet.diff(defaultKeySet)
    )

    test1 ++ test2
  }

  private def listMissingMessageKeys(header: String, missingKeys: Set[String]) =
    missingKeys.toList.sorted.mkString(header + displayLine, "\n", displayLine)
}
