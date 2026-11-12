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
import views.html.PayrollingSummary

import scala.language.postfixOps

class PayrollingSummaryViewSpec extends PBIKViewSpec {

  private val summaryView: PayrollingSummary        = injected[PayrollingSummary]
  private val carBik: BenefitInKindWithCount        = BenefitInKindWithCount(IabdType.CarBenefit, 2)
  private val medicalBik: BenefitInKindWithCount    = BenefitInKindWithCount(IabdType.MedicalInsurance, 0)
  private val accommodation: BenefitInKindWithCount = BenefitInKindWithCount(IabdType.Accommodation, 3)

  private def view(
    benefits: List[BenefitInKindWithCount],
    nextYearBenefits: List[BenefitInKindWithCount] = List.empty[BenefitInKindWithCount]
  )(implicit request: AuthenticatedRequest[?]): Html =
    summaryView(taxYearRange.cy, benefits, nextYearBenefits, isExhaustedCY1 = false)

  private def testView(userType: String)(implicit request: AuthenticatedRequest[?]): Unit = {
    s"overview for CY with benefits and no benefits on CY1 - $userType"              must {
      implicit val html: Html = view(List(carBik, medicalBik))

      behave like pageWithTitle(messages("PayrollingSummary.heading"))
      behave like pageWithHeaderH2(messages("PayrollingSummary.tab.heading"))
      behave like pageWithBackLink()
      behave like pageWithLink(
        messages(s"PayrollingSummary.choose.benefit.link.text." + userType),
        "/payrollbik/cy1/choose-benefit-expense"
      )

      "not display empty benefits message when CY benefits are present" in {
        val doc          = Jsoup.parse(view(List(carBik, medicalBik), List(carBik, accommodation)).toString)
        val emptyMessage = "empty message"
        doc.body().text() must not include emptyMessage
      }

      "show links to exclude employees when exclusion count is 0" in {
        val doc                     = Jsoup.parse(view(List(medicalBik), List(accommodation)).toString)
        val excludeLinkText: String = messages("PayrollingSummary.table.exclude.link.text")
        doc.body().text() must include(excludeLinkText)
      }

      "show links to manage employees when they had been excluded before" in {
        val doc                    = Jsoup.parse(view(List(carBik), List(accommodation)).toString)
        val manageLinkText: String = messages("PayrollingSummary.table.manage.link.text")
        doc.body().text() must include(manageLinkText)
      }

      "not display benefits table on CY1" in {
        val doc = Jsoup.parse(view(List(medicalBik)).toString)
        doc.select("#cy1-benefits-table-header").first() mustBe None.orNull
      }
    }

    s"overview for no benefits on current tax year but on next tax year - $userType" must {

      implicit val html: Html = view(List.empty[BenefitInKindWithCount], List(carBik, accommodation))
      val doc                 = Jsoup.parse(view(List.empty[BenefitInKindWithCount], List(accommodation)).toString)

      behave like pageWithLink(
        messages(s"PayrollingSummary.choose.benefit.link.text." + userType),
        "/payrollbik/cy1/choose-benefit-expense"
      )

      "not show benefits tab for CY" in {
        doc.select("#tab_tab-1").first() mustBe None.orNull
      }

      "display benefits for CY1" in {
        doc.select("#cy1-benefits-table-header").first().text() must include(
          messages("PayrollingSummary.table.header")
        )
      }
    }

    s"overview for no benefits on current or next tax year - $userType" must {

      implicit val html: Html = view(List.empty[BenefitInKindWithCount])

      behave like pageWithLink(
        messages(s"PayrollingSummary.choose.benefit.link.text." + userType),
        "/payrollbik/cy1/choose-benefit-expense"
      )

      "not show benefits tab for CY" in {
        doc.select("#tab_tab-1").first() mustBe None.orNull
      }

      "not display benefits table on CY" in {
        val doc = Jsoup.parse(view(List.empty[BenefitInKindWithCount]).toString)
        doc.select("#cy-benefits-table-header").first() mustBe None.orNull
      }

      "not display benefits table on CY1" in {
        val doc = Jsoup.parse(view(List.empty[BenefitInKindWithCount]).toString)
        doc.select("#cy1-benefits-table-header").first() mustBe None.orNull
      }
    }

    s"overview for benefits for current and next tax year - $userType" must {

      implicit val html: Html = view(List(carBik, medicalBik), List(carBik, accommodation))
      val doc                 = Jsoup.parse(view(List(carBik, medicalBik), List(carBik, accommodation)).toString)

      behave like pageWithLink(
        messages(s"PayrollingSummary.choose.benefit.link.text." + userType),
        "/payrollbik/cy1/choose-benefit-expense"
      )

      "display benefits for CY" in {
        doc.select("#cy-benefits-table-header").first().text() must include(
          messages("PayrollingSummary.table.header")
        )
      }

      "display benefits for CY1" in {
        doc.select("#cy1-benefits-table-header").first().text() must include(
          messages("PayrollingSummary.table.header")
        )
      }
    }
  }

  // Run tests for both user types

  testView("organisation")(organisationRequest)
  testView("agent")(agentRequest)
}
