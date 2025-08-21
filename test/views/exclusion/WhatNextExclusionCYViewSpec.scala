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

package views.exclusion

import config.PbikAppConfig
import models.auth.AuthenticatedRequest
import models.v1.IabdType
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.exclusion.WhatNextExclusion

class WhatNextExclusionCYViewSpec extends PBIKViewSpec {

  val formMappings: FormMappings               = injected[FormMappings]
  val whatNextExclusionView: WhatNextExclusion = injected[WhatNextExclusion]

  implicit val appConfig: PbikAppConfig = injected[PbikAppConfig]

  private val iabdType = IabdType.MedicalInsurance

  implicit def view()(implicit request: AuthenticatedRequest[?]): Html =
    whatNextExclusionView(taxYearRange, "cyp1", iabdType, tracePerson)

  "whatNextAddRemove - organisation" must {
    implicit val html: Html = view()(organisationRequest)

    behave like pageWithTitle(messages("whatNext.exclude.heading"))
    behave like pageWithHeader(messages("whatNext.exclude.heading"))
    behave like pageWithLink(
      messages("whatNext.exclude.you.do.p.link." + organisationRequest.userType),
      "/payrollbik/cy1/registered-benefits-expenses"
    )
    behave like pageWithLink(
      messages("whatNext.exclude.more.p.link", "Private medical treatment or insurance"),
      s"/payrollbik/cyp1/${iabdType.id}/excluded-employees"
    )
    behave like pageWithIdAndText(
      "John A Doe will not have Private medical treatment or insurance taxed through payroll from 06 April 2026.",
      "confirmation-p"
    )

  }

  "whatNextAddRemove - agent" must {
    implicit val html: Html = view()(agentRequest)

    behave like pageWithTitle(messages("whatNext.exclude.heading"))
    behave like pageWithHeader(messages("whatNext.exclude.heading"))
    behave like pageWithLink(
      messages("whatNext.exclude.you.do.p.link." + agentRequest.userType),
      "/payrollbik/cy1/registered-benefits-expenses"
    )
    behave like pageWithLink(
      messages("whatNext.exclude.more.p.link", "Private medical treatment or insurance"),
      s"/payrollbik/cyp1/${iabdType.id}/excluded-employees"
    )
    behave like pageWithIdAndText(
      "John A Doe will not have Private medical treatment or insurance taxed through payroll from 06 April 2026.",
      "confirmation-p"
    )

  }

}
