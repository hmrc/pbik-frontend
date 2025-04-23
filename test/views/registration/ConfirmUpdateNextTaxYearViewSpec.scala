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

import config.PbikAppConfig
import models.auth.AuthenticatedRequest
import models.v1.IabdType
import models.{RegistrationItem, RegistrationList}
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.registration.ConfirmUpdateNextTaxYear

class ConfirmUpdateNextTaxYearViewSpec extends PBIKViewSpec {

  val formMappings: FormMappings                             = injected[FormMappings]
  val confirmUpdateNextTaxYearView: ConfirmUpdateNextTaxYear = injected[ConfirmUpdateNextTaxYear]

  def view(implicit request: AuthenticatedRequest[_]): Html = confirmUpdateNextTaxYearView(
    bikList,
    taxYearRange
  )

  implicit val appConfig: PbikAppConfig    = injected[PbikAppConfig]
  val bikList: RegistrationList            = RegistrationList(active = List.empty[RegistrationItem])
  val removalBik: Option[RegistrationItem] = Some(
    RegistrationItem(IabdType.MedicalInsurance, active = true, enabled = true)
  )

  "nextYearPage - organisation" must {
    implicit def html: Html = view(organisationRequest)

    behave like pageWithTitle(messages("AddBenefits.Confirm.Multiple.Title"))
    behave like pageWithHeader(messages("AddBenefits.Confirm.Multiple.Heading"))
    behave like pageWithContinueButtonForm("/payrollbik/cy1/check-the-benefits", "Confirm and continue")
  }

  "nextYearPage - agent" must {
    implicit def html: Html = view(agentRequest)

    behave like pageWithTitle(messages("AddBenefits.Confirm.Multiple.Title"))
    behave like pageWithHeader(messages("AddBenefits.Confirm.Multiple.Heading"))
    behave like pageWithContinueButtonForm("/payrollbik/cy1/check-the-benefits", "Confirm and continue")
  }
}
