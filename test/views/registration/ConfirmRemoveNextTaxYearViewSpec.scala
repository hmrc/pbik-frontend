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

package views.registration

import models.v1.IabdType
import models.{AuthenticatedRequest, Bik}
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.registration.ConfirmRemoveNextTaxYear

class ConfirmRemoveNextTaxYearViewSpec extends PBIKViewSpec {

  private val confirmUpdateNextTaxYearView: ConfirmRemoveNextTaxYear = app.injector.instanceOf[ConfirmRemoveNextTaxYear]
  private val benefit                                                = IabdType.CarFuelBenefit
  private def view(implicit request: AuthenticatedRequest[_]): Html  = confirmUpdateNextTaxYearView(
    Bik.asBenefitString(benefit.id.toString),
    taxYearRange
  )

  "confirmRemoveNextTaxYear - organisation" must {
    implicit def html: Html = view(organisationRequest)

    behave like pageWithTitle(messages("RemoveBenefits.confirm.title"))
    behave like pageWithHeader(messages("RemoveBenefits.confirm.heading"))
    behave like pageWithIdAndText(messages("RemoveBenefits.confirm.table.key1"), "table-key")
    behave like pageWithIdAndText(messages("BenefitInKind.label." + benefit.id), "table-value")
    behave like pageWithIdAndText(
      messages(
        "RemoveBenefits.confirm.p1." + organisationRequest.userType,
        messages("BenefitInKind.label." + benefit.id),
        taxYearRange.cy
      ),
      "benefit-info"
    )
    behave like pageWithIdAndText(messages("RemoveBenefits.confirm.p2." + organisationRequest.userType), "user-info")
    behave like pageWithConfirmAndContinueButtonAndLinkAndText(
      "button-confirm",
      "/payrollbik/cy1/car-fuel/confirm-remove-benefit-expense",
      "Confirm and continue"
    )
  }

  "confirmRemoveNextTaxYear - agent" must {
    implicit def html: Html = view(agentRequest)

    behave like pageWithTitle(messages("RemoveBenefits.confirm.title"))
    behave like pageWithHeader(messages("RemoveBenefits.confirm.heading"))
    behave like pageWithIdAndText(messages("RemoveBenefits.confirm.table.key1"), "table-key")
    behave like pageWithIdAndText(messages("BenefitInKind.label." + benefit.id), "table-value")
    behave like pageWithIdAndText(
      messages(
        "RemoveBenefits.confirm.p1." + agentRequest.userType,
        messages("BenefitInKind.label." + benefit.id),
        taxYearRange.cy
      ),
      "benefit-info"
    )
    behave like pageWithIdAndText(messages("RemoveBenefits.confirm.p2." + agentRequest.userType), "user-info")
    behave like pageWithConfirmAndContinueButtonAndLinkAndText(
      "button-confirm",
      "/payrollbik/cy1/car-fuel/confirm-remove-benefit-expense",
      "Confirm and continue"
    )
  }
}
