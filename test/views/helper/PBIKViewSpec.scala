/*
 * Copyright 2019 HM Revenue & Customs
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

import config.PbikContext
import connectors.ContactFrontendConnector
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait PBIKViewBehaviours extends PlaySpec with JsoupMatchers {

  def view: Html

  def doc: Document = Jsoup.parse(view.toString())

  def doc(view: Html): Document = Jsoup.parse(view.toString())

  def pageWithTitle(titleText: String): Unit = {
    "have a static title" in {
      doc.title must include(titleText)
    }
  }

  def pageWithHeader(headerText: String): Unit = {
    "have a static h1 header" in {
      doc must haveHeadingWithText(headerText)
    }
  }

  def pageWithHeaderH2(headerText: String): Unit = {
    "have a static h2 header" in {
      doc must haveHeadingH2WithText(headerText)
    }
  }

  def pageWithBackLink: Unit = {
    "have a back link" in {
      doc must haveBackLink
    }
  }

  def pageWithIdAndText(pageText: String, id: String): Unit = {
    s"have a static text ($pageText) with id ($id)" in {
      doc must haveElementWithIdAndText (pageText, id)
    }
  }


  def pageWithYesNoRadioButton(idYes: String, idNo:String){
    "have a yes/no radio button" in {

      doc.getElementById(idYes) must not be null
      doc.getElementById(idNo) must not be null

    }
  }

  def pageWithTextBox (id:String, label: String): Unit = {
    s"have  a text box with label $label" in {
      doc must haveInputLabelWithText (id , label)
    }
  }

  def pageWithLink(text: String, href: String): Unit = {
    s"have a link with url $href and text $text" in {
      val a = doc.select(s"a[href=$href]").first()
      a must not be null
      a.text.trim mustBe text.trim
    }
  }

  def pageWithContinueButtonForm(submitUrl: String, buttonText: String): Unit = {
    pageWithButtonForm(submitUrl, buttonText)
  }

  def nonBreakable(string: String): String = string.replace(" ", "\u00A0")

  def pageWithButtonForm(submitUrl: String, buttonText: String): Unit = {
    "have a form with a submit button or input labelled as buttonText" in {
      doc must haveSubmitButton(buttonText)
    }
    "have a form with the correct submit url" in {
      doc must haveFormWithSubmitUrl(submitUrl)
    }
  }
}

trait PBIKBaseViewSpec extends PlaySpec with GuiceOneAppPerSuite {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
  implicit val pbikContext: PbikContext = new PbikContext(mock(classOf[ContactFrontendConnector])) {
    override def getPageHelpPartial()(implicit hc: HeaderCarrier): Future[String] = Future.successful("")
  }
}

trait PBIKViewSpec extends PBIKBaseViewSpec with PBIKViewBehaviours

