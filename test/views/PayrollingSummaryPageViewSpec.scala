/*
 * Copyright 2026 HM Revenue & Customs
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

import models.auth.AuthenticatedRequest
import models.v1.{BenefitInKindWithCount, IabdType}
import org.jsoup.Jsoup
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.PayrollingSummaryPageMpbik

import scala.language.postfixOps

class PayrollingSummaryPageViewSpec extends PBIKViewSpec {

  private val payrollingSummaryPageView: PayrollingSummaryPageMpbik = injected[PayrollingSummaryPageMpbik]
  private val carIabdType: String                                   = IabdType.CarBenefit.id.toString
  private val carBik: BenefitInKindWithCount                        = BenefitInKindWithCount(IabdType.CarBenefit, 2)
  private val medicalBik: BenefitInKindWithCount                    = BenefitInKindWithCount(IabdType.MedicalInsurance, 0)

  private def view(
    benefits: List[BenefitInKindWithCount]
  )(implicit request: AuthenticatedRequest[?]): Html =
    payrollingSummaryPageView(taxYearRange.cy, benefits)

  private def testView(userType: String)(implicit request: AuthenticatedRequest[?]): Unit =
    s"overview for CY - $userType" must {
      implicit val html: Html = view(List(carBik, medicalBik))

      behave like pageWithTitle(messages("PayrollingSummaryMPBIK.heading"))
      behave like pageWithHeaderH2(messages("PayrollingSummaryMPBIK.tab.heading"))
      behave like pageWithBackLink()

      "not display empty benefits message when CY benefits are present" in {
        val doc          = Jsoup.parse(view(List(carBik, medicalBik)).toString)
        val emptyMessage = messages("Overview.empty.benefits.p")
        doc.body().text() must not include emptyMessage
      }

      "show links to exclude employees when " in {
        val doc                     = Jsoup.parse(view(List(medicalBik)).toString)
        val excludeLinkText: String = messages("PayrollingSummaryMPBIK.table.exclude.link.text")
        doc.body().text() must include(excludeLinkText)
      }

      "show links to manage employees when they had been excluded before" in {
        val doc                    = Jsoup.parse(view(List(carBik)).toString)
        val manageLinkText: String = messages("PayrollingSummaryMPBIK.table.manage.link.text")
        doc.body().text() must include(manageLinkText)
      }
    }

  // Run tests for both user types
  testView("organisation")(organisationRequest)
  testView("agent")(agentRequest)
}
