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

import models.{AuthenticatedRequest, Bik, MandatoryRadioButton}
import org.jsoup.Jsoup
import play.api.data.Form
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.exclusion.ExclusionOverview

class ExclusionOverviewViewSpec extends PBIKViewSpec {

  val formMappings: FormMappings                         = app.injector.instanceOf[FormMappings]
  val exclusionOverviewView: ExclusionOverview           = app.injector.instanceOf[ExclusionOverview]
  private val form: Form[MandatoryRadioButton]           = formMappings.binaryRadioButton
  private val formWithErrors: Form[MandatoryRadioButton] = form.withError("test", "error")

  private val iabdString = "car"

  def viewWithForm(form: Form[MandatoryRadioButton])(implicit request: AuthenticatedRequest[_]): Html =
    exclusionOverviewView(taxYearRange, "cyp1", iabdString, List(), form)

  "exclusionOverview - organisation" must {
    implicit val html: Html = viewWithForm(form)(organisationRequest)

    behave like pageWithTitle(messages(s"BenefitInKind.label.${Bik.asNPSTypeValue(iabdString)}"))
    behave like pageWithHeader(messages(s"BenefitInKind.label.${Bik.asNPSTypeValue(iabdString)}"))
    behave like pageWithContinueButtonForm(s"/payrollbik/cyp1/$iabdString/excluded-employees", "Continue")
    behave like pageWithYesNoRadioButton("confirmation-yes", "confirmation-yes")

    "check the excluded employees page for the errors" in {
      val viewWithFormWithErrors = viewWithForm(formWithErrors)(organisationRequest)
      val doc                    = Jsoup.parse(viewWithFormWithErrors.toString())

      doc must haveErrorSummary(messages("ExclusionOverview.error.required"))
      doc must haveErrorNotification(messages("ExclusionOverview.error.required"))
    }

  }

  "exclusionOverview - agent" must {
    implicit val html: Html = viewWithForm(form)(agentRequest)

    behave like pageWithTitle(messages(s"BenefitInKind.label.${Bik.asNPSTypeValue(iabdString)}"))
    behave like pageWithHeader(messages(s"BenefitInKind.label.${Bik.asNPSTypeValue(iabdString)}"))
    behave like pageWithContinueButtonForm(s"/payrollbik/cyp1/$iabdString/excluded-employees", "Continue")
    behave like pageWithYesNoRadioButton("confirmation-yes", "confirmation-yes")

    "check the excluded employees page for the errors" in {
      val viewWithFormWithErrors = viewWithForm(formWithErrors)(agentRequest)
      val doc                    = Jsoup.parse(viewWithFormWithErrors.toString())

      doc must haveErrorSummary(messages("ExclusionOverview.error.required"))
      doc must haveErrorNotification(messages("ExclusionOverview.error.required"))
    }

  }
}
