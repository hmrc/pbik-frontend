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

package views.helper

import org.jsoup.nodes.{Attributes, Document, Element}
import org.jsoup.select.Elements
import org.scalatest.matchers.{MatchResult, Matcher}

trait JsoupMatchers {

  import scala.jdk.CollectionConverters._

  class TagWithTextMatcher(expectedContent: String, tag: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val elements: List[String] =
        left
          .getElementsByTag(tag)
          .asScala
          .toList
          .map(_.text)

      lazy val elementContents = elements.mkString("\t", "\n\t", "")

      MatchResult(
        elements.contains(expectedContent),
        s"[$expectedContent] not found in '$tag' elements:[\n$elementContents]",
        s"'$tag' element found with text [$expectedContent]"
      )
    }
  }

  class CssSelector(selector: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val elements: Elements =
        left.select(selector)

      MatchResult(
        elements.size >= 1,
        s"No element found with '$selector' selector",
        s"${elements.size} elements found with '$selector' selector"
      )
    }
  }

  class CssSelectorWithTextMatcher(expectedContent: String, selector: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val elements: List[String] =
        left
          .select(selector)
          .asScala
          .toList
          .map(_.text)

      lazy val elementContents = elements.mkString("\t", "\n\t", "")

      MatchResult(
        elements.exists(_.contains(expectedContent)),
        s"[$expectedContent] not found in elements with '$selector' selector:[\n$elementContents]",
        s"[$expectedContent] element found with '$selector' selector and text [$expectedContent]"
      )
    }
  }

  class CssSelectorWithAttributeValueMatcher(attributeName: String, attributeValue: String, selector: String)
      extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val attributes: List[Attributes] =
        left
          .select(selector)
          .asScala
          .toList
          .map(_.attributes())

      lazy val attributeContents = attributes.mkString("\t", "\n\t", "")

      MatchResult(
        attributes.map(_.get(attributeName)).contains(attributeValue),
        s"[$attributeName=$attributeValue] not found in elements with '$selector' selector:[\n$attributeContents]",
        s"[$attributeName=$attributeValue] element found with '$selector' selector"
      )
    }
  }

  class CssSelectorWithClassMatcher(className: String, selector: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val classes: List[String] =
        left
          .select(selector)
          .asScala
          .toList
          .map(_.className())

      lazy val classContents = classes.mkString("\t", "\n\t", "")

      MatchResult(
        classes.exists(_.contains(className)),
        s"[class=$className] not found in elements with '$selector' selector:[\n$classContents]",
        s"[class=$className] element found with '$selector' selector"
      )
    }
  }

  class IdSelectorWithUrlMatcher(expectedContent: String, selector: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val elements: String =
        left.getElementById(selector).attr("href")

      lazy val elementContents = elements.mkString("\t", "\n\t", "")

      MatchResult(
        elements.contains(expectedContent),
        s"[$expectedContent] not found in elements with id '$selector':[\n$elementContents]",
        s"[$expectedContent] element found with id '$selector' and url [$expectedContent]"
      )
    }
  }

  class IdSelectorWithUrlAndTextMatcher(id: String, value: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val element: Element   = left.getElementById(id)
      val valueFound: String = element.attr("value")

      MatchResult(
        valueFound.contains(value),
        s"[url:$value] not found in element with id:'$id' \nInstead found:[url:$valueFound]",
        s"Element found with id '$id' and url [$value]"
      )
    }
  }

  def haveHeadingWithText(expectedText: String): TagWithTextMatcher = new TagWithTextMatcher(expectedText, "h1")

  def haveHeadingH2WithText(expectedText: String): TagWithTextMatcher = new TagWithTextMatcher(expectedText, "h2")

  def haveElementWithIdAndText(expectedText: String, id: String): CssSelectorWithTextMatcher =
    new CssSelectorWithTextMatcher(expectedText, s"#$id")

  def haveElementWithId(id: String): CssSelector = new CssSelector(s"#$id")

  def haveBackLink: CssSelector = new CssSelector("a[id=back-link]")

  def haveSubmitButton(expectedText: String): CssSelectorWithAttributeValueMatcher =
    new CssSelectorWithAttributeValueMatcher("value", expectedText, "input[type=submit]")

  def haveFormWithSubmitUrl(url: String): CssSelectorWithAttributeValueMatcher =
    new CssSelectorWithAttributeValueMatcher("action", url, "form[method=POST]")

  def haveInputLabelWithText(id: String, expectedText: String): CssSelectorWithTextMatcher =
    new CssSelectorWithTextMatcher(expectedText, s"label[for=$id]")

  def haveElementAtPathWithText(elementSelector: String, expectedText: String): CssSelectorWithTextMatcher =
    new CssSelectorWithTextMatcher(expectedText, elementSelector)

  def haveElementAtPathWithClass(elementSelector: String, className: String): CssSelectorWithClassMatcher =
    new CssSelectorWithClassMatcher(className, elementSelector)

  def haveErrorSummary(expectedText: String): CssSelectorWithTextMatcher =
    new CssSelectorWithTextMatcher(expectedText, ".govuk-list.govuk-error-summary__list")

  def haveErrorNotification(expectedText: String): CssSelectorWithTextMatcher =
    new CssSelectorWithTextMatcher(expectedText, ".govuk-error-message")

  def haveClassWithText(expectedText: String, className: String): CssSelectorWithTextMatcher =
    new CssSelectorWithTextMatcher(expectedText, s".$className")

  def haveLinkWithUrlWithID(id: String, expectedURL: String): IdSelectorWithUrlMatcher =
    new IdSelectorWithUrlMatcher(expectedURL, id)

  def haveValueElement(id: String, value: String): IdSelectorWithUrlAndTextMatcher =
    new IdSelectorWithUrlAndTextMatcher(id, value)
}
