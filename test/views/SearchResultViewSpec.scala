/*
 * Copyright 2020 HM Revenue & Customs
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
import views.helper.{PBIKBaseViewSpec, PBIKViewBehaviours}
import views.html.exclusion.SearchResults

class SearchResultViewSpec extends PBIKBaseViewSpec {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val formMappings = app.injector.instanceOf[FormMappings]
  val searchResultsView = app.injector.instanceOf[SearchResults]
  val listOfMatches = EiLPersonList(List.empty[EiLPerson])

  def taxYearRange = TaxYearRange(2018, 2019, 2020)

  def viewWithForm(form: Form[ExclusionNino]): Html =
    searchResultsView(taxYearRange, "cyp1", "30", listOfMatches, form, "", EmpRef("", ""))

  "exclusionNinoOrNoNinoForm" in new PBIKViewBehaviours {

    override def view: Html = viewWithForm(formMappings.individualSelectionForm)

    behave like pageWithTitle(messages("ExclusionSearch.title"))
    behave like pageWithHeader(messages("ExclusionSearch.title"))
    behave like pageWithButtonForm("/payrollbik/cyp1/medical/exclude-employee-searchSearchResultViewSpec", "Confirm")

  }

  "check the nino search page for text validation" in new PBIKViewBehaviours {

    override def view: Html =
      viewWithForm(
        formMappings.individualSelectionForm.bind(
          Map[String, String](
            ("individualSelection", "AA111111"),
            ("firstname", "John"),
            ("surname", "Smith"),
            ("worksPayrollNumber", "123"))))

    behave like pageWithIdAndText("table-row-name", "John Smith")
    behave like pageWithIdAndText("table-row-nino", "AA111111")
    behave like pageWithIdAndText("table-row-dob", "123")

  }

}
