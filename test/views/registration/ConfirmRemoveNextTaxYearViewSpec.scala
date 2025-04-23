/*
 * Copyright 2025 HM Revenue & Customs
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

import models.auth.AuthenticatedRequest
import models.v1.IabdType
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.registration.ConfirmRemoveNextTaxYear

class ConfirmRemoveNextTaxYearViewSpec extends PBIKViewSpec {

  private val confirmUpdateNextTaxYearView: ConfirmRemoveNextTaxYear = injected[ConfirmRemoveNextTaxYear]
  private val benefit                                                = IabdType.CarFuelBenefit
  private def view(implicit request: AuthenticatedRequest[_]): Html  =
    confirmUpdateNextTaxYearView(benefit, taxYearRange)

  "confirmRemoveNextTaxYear - organisation" must {
    implicit def html: Html = view(organisationRequest)

    behave like pageWithTitle(messages("RemoveBenefits.confirm.title"))
    behave like pageWithHeader(messages("RemoveBenefits.confirm.heading"))
    behave like pageWithIdAndText(messages("Service.field.stop.benefit"), "table-key")
    behave like pageWithIdAndText(messages("BenefitInKind.label." + benefit.id), "table-value")
    behave like pageWithIdAndText(
      messages(
        "RemoveBenefits.confirm.p1." + organisationRequest.userType,
        messages("BenefitInKind.label." + benefit.id),
        taxYearRange.cy.toString
      ),
      "benefit-info"
    )
    behave like pageWithIdAndText(messages("RemoveBenefits.confirm.p2." + organisationRequest.userType), "user-info")
    behave like pageWithConfirmAndContinueButtonAndLinkAndText(
      "button-confirm",
      s"/payrollbik/cy1/${benefit.id}/confirm-remove-benefit-expense",
      "Confirm and continue"
    )
  }

  "confirmRemoveNextTaxYear - agent" must {
    implicit def html: Html = view(agentRequest)

    behave like pageWithTitle(messages("RemoveBenefits.confirm.title"))
    behave like pageWithHeader(messages("RemoveBenefits.confirm.heading"))
    behave like pageWithIdAndText(messages("Service.field.stop.benefit"), "table-key")
    behave like pageWithIdAndText(messages("BenefitInKind.label." + benefit.id), "table-value")
    behave like pageWithIdAndText(
      messages(
        "RemoveBenefits.confirm.p1." + agentRequest.userType,
        messages("BenefitInKind.label." + benefit.id),
        taxYearRange.cy.toString
      ),
      "benefit-info"
    )
    behave like pageWithIdAndText(messages("RemoveBenefits.confirm.p2." + agentRequest.userType), "user-info")
    behave like pageWithConfirmAndContinueButtonAndLinkAndText(
      "button-confirm",
      s"/payrollbik/cy1/${benefit.id}/confirm-remove-benefit-expense",
      "Confirm and continue"
    )
  }
}
