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

package views.exclusion

import config.PbikAppConfig
import models.auth.AuthenticatedRequest
import models.v1.IabdType
import play.twirl.api.Html
import utils.{FormMappings, TaxDateUtils}
import views.helper.PBIKViewSpec
import views.html.exclusion.WhatNextExclusion

class WhatNextExclusionCY1ViewSpec extends PBIKViewSpec {

  val formMappings: FormMappings               = injected[FormMappings]
  val whatNextExclusionView: WhatNextExclusion = injected[WhatNextExclusion]

  implicit val appConfig: PbikAppConfig = injected[PbikAppConfig]

  private val iabdType     = IabdType.VanBenefit
  private val taxDateUtils = new TaxDateUtils

  implicit def view()(implicit request: AuthenticatedRequest[?]): Html =
    whatNextExclusionView(taxYearRange, "cy", iabdType, tracePerson)

  "whatNextAddRemove - organisation" must {
    implicit val html: Html = view()(organisationRequest)

    behave like pageWithTitle(messages("whatNext.exclude.heading"))
    behave like pageWithHeader(messages("whatNext.exclude.heading"))
    behave like pageWithLink(
      messages("whatNext.exclude.you.do.p.cy.link." + organisationRequest.userType),
      "/payrollbik/cy/registered-benefits-expenses"
    )
    behave like pageWithLink(
      messages("whatNext.exclude.more.p.cy.link", "Vans"),
      s"/payrollbik/cy/${iabdType.id}/excluded-employees"
    )
    behave like pageWithIdAndText(
      "John A Doe will not have Vans taxed through payroll from " + taxDateUtils.getDisplayTodayDate(),
      "confirmation-p"
    )
  }

  "whatNextAddRemove - agent" must {
    implicit val html: Html = view()(agentRequest)

    behave like pageWithTitle(messages("whatNext.exclude.heading"))
    behave like pageWithHeader(messages("whatNext.exclude.heading"))
    behave like pageWithLink(
      messages("whatNext.exclude.you.do.p.cy.link." + agentRequest.userType),
      "/payrollbik/cy/registered-benefits-expenses"
    )
    behave like pageWithLink(
      messages("whatNext.exclude.more.p.cy.link", "Vans"),
      s"/payrollbik/cy/${iabdType.id}/excluded-employees"
    )
    behave like pageWithIdAndText(
      "John A Doe will not have Vans taxed through payroll from " + taxDateUtils.getDisplayTodayDate(),
      "confirmation-p"
    )

  }

}
