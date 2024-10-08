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

package views.exclusion

import models.auth.AuthenticatedRequest
import models.v1.IabdType
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.exclusion.WhatNextRescind

class WhatNextRescindViewSpec extends PBIKViewSpec {

  private val whatNextRescindView: WhatNextRescind = injected[WhatNextRescind]
  private val benefit: IabdType.Value              = IabdType.CarFuelBenefit

  private def view(implicit request: AuthenticatedRequest[_]): Html = whatNextRescindView(
    taxYearRange,
    "cyp1",
    benefit,
    exclusionPerson
  )

  "whatNextRescind - organisation" must {
    implicit def html: Html = view(organisationRequest)

    behave like pageWithTitle(messages("whatNext.rescind.heading"))
    behave like pageWithHeader(messages("whatNext.rescind.heading"))
    behave like pageWithLink(
      messages("whatNext.exclude.you.do.p.link." + organisationRequest.userType),
      "/payrollbik/cy1/registered-benefits-expenses"
    )
    behave like pageWithLink(
      messages("whatNext.exclude.more.p.link", "Car fuel"),
      s"/payrollbik/cyp1/${benefit.id}/excluded-employees"
    )
  }

  "whatNextRescind - agent" must {
    implicit def html: Html = view(agentRequest)

    behave like pageWithTitle(messages("whatNext.rescind.heading"))
    behave like pageWithHeader(messages("whatNext.rescind.heading"))
    behave like pageWithLink(
      messages("whatNext.exclude.you.do.p.link." + agentRequest.userType),
      "/payrollbik/cy1/registered-benefits-expenses"
    )
    behave like pageWithLink(
      messages("whatNext.exclude.more.p.link", "Car fuel"),
      s"/payrollbik/cyp1/${benefit.id}/excluded-employees"
    )
  }
}
