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

import scala.collection.immutable.List

class SummaryViewSpec extends PBIKViewSpec {

  private lazy val payrollingSummaryView: PayrollingSummary =
    injected[PayrollingSummary]
  private val accommodationBik: BenefitInKindWithCount      = BenefitInKindWithCount(IabdType.Accommodation, 2)
  private val loanBik: BenefitInKindWithCount               = BenefitInKindWithCount(IabdType.BeneficialLoan, 2)

  private def view(
    benefitsCY: List[BenefitInKindWithCount]
  )(implicit request: AuthenticatedRequest[?]): Html =
    payrollingSummaryView(
      taxYearRange.cy,
      benefitsCY,
      benefitsCY,
      false
    )

  private def testCYView(userType: String)(implicit request: AuthenticatedRequest[?]): Unit =
    s"overview for CY - $userType" must {
      implicit val html: Html = view(List(accommodationBik, loanBik))

      behave like pageWithTitle(messages(s"PayrollingSummary.heading"))
      behave like pageWithHeader(messages(s"PayrollingSummary.heading"))
      behave like pageWithIdAndText(
        messages(s"PayrollingSummary.p1.$userType", request.clientName.getOrElse("")),
        "service-use"
      )
      behave like pageWithBackLink()

      "show correct title for the tab for CY" in {
        val doc = Jsoup.parse(view(List(accommodationBik, loanBik)).toString())
        doc.select("#benefits-tab").text() mustBe messages(
          "PayrollingSummary.tab.heading"
        )
      }

      "show correct tab tag for CY" in {
        val doc = Jsoup.parse(view(List(accommodationBik, loanBik)).toString())

        println("ALE")
        println(doc)
        println("ALE")

        doc.select("#tab_tab-1").text() mustBe messages(
          "PayrollingSummary.tab.taxYear",
          taxYearRange.cyminus1.toString,
          taxYearRange.cy.toString
        )
      }

    }

  // Run tests for both user types
  testCYView("organisation")(organisationRequest)
  testCYView("agent")(agentRequest)
}
