/*
 * Copyright 2024 HM Revenue & Customs
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

import models.agent.{AccountsOfficeReference, Client}
import models.{AuthenticatedRequest, EmpRef, TaxYearRange, UserName}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.retrieve.Name

trait PBIKViewBehaviours extends PlaySpec with JsoupMatchers {

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

trait PBIKBaseViewSpec extends PlaySpec with GuiceOneAppPerSuite {

  val empRef: EmpRef = EmpRef("123", "AB12345")

  private val username: UserName = UserName(Name(Some("test"), Some("tester")))
  private val organisationClient = None
  private val agentClient        = Some(
    Client(
      uk.gov.hmrc.domain.EmpRef(empRef.taxOfficeNumber, empRef.taxOfficeReference),
      AccountsOfficeReference("123AB12345678", "123", "A", "12345678"),
      Some("client test name"),
      lpAuthorisation = false,
      None,
      None
    )
  )

  val organisationRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(empRef, username, FakeRequest(), organisationClient)
  val agentRequest: AuthenticatedRequest[AnyContentAsEmpty.type]        =
    AuthenticatedRequest(empRef, username, FakeRequest(), agentClient)
  implicit val messages: Messages                                       = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))
  val cyMessages: Messages                                              = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("cy")))

  val (year2018, year2019, year2020): (Int, Int, Int) = (2018, 2019, 2020)

  def taxYearRange: TaxYearRange = TaxYearRange(year2018, year2019, year2020)
}

trait PBIKViewSpec extends PBIKBaseViewSpec with PBIKViewBehaviours
