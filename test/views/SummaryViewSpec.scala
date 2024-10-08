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

  def view(
    benefitsCY: List[BenefitInKindWithCount],
    benefitsCY1: List[BenefitInKindWithCount],
    showChangeYearLink: Boolean = true
  )(implicit
    request: AuthenticatedRequest[_]
  ): Html =
    summaryView(
      cyAllowed = true,
      taxYearRange,
      benefitsCY,
      benefitsCY1,
      serviceBiksCountCY,
      serviceBiksCountCYP1,
      showChangeYearLink
    )

  "overview with benefits - organisation" must {
    implicit val html: Html = view(List.empty, List(carBik, medicalBik))(organisationRequest)

    behave like pageWithTitle(messages("Overview.benefitsRegistered.heading"))
    behave like pageWithHeader(messages("Overview.benefitsRegistered.heading"))
    behave like pageWithLink(messages("Overview.table.add.link"), "/payrollbik/cy1/choose-benefit-expense")
    behave like pageWithBackLink()
    behave like pageWithLinkHiddenText(
      s"cy1-remove-$carIabdType",
      s"${messages("BenefitInKind.label." + carIabdType)} ${messages("Overview.current.from")} ${messages("Overview.current.payroll.p11d")}"
    )
    behave like pageWithLink(messages("Overview.change.year.cy1.text.link"), "/payrollbik/select-year")

    "overview with no benefits" in {
      val doc = Jsoup.parse(view(List.empty, List.empty)(organisationRequest).toString())
      doc.title must include(messages("Overview.benefitsRegistered.heading"))
      doc       must haveHeadingWithText(messages("Overview.benefitsRegistered.heading"))
    }

    "overview without change year link" in {
      val doc = Jsoup.parse(view(List.empty, List.empty, showChangeYearLink = false)(organisationRequest).toString())
      doc.title must include(messages("Overview.benefitsRegistered.heading"))
      doc       must haveHeadingWithText(messages("Overview.benefitsRegistered.heading"))
      doc.select(s"#tax-year-select").first() mustBe None.orNull
    }
  }

  "overview with benefits - agent" must {
    implicit val html: Html = view(List.empty, List(carBik))(agentRequest)

    behave like pageWithTitle(messages("Overview.benefitsRegistered.heading"))
    behave like pageWithHeader(messages("Overview.benefitsRegistered.heading"))
    behave like pageWithLink(messages("Overview.table.add.link"), "/payrollbik/cy1/choose-benefit-expense")
    behave like pageWithBackLink()
    behave like pageWithLinkHiddenText(
      s"cy1-remove-$carIabdType",
      s"${messages("BenefitInKind.label." + carIabdType)} ${messages("Overview.current.from")} ${messages("Overview.current.payroll.p11d")}"
    )
    behave like pageWithLink(messages("Overview.change.year.cy1.text.link"), "/payrollbik/select-year")

    "overview with no benefits" in {
      val doc = Jsoup.parse(view(List.empty, List.empty)(agentRequest).toString())
      doc.title must include(messages("Overview.benefitsRegistered.heading"))
      doc       must haveHeadingWithText(messages("Overview.benefitsRegistered.heading"))
    }

    "overview without change year link" in {
      val doc = Jsoup.parse(view(List.empty, List.empty, showChangeYearLink = false)(organisationRequest).toString())
      doc.title must include(messages("Overview.benefitsRegistered.heading"))
      doc       must haveHeadingWithText(messages("Overview.benefitsRegistered.heading"))
      doc.select(s"#tax-year-select").first() mustBe None.orNull
    }
  }

}
