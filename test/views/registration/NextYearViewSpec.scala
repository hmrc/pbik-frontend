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

import models.{AuthenticatedRequest, RegistrationList}
import org.jsoup.Jsoup
import play.api.data.Form
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.registration.NextTaxYear

class NextYearViewSpec extends PBIKViewSpec {

  val formMappings: FormMappings   = app.injector.instanceOf[FormMappings]
  val nextTaxYearView: NextTaxYear = app.injector.instanceOf[NextTaxYear]

  def viewWithForm(form: Form[RegistrationList])(implicit request: AuthenticatedRequest[_]): Html =
    nextTaxYearView(form, additive = true, taxYearRange, List(), List(), List(), List(), 1)

  "nextYearPage - organisation" must {
    implicit def html: Html = viewWithForm(formMappings.objSelectedForm)(organisationRequest)

    behave like pageWithTitle(messages("AddBenefits.Heading"))
    behave like pageWithHeader(messages("AddBenefits.Heading"))
    behave like pageWithContinueButtonForm("/payrollbik/cy1/choose-benefit-expense", "Continue")

    "check the add benefit page for the errors" in {
      val view = viewWithForm(formMappings.objSelectedForm.bind(Map[String, String]()))(organisationRequest)
      val doc  = Jsoup.parse(view.toString())

      doc must haveErrorSummary(messages("AddBenefits.noselection.error"))
      doc must haveErrorNotification(messages("AddBenefits.noselection.error"))
    }

  }

  "nextYearPage - agent" must {
    implicit def html: Html = viewWithForm(formMappings.objSelectedForm)(agentRequest)

    behave like pageWithTitle(messages("AddBenefits.Heading"))
    behave like pageWithHeader(messages("AddBenefits.Heading"))
    behave like pageWithContinueButtonForm("/payrollbik/cy1/choose-benefit-expense", "Continue")

    "check the add benefit page for the errors" in {
      val view = viewWithForm(formMappings.objSelectedForm.bind(Map[String, String]()))(agentRequest)
      val doc  = Jsoup.parse(view.toString())

      doc must haveErrorSummary(messages("AddBenefits.noselection.error"))
      doc must haveErrorNotification(messages("AddBenefits.noselection.error"))
    }

  }

}
