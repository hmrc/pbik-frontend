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

import models._
import org.jsoup.Jsoup
import play.api.data.Form
import play.twirl.api.Html
import utils.FormMappings
import views.helper.{PBIKBaseViewSpec, PBIKViewBehaviours}


class SearchResultViewSpec extends PBIKBaseViewSpec with FormMappings {

  def taxYearRange = TaxYearRange(2018, 2019, 2020)

  def viewWithForm(form: Form[(String, EiLPersonList)]): Html =
    views.html.exclusion.searchResults(taxYearRange, "cyp1", "30", form, "", EmpRef("", ""))


  class TestSuite(form: Form[(String, EiLPersonList)]) extends PBIKViewBehaviours {

    override def view: Html = viewWithForm(form)

  }

  "exclusionNinoOrNoNinoForm" in new PBIKViewBehaviours {

    override def view: Html = viewWithForm(individualsFormWithRadio)

    behave like pageWithTitle(messages("ExclusionSearch.title"))
    behave like pageWithHeader(messages("ExclusionSearch.title"))
    behave like pageWithButtonForm("/payrollbik/cyp1/medical/exclude-employee-searchSearchResultViewSpec", "Confirm")

  }

  "check the nino search page for text validation" in new PBIKViewBehaviours {

    override def view: Html = viewWithForm(individualsFormWithRadio.bind(Map[String, String](("nino", "AA111111"), ("firstname", "John"), ("surname", "Smith"), ("worksPayrollNumber", "123"))))

    behave like pageWithIdAndText("table-row-name", "John Smith")
    behave like pageWithIdAndText("table-row-nino", "AA111111")
    behave like pageWithIdAndText("table-row-dob", "123")

  }







}
