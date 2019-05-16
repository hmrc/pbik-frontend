/*
 * Copyright 2019 HM Revenue & Customs
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

///*
// * Copyright 2019 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */

package views

import models.{EmpRef, RegistrationList, TaxYearRange}
import org.jsoup.Jsoup
import play.api.data.Form
import play.api.mvc.Flash
import play.twirl.api.Html
import utils.BikListUtils.MandatoryRadioButton
import utils.FormMappings
import views.helper.PBIKViewSpec


class ExclusionNinoOrNoNinoViewSpec extends PBIKViewSpec with FormMappings {

  def taxYearRange = TaxYearRange(2018, 2019, 2020)
  val messageKeyPrefix = "exclusionNinoOrNoNinoForm"

  override def view: Html = viewWithForm(new Flash)

  def viewWithForm(flash: Flash): Html =
    views.html.exclusion.exclusionNinoOrNoNinoForm(taxYearRange, "cyp1", "30", "",EmpRef("", ""))(implicitly, flash, implicitly, implicitly)



  "exclusionNinoOrNoNinoForm" must {
    behave like pageWithTitle(messages("ExclusionNinoDecision.title"))
    behave like pageWithHeader(messages("ExclusionNinoDecision.title"))
    behave like pageWithContinueButtonForm("/payrollbik/cyp1/medical/exclude-employee-search", "Continue")
    behave like pageWithYesNoRadioButton("button-nino", "button-no-nino")

    "check the add benefit page for the errors" in {

      val view = viewWithForm(new Flash(Map("error" -> messages("ExclusionDecision.noselection.error"))))
      val doc  = Jsoup.parse(view.toString())

      doc must haveErrorSummary(messages("ExclusionDecision.noselection.error"))
      doc must haveErrorNotification(messages("ExclusionDecision.noselection.error"))
    }

  }

}
