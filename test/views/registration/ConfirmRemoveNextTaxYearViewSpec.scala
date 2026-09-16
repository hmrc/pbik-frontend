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

package views.registration

import models.auth.AuthenticatedRequest
import models.v1.IabdType
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.registration.ConfirmRemoveNextTaxYear

class ConfirmRemoveNextTaxYearViewSpec extends PBIKViewSpec {

  private val confirmUpdateNextTaxYearView: ConfirmRemoveNextTaxYear = injected[ConfirmRemoveNextTaxYear]
  private val benefit                                                = IabdType.CarFuelBenefit
  private def view(implicit request: AuthenticatedRequest[?]): Html  =
    confirmUpdateNextTaxYearView(benefit, taxYearRange)

  "confirmRemoveNextTaxYear - organisation" must {
    implicit def html: Html = view(organisationRequest)

    behave like pageWithTitle(messages("RemoveBenefitsMPBIK2.reason.Title." + organisationRequest.userType))
    behave like pageWithHeader(messages("RemoveBenefitsMPBIK2.reason.Title." + organisationRequest.userType))
    behave like pageWithIdAndText(messages("RemoveBenefitsMPBIK2.selected.benefit"), "table-key")
    behave like pageWithIdAndText(messages("BenefitInKindMPBIK2.label." + benefit.id), "table-value")
    behave like pageWithIdAndText(
      messages(
        "RemoveBenefitsMPBIK2.confirm.p1." + organisationRequest.userType,
        taxYearRange.cy.toString
      ),
      "benefit-info"
    )
    behave like pageWithIdAndText(
      messages("RemoveBenefitsMPBIK2.confirm.declaration." + organisationRequest.userType),
      "user-info"
    )
    behave like pageWithConfirmAndContinueButtonAndLinkAndText(
      "button-confirm",
      s"/payrollbik/cy1/${benefit.id}/confirm-remove-benefit-expense",
      "Confirm and continue"
    )
  }

  "confirmRemoveNextTaxYear - agent" must {
    implicit def html: Html = view(agentRequest)

    behave like pageWithTitle(messages("RemoveBenefitsMPBIK2.reason.Title." + agentRequest.userType))
    behave like pageWithHeader(messages("RemoveBenefitsMPBIK2.reason.Title." + agentRequest.userType))
    behave like pageWithIdAndText(messages("RemoveBenefitsMPBIK2.selected.benefit"), "table-key")
    behave like pageWithIdAndText(messages("BenefitInKindMPBIK2.label." + benefit.id), "table-value")
    behave like pageWithIdAndText(
      messages(
        "RemoveBenefitsMPBIK2.confirm.p1." + agentRequest.userType,
        taxYearRange.cy.toString
      ),
      "benefit-info"
    )
    behave like pageWithIdAndText(
      messages("RemoveBenefitsMPBIK2.confirm.declaration." + agentRequest.userType),
      "user-info"
    )
    behave like pageWithConfirmAndContinueButtonAndLinkAndText(
      "button-confirm",
      s"/payrollbik/cy1/${benefit.id}/confirm-remove-benefit-expense",
      "Confirm and continue"
    )
  }
}
