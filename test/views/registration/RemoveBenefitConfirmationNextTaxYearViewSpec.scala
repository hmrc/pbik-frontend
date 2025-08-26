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
import models.{RegistrationItem, RegistrationList, TaxYearRange}
import play.twirl.api.Html
import org.jsoup.Jsoup
import utils.FormMappings
import models.v1.IabdType.IabdType
import views.helper.PBIKViewSpec
import views.html.registration.RemoveBenefitConfirmationNextTaxYear
import config.PbikAppConfig

class RemoveBenefitConfirmationNextTaxYearViewSpec extends PBIKViewSpec {

  val removeBenefitConfirmationNextTaxYearView: RemoveBenefitConfirmationNextTaxYear =
    injected[RemoveBenefitConfirmationNextTaxYear]

  val bikList: RegistrationList = RegistrationList(
    active = List(
      RegistrationItem(IabdType.MedicalInsurance, active = true, enabled = true)
    )
  )

  private def view(implicit request: AuthenticatedRequest[?]): Html =
    removeBenefitConfirmationNextTaxYearView(
      isCurrentYear = true,
      taxYearRange,
      bikList,
      IabdType.MedicalInsurance
    )

  "RemoveBenefitConfirmationNextTaxYear view" must {

    "display agent-specific info when request.isAgent is true" in {
      val doc = Jsoup.parse(view(agentRequest).toString)

      doc.select(".govuk-summary-list__row").size() must be > 0
      doc.text() must include(messages("Service.field.client.name"))
      doc.text() must include(agentRequest.clientName.get)
    }

    "NOT display agent-specific info when request.isAgent is false" in {
      val doc = Jsoup.parse(view(organisationRequest).toString)

      doc.text() must not include messages("Service.field.client.name")
    }

    "display the panel title and benefit info correctly" in {
      val doc = Jsoup.parse(view(organisationRequest).toString)

      doc.select(".govuk-panel.govuk-panel--confirmation").size() must be > 0
      doc.text() must include(messages("whatNext.remove.heading"))
      doc.text() must include(messages("BenefitInKind.label." + IabdType.MedicalInsurance.id))
    }

  }
}
