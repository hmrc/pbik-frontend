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
import views.helper.{PBIKViewBehaviours, PBIKViewSpec}


class NinoExclusionSearchViewSpec extends PBIKViewSpec with FormMappings {

  def taxYearRange = TaxYearRange(2018, 2019, 2020)
  val messageKeyPrefix = "ninoExclusionSearchForm"

  override def view: Html = viewWithForm(exclusionSearchFormWithoutNino)

  def viewWithForm(form: Form[EiLPerson]): Html =
    views.html.exclusion.ninoExclusionSearchForm(taxYearRange, "cyp1", "30", form, true, EmpRef("", ""))


  "exclusionNinoOrNoNinoForm" must {
    behave like pageWithTitle(messages("ExclusionSearch.form.title"))
    behave like pageWithHeader(messages("ExclusionSearch.form.title"))
    behave like pageWithContinueButtonForm("/payrollbik/cyp1/medical/nino/exclude-employee-results", "Continue")
    behave like pageWithTextBox("nino", messages("Service.field.nino"))
    behave like pageWithTextBox("firstname", messages("Service.field.firstname"))
    behave like pageWithTextBox("surname", messages("Service.field.surname"))
  }
  "check the nino exclusion page for the empty errors" in new PBIKViewBehaviours {

    val view = viewWithForm(exclusionSearchFormWithNino.bind(Map[String, String](("nino", ""),("firstname", ""), ("surname", ""))))

    doc must haveErrorSummary(messages("error.empty.nino").replace(".", ""))
    doc must haveErrorNotification(messages("error.empty.nino"))
    doc must haveErrorSummary(messages("error.empty.firstname").replace(".", ""))
    doc must haveErrorNotification(messages("error.empty.firstname"))
    doc must haveErrorSummary(messages("error.empty.lastname").replace(".", ""))
    doc must haveErrorNotification(messages("error.empty.lastname"))
  }

  "check the nino exclusion page for the already exist errors" in new PBIKViewBehaviours {

    val view = viewWithForm(exclusionSearchFormWithNino.bind(Map[String, String](("nino", "AA111111"),("firstname", "John"), ("surname", "Smith")))
      .withError("status", messages("ExclusionSearch.Fail.Exists.P")))

    doc must haveErrorSummary(messages("ExclusionSearch.Fail.Exists.P").replace(".", ""))
    doc must haveErrorNotification(messages("ExclusionSearch.Fail.Exists.P"))
  }

  "check the nino exclusion page for incorrect details errors" in new PBIKViewBehaviours {

    val view = viewWithForm(exclusionSearchFormWithNino.bind(Map[String, String](("nino", "AA123456"),("firstname", "John"), ("surname", "Smith"))) .withError("status", messages("ExclusionSearch.Fail.P")))

    doc must haveErrorSummary(messages("ExclusionSearch.Fail.P").replace(".", ""))
    doc must haveErrorNotification(messages("ExclusionSearch.Fail.P"))
  }

}
