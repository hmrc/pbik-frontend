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
import views.html.Summary

class SummaryViewSpec extends PBIKViewSpec {

  private val summaryView: Summary               = injected[Summary]
  private val carIabdType: String                = IabdType.CarBenefit.id.toString
  private val carBik: BenefitInKindWithCount     = BenefitInKindWithCount(IabdType.CarBenefit, 2)
  private val medicalBik: BenefitInKindWithCount = BenefitInKindWithCount(IabdType.MedicalInsurance, 2)
  private val serviceBiksCountCY: Int            = 0
  private val serviceBiksCountCYP1: Int          = 200

  private def view(
    selectedYear: String,
    benefitsCY: List[BenefitInKindWithCount],
    benefitsCYP1: List[BenefitInKindWithCount],
    showChangeYearLink: Boolean = true
  )(implicit request: AuthenticatedRequest[?]): Html =
    summaryView(
      selectedYear,
      taxYearRange,
      benefitsCY,
      benefitsCYP1,
      serviceBiksCountCY,
      serviceBiksCountCYP1,
      showChangeYearLink
    )

  private def testCYView(userType: String)(implicit request: AuthenticatedRequest[?]): Unit =
    s"overview for CY - $userType" must {
      implicit val html: Html = view("cy", List(carBik, medicalBik), List.empty)

      behave like pageWithTitle(messages("Overview.benefitsManage.heading"))
      behave like pageWithHeader(messages("Overview.benefitsManage.heading"))
      behave like pageWithBackLink()

      "not display empty benefits message when CY benefits are present" in {
        val doc          = Jsoup.parse(view("cy", List(carBik, medicalBik), List.empty).toString())
        val emptyMessage = messages("Overview.empty.benefits.p")
        doc.body().text() must not include emptyMessage
      }

      "show empty message if CY benefits are empty" in {
        val doc = Jsoup.parse(view("cy", List.empty, List.empty).toString())
        doc.body().text() must include(messages("Overview.empty.benefits.p"))
      }

      "show correct heading h2 for the page for CY" in {
        val doc = Jsoup.parse(view("cy", List.empty, List.empty).toString())
        doc.select("#benefits-heading-2").text() mustBe messages(
          "Overview.empty.benefits.h2",
          taxYearRange.cyminus1.toString
        )
      }

      "not show no benefits message when CY list is empty and CYP1 list is full" in {
        val lotsOfBiks   = List.fill(200)(carBik)
        val doc          = Jsoup.parse(view("cy", List.empty, lotsOfBiks).toString)
        val emptyMessage = messages("Overview.empty.benefits.p")
        doc.body().text() must not include emptyMessage
      }

    }

  private def testCYP1View(userType: String)(implicit request: AuthenticatedRequest[?]): Unit =
    s"overview for CYP1 - $userType" must {
      implicit val html: Html = view("cy1", List.empty, List(carBik, medicalBik))

      behave like pageWithTitle(messages("Overview.benefitsRegistered.heading"))
      behave like pageWithHeader(messages("Overview.benefitsRegistered.heading"))
      behave like pageWithLink(messages("Overview.table.add.link"), "/payrollbik/cy1/choose-benefit-expense")
      behave like pageWithBackLink()
      behave like pageWithLinkHiddenText(
        s"cy1-remove-$carIabdType",
        s"${messages("BenefitInKind.label." + carIabdType)} ${messages("Overview.current.from")} ${messages("Overview.current.payroll.p11d")}"
      )
      behave like pageWithLink(messages("Overview.change.year.cy1.text.link"), "/payrollbik/select-year")

      "not display empty benefits message when CYP1 benefits are present" in {
        val doc          = Jsoup.parse(view("cy1", List.empty, List(carBik)).toString())
        val emptyMessage = messages("Overview.empty.benefits.p")
        doc.body().text() must not include emptyMessage
      }

      "show empty message if CYP1 benefits are empty" in {
        val doc = Jsoup.parse(view("cy1", List.empty, List.empty).toString)
        doc.body().text() must include(messages("Overview.empty.benefits.p"))
      }

      "not show change year link if disabled" in {
        val doc = Jsoup.parse(view("cy1", List.empty, List.empty, showChangeYearLink = false).toString())
        doc.select("#tax-year-select").first() mustBe None.orNull
      }

      "show correct heading h2 for the page for CY+1" in {
        val doc = Jsoup.parse(view("cy1", List.empty, List.empty).toString())
        doc.select("#benefits-heading-2").text() mustBe messages("Overview.empty.benefits.h2", taxYearRange.cy.toString)
      }
    }

  // Run tests for both user types
  testCYView("organisation")(organisationRequest)
  testCYP1View("organisation")(organisationRequest)

  testCYView("agent")(agentRequest)
  testCYP1View("agent")(agentRequest)
}
