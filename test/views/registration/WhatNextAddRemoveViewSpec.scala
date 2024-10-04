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

import models.auth.AuthenticatedRequest
import models.v1.IabdType
import models.{RegistrationItem, RegistrationList}
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.registration.AddBenefitConfirmationNextTaxYear

class WhatNextAddRemoveViewSpec extends PBIKViewSpec {

  val formMappings: FormMappings                                               = app.injector.instanceOf[FormMappings]
  val addBenefitConfirmationNextTaxYearView: AddBenefitConfirmationNextTaxYear =
    app.injector.instanceOf[AddBenefitConfirmationNextTaxYear]

  val regList: RegistrationList =
    RegistrationList(active = List(RegistrationItem(IabdType.MedicalInsurance, active = true, enabled = true)))

  def view()(implicit request: AuthenticatedRequest[_]): Html =
    addBenefitConfirmationNextTaxYearView(isCurrentYear = true, taxYearRange, regList)

  "whatNextAddRemove - organisation" must {
    implicit val html: Html = view()(organisationRequest)

    behave like pageWithTitle(messages("whatNext.add.heading"))
    behave like pageWithHeader(messages("whatNext.add.heading"))
    behave like pageWithLink(
      messages("whatYouCanDoNext.subHeading.p.link." + organisationRequest.userType),
      "/payrollbik/cy/registered-benefits-expenses"
    )
  }

  "whatNextAddRemove - agent" must {
    implicit val html: Html = view()(agentRequest)

    behave like pageWithTitle(messages("whatNext.add.heading"))
    behave like pageWithHeader(messages("whatNext.add.heading"))
    behave like pageWithLink(
      messages("whatYouCanDoNext.subHeading.p.link." + agentRequest.userType),
      "/payrollbik/cy/registered-benefits-expenses"
    )
  }
}
