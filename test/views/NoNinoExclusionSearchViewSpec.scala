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

package views

import config.{AppConfig, LocalFormPartialRetriever, PbikContext}
import controllers.ExternalUrls
import models._
import play.api.data.Form
import play.twirl.api.Html
import utils.{FormMappings, URIInformation}
import views.helper.{PBIKViewBehaviours, PBIKViewSpec}


class NoNinoExclusionSearchViewSpec extends PBIKViewSpec with FormMappings {

  def taxYearRange = TaxYearRange(2018, 2019, 2020)

  override def view: Html = viewWithForm(exclusionSearchFormWithoutNino)

  implicit val context: PbikContext = app.injector.instanceOf[PbikContext]
  implicit val uriInformation: URIInformation = app.injector.instanceOf[URIInformation]
  implicit val externalURLs: ExternalUrls = app.injector.instanceOf[ExternalUrls]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val localFormPartialRetriever: LocalFormPartialRetriever = app.injector.instanceOf[LocalFormPartialRetriever]

  def viewWithForm(form: Form[EiLPerson]): Html =
    views.html.exclusion.noNinoExclusionSearchForm(taxYearRange, "cyp1", "30", form, alreadyExists = true, EmpRef("", ""))

  "noNinoExclusionSearchPage" must {
    behave like pageWithTitle(messages("ExclusionSearch.form.title"))
    behave like pageWithHeader(messages("ExclusionSearch.form.title"))
    behave like pageWithContinueButtonForm("/payrollbik/cyp1/medical/no-nino/exclude-employee-results", "Continue")
    behave like pageWithTextBox("firstname", messages("Service.field.firstname"))
    behave like pageWithTextBox("surname", messages("Service.field.surname"))
    behave like pageWithTextBox("dob.day", messages("Service.field.dob.day"))
    behave like pageWithTextBox("dob.month", messages("Service.field.dob.month"))
    behave like pageWithTextBox("dob.year", messages("Service.field.dob.year"))
    behave like pageWithYesNoRadioButton("gender-female", "gender-male")
  }

  "check the nino exclusion page for the empty errors" in new PBIKViewBehaviours {

    val view = viewWithForm(exclusionSearchFormWithoutNino.bind(Map[String, String](("firstname", ""), ("surname", ""), ("dob.day", ""), ("dob.month", ""), ("dob.year", ""), ("gender-female", ""), ("gender-male", ""))))

    doc must haveErrorSummary(messages("error.empty.firstname").replace(".", ""))
    doc must haveErrorNotification(messages("error.empty.firstname"))
    doc must haveErrorSummary(messages("error.empty.lastname").replace(".", ""))
    doc must haveErrorNotification(messages("error.empty.lastname"))
    doc must haveErrorSummary(messages("error.empty.dob").replace(".", ""))
    doc must haveErrorNotification(messages("error.empty.dob"))
    doc must haveErrorSummary(messages("error.required").replace(".", ""))
    doc must haveErrorNotification(messages("error.required"))
  }


}
