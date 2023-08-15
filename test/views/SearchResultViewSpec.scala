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
import views.helper.PBIKViewSpec
import views.html.exclusion.SearchResults

class SearchResultViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi         = app.injector.instanceOf[MessagesApi]
  val formMappings: FormMappings       = app.injector.instanceOf[FormMappings]
  val searchResultsView: SearchResults = app.injector.instanceOf[SearchResults]
  val listOfMatches: EiLPersonList     = EiLPersonList(
    List(
      EiLPerson(
        "AA111111",
        "John",
        Some("Stones"),
        "Smith",
        Some("123"),
        Some("01/01/1980"),
        Some("male"),
        Some(10),
        0
      )
    )
  )

  def viewWithForm(form: Form[ExclusionNino]): Html =
    searchResultsView(taxYearRange, "cyp1", "30", listOfMatches, form, "", EmpRef("", ""))

  "exclusionNinoOrNoNinoForm" should {

    implicit def view: Html = viewWithForm(formMappings.individualSelectionForm)

    behave like pageWithTitle(messages("ExclusionSearch.title.single"))
    behave like pageWithHeader(
      messages("BenefitInKind.label.30") + " " + messages("ExclusionSearch.title.single")
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
      )

    behave like pageWithIdAndText("John Smith", "name")
    behave like pageWithIdAndText("AA111111", "nino")
    behave like pageWithIdAndText("123", "wpn")
  }

  "check the individual nino search page for multiple active matches" should {
    val listOfActives = EiLPersonList(
      List(
        EiLPerson(
          "AA111111",
          "John",
          Some("Stones"),
          "Smith",
          Some("123"),
          Some("01/01/1980"),
          Some("male"),
          Some(10),
          0
        ),
        EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None, 0),
        EiLPerson(
          "AC111111",
          "Humpty",
          Some("Alexander"),
          "Dumpty",
          Some("123"),
          Some("01/01/1980"),
          Some("male"),
          Some(10),
          0
        )
      )
    )

    implicit def view: Html = searchResultsView(
      taxYearRange,
      "cyp1",
      "30",
      listOfActives,
      formMappings.individualSelectionForm,
      "",
      EmpRef("", "")
    )

    behave like pageWithTitle(messages("ExclusionSearch.title.multiple"))
    behave like pageWithHeader(
      messages("BenefitInKind.label.30") + " " + messages("ExclusionSearch.title.multiple")
    )
    behave like pageWithContinueButtonForm(
      "/payrollbik/cyp1/medical//exclude-employee-results",
      "Confirm and continue"
    )
  }
}
