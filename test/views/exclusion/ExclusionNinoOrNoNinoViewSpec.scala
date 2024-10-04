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
import models.form.MandatoryRadioButton
import models.v1.IabdType
import org.jsoup.Jsoup
import play.api.data.Form
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.exclusion.ExclusionNinoOrNoNinoForm

class ExclusionNinoOrNoNinoViewSpec extends PBIKViewSpec {

  private val formMappings: FormMappings                               = app.injector.instanceOf[FormMappings]
  private val form: Form[MandatoryRadioButton]                         = formMappings.binaryRadioButton
  private val formWithErrors: Form[MandatoryRadioButton]               = form.withError("test", "error")
  private val exclusionNinoOrNoNinoFormView: ExclusionNinoOrNoNinoForm =
    app.injector.instanceOf[ExclusionNinoOrNoNinoForm]

  private val iabdType = IabdType.MedicalInsurance

  def viewWithForm(form: Form[MandatoryRadioButton])(implicit request: AuthenticatedRequest[_]): Html =
    exclusionNinoOrNoNinoFormView(taxYearRange, "cyp1", iabdType, "", form)

  "exclusionNinoOrNoNinoPage - organisation" must {
    implicit val html: Html = viewWithForm(form)(organisationRequest)

    behave like pageWithTitle(messages("ExclusionNinoDecision.title"))
    behave like pageWithHeader(messages("ExclusionNinoDecision.title"))
    behave like pageWithContinueButtonForm(
      s"/payrollbik/cyp1/${iabdType.id}/employee-national-insurance-number",
      "Continue"
    )
    behave like pageWithYesNoRadioButton("button-nino", "button-no-nino")

    "check the add benefit page for the errors" in {
      val viewWithFormWithErrors = viewWithForm(formWithErrors)(organisationRequest)
      val doc                    = Jsoup.parse(viewWithFormWithErrors.toString())

      doc must haveErrorSummary(messages("ExclusionDecision.noselection.error"))
      doc must haveErrorNotification(messages("ExclusionDecision.noselection.error"))
    }
  }

  "exclusionNinoOrNoNinoPage - agent" must {
    implicit val html: Html = viewWithForm(form)(agentRequest)

    behave like pageWithTitle(messages("ExclusionNinoDecision.title"))
    behave like pageWithHeader(messages("ExclusionNinoDecision.title"))
    behave like pageWithContinueButtonForm(
      s"/payrollbik/cyp1/${iabdType.id}/employee-national-insurance-number",
      "Continue"
    )
    behave like pageWithYesNoRadioButton("button-nino", "button-no-nino")

    "check the add benefit page for the errors" in {
      val viewWithFormWithErrors = viewWithForm(formWithErrors)(agentRequest)
      val doc                    = Jsoup.parse(viewWithFormWithErrors.toString())

      doc must haveErrorSummary(messages("ExclusionDecision.noselection.error"))
      doc must haveErrorNotification(messages("ExclusionDecision.noselection.error"))
    }
  }

}
