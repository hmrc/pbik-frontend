/*
 * Copyright 2025 HM Revenue & Customs
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
import models.form.NinoForm
import models.v1.IabdType
import play.api.data.Form
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.exclusion.NinoExclusionSearchForm

class NinoExclusionSearchViewSpec extends PBIKViewSpec {

  val formMappings: FormMappings                           = injected[FormMappings]
  val ninoExclusionSearchFormView: NinoExclusionSearchForm = injected[NinoExclusionSearchForm]

  private val iabdType = IabdType.Mileage

  def viewWithForm(form: Form[NinoForm])(implicit request: AuthenticatedRequest[_]): Html =
    ninoExclusionSearchFormView(taxYearRange, "cyp1", iabdType, form, alreadyExists = true)

  "NinoExclusionSearchView - organisation" must {
    implicit def view: Html =
      viewWithForm(formMappings.exclusionSearchFormWithNino(organisationRequest))(organisationRequest)

    behave like pageWithTitle(messages("ExclusionSearch.form.title"))
    behave like pageWithHeader(messages("ExclusionSearch.form.header"))
    behave like pageWithContinueButtonForm(s"/payrollbik/cyp1/${iabdType.id}/nino/search-for-employee", "Continue")
    behave like pageWithTextBox("nino", messages("Service.field.nino"))
    behave like pageWithTextBox("firstname", messages("Service.field.firstname"))
    behave like pageWithTextBox("surname", messages("Service.field.surname"))

    "check the nino exclusion page for the empty errors" in {
      implicit def view: Html = viewWithForm(
        formMappings
          .exclusionSearchFormWithNino(organisationRequest)
          .bind(
            Map(("nino", ""), ("firstname", ""), ("surname", ""))
          )
      )(organisationRequest)

      doc must haveErrorSummary(messages("error.empty.nino").replace(".", ""))
      doc must haveErrorNotification(messages("error.empty.nino"))
      doc must haveErrorSummary(messages("error.empty.firstname").replace(".", ""))
      doc must haveErrorNotification(messages("error.empty.firstname"))
      doc must haveErrorSummary(messages("error.empty.lastname").replace(".", ""))
      doc must haveErrorNotification(messages("error.empty.lastname"))
    }

    "check for invalid inputs" in {
      implicit def view: Html = viewWithForm(
        formMappings
          .exclusionSearchFormWithNino(organisationRequest)
          .bind(
            Map(("nino", "1"), ("firstname", "1"), ("surname", "1"))
          )
      )(organisationRequest)

      doc must haveErrorSummary(messages("error.incorrect.nino").replace(".", ""))
      doc must haveErrorNotification(messages("error.incorrect.nino"))
      doc must haveErrorSummary(messages("error.incorrect.firstname").replace(".", ""))
      doc must haveErrorNotification(messages("error.incorrect.firstname"))
      doc must haveErrorSummary(messages("error.incorrect.lastname").replace(".", ""))
      doc must haveErrorNotification(messages("error.incorrect.lastname"))
    }
  }

  "NinoExclusionSearchView - agent" must {
    implicit def view: Html =
      viewWithForm(formMappings.exclusionSearchFormWithNino(agentRequest))(agentRequest)

    behave like pageWithTitle(messages("ExclusionSearch.form.title"))
    behave like pageWithHeader(messages("ExclusionSearch.form.header"))
    behave like pageWithContinueButtonForm(s"/payrollbik/cyp1/${iabdType.id}/nino/search-for-employee", "Continue")
    behave like pageWithTextBox("nino", messages("Service.field.nino"))
    behave like pageWithTextBox("firstname", messages("Service.field.firstname"))
    behave like pageWithTextBox("surname", messages("Service.field.surname"))

    "check the nino exclusion page for the empty errors" in {
      implicit def view: Html = viewWithForm(
        formMappings
          .exclusionSearchFormWithNino(agentRequest)
          .bind(
            Map(("nino", ""), ("firstname", ""), ("surname", ""))
          )
      )(agentRequest)

      doc must haveErrorSummary(messages("error.empty.nino").replace(".", ""))
      doc must haveErrorNotification(messages("error.empty.nino"))
      doc must haveErrorSummary(messages("error.empty.firstname").replace(".", ""))
      doc must haveErrorNotification(messages("error.empty.firstname"))
      doc must haveErrorSummary(messages("error.empty.lastname").replace(".", ""))
      doc must haveErrorNotification(messages("error.empty.lastname"))
    }

    "check for invalid inputs" in {
      implicit def view: Html = viewWithForm(
        formMappings
          .exclusionSearchFormWithNino(agentRequest)
          .bind(
            Map(("nino", "1"), ("firstname", "1"), ("surname", "1"))
          )
      )(agentRequest)

      doc must haveErrorSummary(messages("error.incorrect.nino").replace(".", ""))
      doc must haveErrorNotification(messages("error.incorrect.nino"))
      doc must haveErrorSummary(messages("error.incorrect.firstname").replace(".", ""))
      doc must haveErrorNotification(messages("error.incorrect.firstname"))
      doc must haveErrorSummary(messages("error.incorrect.lastname").replace(".", ""))
      doc must haveErrorNotification(messages("error.incorrect.lastname"))
    }
  }

}
