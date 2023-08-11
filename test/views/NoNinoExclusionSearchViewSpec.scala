/*
 * Copyright 2023 HM Revenue & Customs
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

import models._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.FormMappings
import views.helper.{PBIKBaseViewSpec, PBIKNoViewBehaviours}
import views.html.exclusion.NoNinoExclusionSearchForm

class NoNinoExclusionSearchViewSpec extends PBIKBaseViewSpec with PBIKNoViewBehaviours {

  private val formMappings: FormMappings    = app.injector.instanceOf[FormMappings]
  private val noNinoExclusionSearchFormView = app.injector.instanceOf[NoNinoExclusionSearchForm]

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  def view: Html               = viewWithForm(formMappings.exclusionSearchFormWithoutNino)

  private def viewWithForm(form: Form[EiLPerson]): Html =
    noNinoExclusionSearchFormView(taxYearRange, "cyp1", "30", form, alreadyExists = true, EmpRef("", ""))

  "NoNinoExclusionSearchView" must {
    behave like pageWithTitle(view, messages("ExclusionSearch.form.title"))
    behave like pageWithContinueButtonForm(view, "/payrollbik/cyp1/medical/no-nino/search-for-employee", "Continue")
    behave like pageWithTextBox(view, "firstname", messages("Service.field.firstname"))
    behave like pageWithTextBox(view, "surname", messages("Service.field.surname"))
    behave like pageWithTextBox(view, "dob.day", messages("Service.field.dob.day"))
    behave like pageWithTextBox(view, "dob.month", messages("Service.field.dob.month"))
    behave like pageWithTextBox(view, "dob.year", messages("Service.field.dob.year"))
    behave like pageWithYesNoRadioButton(view, "gender-female", "gender-male")
  }

  "check the nino exclusion page for the empty errors" in {
    val view: Html = viewWithForm(
      formMappings.exclusionSearchFormWithoutNino.bind(
        Map(
          ("firstname", ""),
          ("surname", ""),
          ("dob.day", ""),
          ("dob.month", ""),
          ("dob.year", ""),
          ("gender-female", ""),
          ("gender-male", "")
        )
      )
    )

    doc(view) must haveErrorSummary(messages("error.empty.firstname").replace(".", ""))
    doc(view) must haveErrorNotification(messages("error.empty.firstname"))
    doc(view) must haveErrorSummary(messages("error.empty.lastname").replace(".", ""))
    doc(view) must haveErrorNotification(messages("error.empty.lastname"))
    doc(view) must haveErrorSummary(messages("error.empty.dob").replace(".", ""))
    doc(view) must haveErrorNotification(messages("error.empty.dob"))
    doc(view) must haveErrorSummary(messages("error.required").replace(".", ""))
    doc(view) must haveErrorNotification(messages("error.required"))
  }

  "check for invalid inputs" in {
    val view: Html = viewWithForm(
      formMappings.exclusionSearchFormWithoutNino.bind(
        Map(
          ("firstname", "1"),
          ("surname", "1"),
          ("dob.day", "01"),
          ("dob.month", "10"),
          ("dob.year", "9999")
        )
      )
    )

    doc(view) must haveErrorSummary(messages("error.incorrect.firstname").replace(".", ""))
    doc(view) must haveErrorNotification(messages("error.incorrect.firstname"))
    doc(view) must haveErrorSummary(messages("error.incorrect.lastname").replace(".", ""))
    doc(view) must haveErrorNotification(messages("error.incorrect.lastname"))
    doc(view) must haveErrorSummary(messages("error.invaliddate.future.year").replace(".", ""))
    doc(view) must haveErrorNotification(messages("error.invaliddate.future.year"))
    doc(view) must haveErrorSummary(messages("error.required").replace(".", ""))
    doc(view) must haveErrorNotification(messages("error.required"))
  }
}
