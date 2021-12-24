/*
 * Copyright 2021 HM Revenue & Customs
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

package views

import models.{Bik, EmpRef, TaxYearRange}
import org.jsoup.Jsoup
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.BikListUtils
import views.helper.PBIKViewSpec
import views.html.Summary

//scalastyle:off magic.number
class SummaryViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val bikListUtils: BikListUtils = app.injector.instanceOf[BikListUtils]
  val summaryView: Summary = app.injector.instanceOf[Summary]

  def taxYearRange: TaxYearRange = TaxYearRange(2018, 2019, 2020)

  override def view: Html =
    summaryView(cyAllowed = true, taxYearRange, List(), List(Bik("31", 30)), 200, 0, "", EmpRef("", ""))

  def viewWithNoBenefits(): Html =
    summaryView(cyAllowed = true, taxYearRange, List(), List(), 200, 0, "", EmpRef("", ""))

  "overview with benefits" must {
    behave like pageWithTitle(messages("Overview.benefitsRegistered.heading"))
    behave like pageWithHeader(
      messages("Overview.next.heading", taxYearRange.cy + "", taxYearRange.cyplus1 + "")
        + " " + messages("Overview.benefitsRegistered.heading"))
    behave like pageWithLink(messages("Overview.table.add.link"), "/payrollbik/cy/choose-benefit-expense")
    behave like pageWithBackLink()
  }

  "overview with no benefits" in {

    val doc = Jsoup.parse(viewWithNoBenefits().toString())
    doc.title must include(messages("Overview.noBenefitsRegistered.title"))
    doc must haveHeadingWithText(
      messages("Overview.next.heading", taxYearRange.cy + "", taxYearRange.cyplus1 + "")
        + " " + messages("Overview.noBenefitsRegistered.heading"))
  }

}
