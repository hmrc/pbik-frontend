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

package views

import models._
import models.auth.AuthenticatedRequest
import models.form.ExclusionNino
import models.v1.IabdType
import models.v1.trace.TracePersonResponse
import play.api.data.Form
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.exclusion.SearchResults

class SearchResultViewSpec extends PBIKViewSpec {

  val formMappings: FormMappings       = injected[FormMappings]
  val searchResultsView: SearchResults = injected[SearchResults]

  private val iabdType = IabdType.MedicalInsurance

  def viewWithForm(form: Form[ExclusionNino])(implicit request: AuthenticatedRequest[?]): Html =
    searchResultsView(taxYearRange, "cyp1", iabdType, List(tracePerson), form, "")

  "organisation" must {
    "exclusionNinoOrNoNinoForm" should {

      implicit def view: Html = viewWithForm(formMappings.individualSelectionForm)(organisationRequest)

      behave like pageWithTitle(messages("ExclusionSearch.title.single"))
      behave like pageWithHeader(
        messages(s"BenefitInKind.label.${iabdType.id}") + " " + messages("ExclusionSearch.title.single")
      )
      behave like pageWithElementAndText(
        "button-confirm",
        messages("Service.confirm")
      )
    }

    "check the nino search page for text validation" should {

      implicit def view: Html =
        viewWithForm(
          formMappings.individualSelectionForm.bind(
            Map[String, String](
              ("individualNino", "AA111111")
            )
          )
        )(organisationRequest)

      behave like pageWithIdAndText(s"${tracePerson.fullName}", "name")
      behave like pageWithIdAndText(tracePerson.nationalInsuranceNumber, "nino")
      behave like pageWithIdAndText(tracePerson.getWorksPayrollNumber, "wpn")
    }

    "check the individual nino search page for multiple active matches" should {
      val listOfActives =
        List(
          TracePersonResponse("AB123456C", "John", Some("A"), "Doe", None, 22),
          TracePersonResponse("AB123456D", "Jane", Some("B"), "Doe", Some("12345"), 22),
          TracePersonResponse("AB123456E", "Jora", Some("C"), "Doe", Some("12345"), 22)
        )

      implicit def view: Html = searchResultsView(
        taxYearRange,
        "cyp1",
        iabdType,
        listOfActives,
        formMappings.individualSelectionForm,
        ""
      )(organisationRequest, messages)

      behave like pageWithTitle(messages("ExclusionSearch.title.multiple"))
      behave like pageWithHeader(
        messages(s"BenefitInKind.label.${iabdType.id}") + " " + messages("ExclusionSearch.title.multiple")
      )
      behave like pageWithContinueButtonForm(
        s"/payrollbik/cyp1/${iabdType.id}//exclude-employee-results",
        "Confirm and continue"
      )
    }
  }

  "Agent" must {
    "exclusionNinoOrNoNinoForm" should {

      implicit def view: Html = viewWithForm(formMappings.individualSelectionForm)(agentRequest)

      behave like pageWithTitle(messages("ExclusionSearch.title.single"))
      behave like pageWithHeader(
        messages(s"BenefitInKind.label.${iabdType.id}") + " " + messages("ExclusionSearch.title.single")
      )
      behave like pageWithElementAndText(
        "button-confirm",
        messages("Service.confirm")
      )
    }

    "check the nino search page for text validation" should {

      implicit def view: Html =
        viewWithForm(
          formMappings.individualSelectionForm.bind(
            Map[String, String](
              ("individualNino", "AA111111")
            )
          )
        )(agentRequest)

      behave like pageWithIdAndText(s"${tracePerson.fullName}", "name")
      behave like pageWithIdAndText(tracePerson.nationalInsuranceNumber, "nino")
      behave like pageWithIdAndText(tracePerson.getWorksPayrollNumber, "wpn")
    }

    "check the individual nino search page for multiple active matches" should {
      val listOfActives =
        List(
          TracePersonResponse("AB123456C", "John", Some("A"), "Doe", None, 22),
          TracePersonResponse("AB123456D", "Jane", Some("B"), "Doe", None, 22),
          TracePersonResponse("AB123456E", "Jora", Some("C"), "Doe", Some("12345"), 22)
        )

      implicit def view: Html = searchResultsView(
        taxYearRange,
        "cyp1",
        iabdType,
        listOfActives,
        formMappings.individualSelectionForm,
        ""
      )(agentRequest, messages)

      behave like pageWithTitle(messages("ExclusionSearch.title.multiple"))
      behave like pageWithHeader(
        messages(s"BenefitInKind.label.${iabdType.id}") + " " + messages("ExclusionSearch.title.multiple")
      )
      behave like pageWithContinueButtonForm(
        s"/payrollbik/cyp1/${iabdType.id}//exclude-employee-results",
        "Confirm and continue"
      )
    }
  }

}
