/*
 * Copyright 2026 HM Revenue & Customs
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
import org.jsoup.Jsoup
import views.html.exclusion.NinoExclusionSearchForm

class NinoExclusionSearchViewSpec extends PBIKViewSpec {

  val formMappings: FormMappings                           = injected[FormMappings]
  val ninoExclusionSearchFormView: NinoExclusionSearchForm = injected[NinoExclusionSearchForm]

  private val iabdType        = IabdType.Mileage
  private val iabdTypeMileage = "Mileage allowance and passenger payments"

  private val april2026MpbikToggle: Boolean = pbikAppConfig.mpbikToggle

  def viewWithForm(form: Form[NinoForm])(implicit request: AuthenticatedRequest[?]): Html =
    ninoExclusionSearchFormView(taxYearRange, "cyp1", iabdType, form, alreadyExists = true)

  "NinoExclusionSearchView - organisation" must {
    implicit def view: Html =
      viewWithForm(formMappings.exclusionSearchFormWithNino(organisationRequest))(organisationRequest)

    behave like pageWithTitle(messages("ExclusionSearch.form.title"))
    if (april2026MpbikToggle)
      behave like pageWithHeader(messages("ExclusionSearch.form.headerMPBIK", "" + iabdTypeMileage))
    else
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

      val msgIncorrectFirstname =
        if (april2026MpbikToggle)
          "error.incorrect.firstnameMPBIK"
        else
          "error.incorrect.firstname"

      val msgIncorrectLastname =
        if (april2026MpbikToggle)
          "error.incorrect.lastnameMPBIK"
        else
          "error.incorrect.lastname"

      doc must haveErrorSummary(messages(msgIncorrectFirstname).replace(".", ""))
      doc must haveErrorNotification(messages(msgIncorrectFirstname))
      doc must haveErrorSummary(messages(msgIncorrectLastname).replace(".", ""))
      doc must haveErrorNotification(messages(msgIncorrectLastname))
    }

    "check for name length validation" in {
      val longName = "a" * 36

      implicit def view: Html = viewWithForm(
        formMappings
          .exclusionSearchFormWithNino(organisationRequest)
          .bind(
            Map(
              "nino"      -> "AA123456A",
              "firstname" -> longName,
              "surname"   -> longName
            )
          )
      )(organisationRequest)

      if (april2026MpbikToggle) {
        doc must haveErrorSummary(messages("error.firstname.lengthMPBIK").replace(".", ""))
        doc must haveErrorNotification(messages("error.firstname.lengthMPBIK"))
        doc must haveErrorSummary(messages("error.lastname.lengthMPBIK").replace(".", ""))
        doc must haveErrorNotification(messages("error.lastname.lengthMPBIK"))
      }
    }
  }

  "NinoExclusionSearchView - agent" must {
    implicit def view: Html =
      viewWithForm(formMappings.exclusionSearchFormWithNino(agentRequest))(agentRequest)

    behave like pageWithTitle(messages("ExclusionSearch.form.title"))
    if (april2026MpbikToggle)
      behave like pageWithHeader(messages("ExclusionSearch.form.headerMPBIK", "" + iabdTypeMileage))
    else
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

      val msgIncorrectFirstname =
        if (april2026MpbikToggle)
          "error.incorrect.firstnameMPBIK"
        else
          "error.incorrect.firstname"

      val msgIncorrectLastname =
        if (april2026MpbikToggle)
          "error.incorrect.lastnameMPBIK"
        else
          "error.incorrect.lastname"

      doc must haveErrorSummary(messages(msgIncorrectFirstname).replace(".", ""))
      doc must haveErrorNotification(messages(msgIncorrectFirstname))
      doc must haveErrorSummary(messages(msgIncorrectLastname).replace(".", ""))
      doc must haveErrorNotification(messages(msgIncorrectLastname))
    }

    "check for name length validation" in {
      val longName = "a" * 36

      implicit def view: Html = viewWithForm(
        formMappings
          .exclusionSearchFormWithNino(agentRequest)
          .bind(
            Map(
              "nino"      -> "AA123456A",
              "firstname" -> longName,
              "surname"   -> longName
            )
          )
      )(agentRequest)

      if (april2026MpbikToggle) {
        doc must haveErrorSummary(messages("error.firstname.lengthMPBIK").replace(".", ""))
        doc must haveErrorNotification(messages("error.firstname.lengthMPBIK"))
        doc must haveErrorSummary(messages("error.lastname.lengthMPBIK").replace(".", ""))
        doc must haveErrorNotification(messages("error.lastname.lengthMPBIK"))
      }
    }

    "show status error when status has errors" in {
      val formWithStatusError = formMappings
        .exclusionSearchFormWithNino(organisationRequest)
        .withError("status", "error-status")

      implicit def view: Html = viewWithForm(formWithStatusError)(organisationRequest)
      val doc                 = Jsoup.parse(view.toString)

      doc                              must haveErrorSummary("error-status")
      doc.select("#error-list-1").text must include("error-status")
    }
  }

}
