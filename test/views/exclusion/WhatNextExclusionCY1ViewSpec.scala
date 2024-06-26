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

import config.PbikAppConfig
import models.{AuthenticatedRequest, EiLPerson}
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.exclusion.WhatNextExclusion

class WhatNextExclusionCY1ViewSpec extends PBIKViewSpec {

  val formMappings: FormMappings               = app.injector.instanceOf[FormMappings]
  val whatNextExclusionView: WhatNextExclusion = app.injector.instanceOf[WhatNextExclusion]

  implicit val appConfig: PbikAppConfig = app.injector.instanceOf[PbikAppConfig]

  private val person = EiLPerson(
    nino = "AB123456C",
    firstForename = "John",
    secondForename = Some("Smith"),
    surname = "Smith",
    worksPayrollNumber = Some("123/AB123456C"),
    dateOfBirth = None,
    gender = None,
    status = None,
    perOptLock = 1
  )

  implicit def view()(implicit request: AuthenticatedRequest[_]): Html =
    whatNextExclusionView(taxYearRange, "cy", "vans", person)

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
      "/payrollbik/cy/vans/excluded-employees"
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
      "/payrollbik/cy/vans/excluded-employees"
    )
  }

}
