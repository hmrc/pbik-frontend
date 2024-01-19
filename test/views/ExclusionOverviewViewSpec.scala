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

package views

import models.EmpRef
import org.jsoup.Jsoup
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.exclusion.ExclusionOverview

class ExclusionOverviewViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi                 = app.injector.instanceOf[MessagesApi]
  val formMappings: FormMappings               = app.injector.instanceOf[FormMappings]
  val exclusionOverviewView: ExclusionOverview = app.injector.instanceOf[ExclusionOverview]

  private val (iabdType, iabdString) = ("31", "car")

  implicit def view: Html =
    exclusionOverviewView(taxYearRange, "cyp1", iabdString, List(), EmpRef("", ""), formMappings.binaryRadioButton)

  def viewWithFormWithErrors(): Html =
    exclusionOverviewView(
      taxYearRange,
      "cyp1",
      iabdString,
      List(),
      EmpRef("", ""),
      formMappings.binaryRadioButton.withError("test", "error")
    )

  "exclusionOverview" must {
    behave like pageWithTitle(messages("ExclusionOverview.notExcludedEmployee.title"))
    behave like pageWithHeader(
      messages(s"BenefitInKind.label.$iabdType")
        + " " + messages("ExclusionOverview.notExcludedEmployee.title")
    )
    behave like pageWithContinueButtonForm(s"/payrollbik/cyp1/$iabdString/excluded-employees", "Continue")
    behave like pageWithYesNoRadioButton("confirmation-yes", "confirmation-yes")

    "check the excluded employees page for the errors" in {

      val doc = Jsoup.parse(viewWithFormWithErrors().toString())
      doc must haveErrorSummary(messages("ExclusionOverview.error.required"))
      doc must haveErrorNotification(messages("ExclusionOverview.error.required"))
    }

  }
}
