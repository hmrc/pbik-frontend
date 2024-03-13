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

package views

import models.{AuthenticatedRequest, Bik}
import org.jsoup.Jsoup
import play.twirl.api.Html
import utils.BikListUtils
import views.helper.PBIKViewSpec
import views.html.Summary

//scalastyle:off magic.number
class SummaryViewSpec extends PBIKViewSpec {

  implicit val bikListUtils: BikListUtils = app.injector.instanceOf[BikListUtils]
  val summaryView: Summary                = app.injector.instanceOf[Summary]

  def view(benefits: List[Bik])(implicit request: AuthenticatedRequest[_]): Html =
    summaryView(cyAllowed = true, taxYearRange, List(), benefits, 200, 0)

  "overview with benefits - organisation" must {
    implicit val html: Html = view(List(Bik("31", 30)))(organisationRequest)

    behave like pageWithTitle(messages("Overview.benefitsRegistered.heading"))
    behave like pageWithHeader(messages("Overview.benefitsRegistered.heading"))
    behave like pageWithLink(messages("Overview.table.add.link"), "/payrollbik/cy/choose-benefit-expense")
    behave like pageWithBackLink()

    "overview with no benefits" in {

      val doc = Jsoup.parse(view(List.empty)(organisationRequest).toString())
      doc.title must include(messages("Overview.benefitsRegistered.heading"))
      doc       must haveHeadingWithText(messages("Overview.benefitsRegistered.heading"))
    }
  }

  "overview with benefits - Agent" must {
    implicit val html: Html = view(List(Bik("31", 30)))(agentRequest)

    behave like pageWithTitle(messages("Overview.benefitsRegistered.heading"))
    behave like pageWithHeader(messages("Overview.benefitsRegistered.heading"))
    behave like pageWithLink(messages("Overview.table.add.link"), "/payrollbik/cy/choose-benefit-expense")
    behave like pageWithBackLink()

    "overview with no benefits" in {

      val doc = Jsoup.parse(view(List.empty)(agentRequest).toString())
      doc.title must include(messages("Overview.benefitsRegistered.heading"))
      doc       must haveHeadingWithText(messages("Overview.benefitsRegistered.heading"))
    }
  }

}
