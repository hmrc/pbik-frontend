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

import base.FakePBIKApplication
import models.TaxYearRange
import models.auth.AuthenticatedRequest
import models.v1.exclusion.PbikExclusionPerson
import models.v1.trace.TracePersonResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.twirl.api.Html

trait PBIKViewBehaviours extends FakePBIKApplication with JsoupMatchers {

  def doc(implicit view: Html): Document = Jsoup.parse(view.toString())

  def pageWithTitle(titleText: String)(implicit view: Html): Unit =
    "have a static title" in {
      doc.title must include(titleText)
    }

  def pageWithHeader(headerText: String)(implicit view: Html): Unit =
    "have a static h1 header" in {
      doc must haveHeadingWithText(headerText)
    }

  def pageWithHeaderH2(headerText: String)(implicit view: Html): Unit =
    "have a static h2 header" in {

      doc must haveHeadingH2WithText(headerText)
    }

  def pageWithBackLink()(implicit view: Html): Unit =
    "have a back link" in {
      doc must haveBackLink
    }

  def pageWithIdAndText(pageText: String, id: String)(implicit view: Html): Unit =
    s"have a static text ($pageText) with id ($id)" in {
      doc must haveElementWithIdAndText(pageText, id)
    }

  def pageWithYesNoRadioButton(idYes: String, idNo: String)(implicit view: Html): Unit =
    "have a yes/no radio button" in {
      doc.getElementById(idYes) must not be None.orNull
      doc.getElementById(idNo)  must not be None.orNull
    }

  def pageWithLink(text: String, href: String)(implicit view: Html): Unit =
    s"have a link with url $href and text $text" in {
      val a = doc.select(s"a[href=$href]").first()
      a must not be None.orNull
      a.text.trim mustBe text.trim
    }

  def pageWithLinkHiddenText(linkId: String, expectedText: String)(implicit view: Html): Unit =
    s"have a link with with id=$linkId and hidden text $expectedText" in {
      val a = doc.select(s"#$linkId .govuk-visually-hidden").first()
      a must not be None.orNull
      a.text.trim mustBe expectedText.trim
    }

  def pageWithTextBox(id: String, label: String)(implicit view: Html): Unit =
    s"have  a text box with label $label" in {
      doc must haveInputLabelWithText(id, label)
    }

  def pageWithConfirmAndContinueButtonAndLinkAndText(id: String, link: String, text: String)(implicit
    view: Html
  ): Unit =
    s"have a link with id=$id and href=$link" in {
      val a = doc.select(s"a[id=$id]").first()
      a must not be None.orNull
      a.attr("href") mustBe link
      a.text().trim mustBe text
    }

  def pageWithContinueButtonForm(submitUrl: String, buttonText: String)(implicit view: Html): Unit =
    pageWithButtonForm(submitUrl, buttonText)

  def nonBreakable(string: String): String = string.replace(" ", "\u00A0")

  def pageWithButtonForm(submitUrl: String, buttonText: String)(implicit view: Html): Unit = {
    "have a form with a submit button or input labelled as buttonText" in {
      doc must haveSubmitButton(buttonText)
    }
    "have a form with the correct submit url" in {
      doc must haveFormWithSubmitUrl(submitUrl)
    }
  }

  def pageWithElementAndText(id: String, text: String)(implicit view: Html): Unit =
    "have a form with element of the corresponding id" in {
      doc must haveElementWithIdAndText(text, id)
    }
}

trait PBIKBaseViewSpec extends FakePBIKApplication {

  val organisationRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    createAuthenticatedRequest(mockRequest, client = organisationClient, userId = Some("tester"))
  val agentRequest: AuthenticatedRequest[AnyContentAsEmpty.type]        =
    createAuthenticatedRequest(mockRequest, client = agentClient, userId = Some("tester"))
  implicit val messages: Messages                                       = injected[MessagesApi].preferred(Seq(lang))
  val cyMessages: Messages                                              = injected[MessagesApi].preferred(Seq(cyLang))

  val (year2018, year2019, year2020): (Int, Int, Int) = (2018, 2019, 2020)

  def taxYearRange: TaxYearRange = TaxYearRange(year2018, year2019, year2020)

  val exclusionPerson: PbikExclusionPerson =
    PbikExclusionPerson("AB123456C", "John", Some("A"), "Doe", Some("12345"), 22)
  val tracePerson: TracePersonResponse     = TracePersonResponse("AB123456C", "John", Some("A"), "Doe", Some("12345"), 22)

}

trait PBIKViewSpec extends PBIKBaseViewSpec with PBIKViewBehaviours
