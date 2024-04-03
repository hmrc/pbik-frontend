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

import models._
import play.api.data.Form
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.exclusion.NoNinoExclusionSearchForm

class NoNinoExclusionSearchViewSpec extends PBIKViewSpec {

  private val formMappings: FormMappings    = app.injector.instanceOf[FormMappings]
  private val noNinoExclusionSearchFormView = app.injector.instanceOf[NoNinoExclusionSearchForm]

  private def viewWithForm(form: Form[EiLPerson])(implicit request: AuthenticatedRequest[_]): Html =
    noNinoExclusionSearchFormView(taxYearRange, "cyp1", "medical", form, alreadyExists = true)

  "NoNinoExclusionSearchView - organisation" must {
    implicit def view: Html =
      viewWithForm(formMappings.exclusionSearchFormWithoutNino(organisationRequest))(organisationRequest)

    behave like pageWithTitle(messages("ExclusionSearch.form.title"))
    behave like pageWithContinueButtonForm("/payrollbik/cyp1/medical/no-nino/search-for-employee", "Continue")
    behave like pageWithTextBox("firstname", messages("Service.field.firstname"))
    behave like pageWithTextBox("surname", messages("Service.field.surname"))
    behave like pageWithTextBox("dob.day", messages("Service.field.dob.day"))
    behave like pageWithTextBox("dob.month", messages("Service.field.dob.month"))
    behave like pageWithTextBox("dob.year", messages("Service.field.dob.year"))
    behave like pageWithYesNoRadioButton("gender-female", "gender-male")

    "check the nino exclusion page for the empty errors" in {
      implicit def view: Html = viewWithForm(
        formMappings
          .exclusionSearchFormWithoutNino(organisationRequest)
          .bind(
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
      )(organisationRequest)

      doc must haveErrorSummary(messages("error.empty.firstname").replace(".", ""))
      doc must haveErrorNotification(messages("error.empty.firstname"))
      doc must haveErrorSummary(messages("error.empty.lastname").replace(".", ""))
      doc must haveErrorNotification(messages("error.empty.lastname"))
      doc must haveErrorSummary(messages("error.empty.dob").replace(".", ""))
      doc must haveErrorNotification(messages("error.empty.dob"))
      doc must haveErrorSummary(messages("error.required").replace(".", ""))
      doc must haveErrorNotification(messages("error.required"))
    }

    "check for invalid inputs" in {
      implicit val view: Html = viewWithForm(
        formMappings
          .exclusionSearchFormWithoutNino(organisationRequest)
          .bind(
            Map(
              ("firstname", "1"),
              ("surname", "1"),
              ("dob.day", "01"),
              ("dob.month", "10"),
              ("dob.year", "9999")
            )
          )
      )(organisationRequest)

      doc must haveErrorSummary(messages("error.incorrect.firstname").replace(".", ""))
      doc must haveErrorNotification(messages("error.incorrect.firstname"))
      doc must haveErrorSummary(messages("error.incorrect.lastname").replace(".", ""))
      doc must haveErrorNotification(messages("error.incorrect.lastname"))
      doc must haveErrorSummary(messages("error.invaliddate.future.year").replace(".", ""))
      doc must haveErrorNotification(messages("error.invaliddate.future.year"))
      doc must haveErrorSummary(messages("error.required").replace(".", ""))
      doc must haveErrorNotification(messages("error.required"))
    }
  }

  "NoNinoExclusionSearchView - agent" must {
    implicit def view: Html = viewWithForm(formMappings.exclusionSearchFormWithoutNino(agentRequest))(agentRequest)

    behave like pageWithTitle(messages("ExclusionSearch.form.title"))
    behave like pageWithContinueButtonForm("/payrollbik/cyp1/medical/no-nino/search-for-employee", "Continue")
    behave like pageWithTextBox("firstname", messages("Service.field.firstname"))
    behave like pageWithTextBox("surname", messages("Service.field.surname"))
    behave like pageWithTextBox("dob.day", messages("Service.field.dob.day"))
    behave like pageWithTextBox("dob.month", messages("Service.field.dob.month"))
    behave like pageWithTextBox("dob.year", messages("Service.field.dob.year"))
    behave like pageWithYesNoRadioButton("gender-female", "gender-male")

    "check the nino exclusion page for the empty errors" in {
      implicit def view: Html = viewWithForm(
        formMappings
          .exclusionSearchFormWithoutNino(agentRequest)
          .bind(
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
      )(agentRequest)

      doc must haveErrorSummary(messages("error.empty.firstname").replace(".", ""))
      doc must haveErrorNotification(messages("error.empty.firstname"))
      doc must haveErrorSummary(messages("error.empty.lastname").replace(".", ""))
      doc must haveErrorNotification(messages("error.empty.lastname"))
      doc must haveErrorSummary(messages("error.empty.dob").replace(".", ""))
      doc must haveErrorNotification(messages("error.empty.dob"))
      doc must haveErrorSummary(messages("error.required").replace(".", ""))
      doc must haveErrorNotification(messages("error.required"))
    }

    "check for invalid inputs" in {
      implicit val view: Html = viewWithForm(
        formMappings
          .exclusionSearchFormWithoutNino(agentRequest)
          .bind(
            Map(
              ("firstname", "1"),
              ("surname", "1"),
              ("dob.day", "01"),
              ("dob.month", "10"),
              ("dob.year", "9999")
            )
          )
      )(agentRequest)

      doc must haveErrorSummary(messages("error.incorrect.firstname").replace(".", ""))
      doc must haveErrorNotification(messages("error.incorrect.firstname"))
      doc must haveErrorSummary(messages("error.incorrect.lastname").replace(".", ""))
      doc must haveErrorNotification(messages("error.incorrect.lastname"))
      doc must haveErrorSummary(messages("error.invaliddate.future.year").replace(".", ""))
      doc must haveErrorNotification(messages("error.invaliddate.future.year"))
      doc must haveErrorSummary(messages("error.required").replace(".", ""))
      doc must haveErrorNotification(messages("error.required"))
    }
  }

}
