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

import config.{AppConfig, LocalFormPartialRetriever, PbikAppConfig}
import models._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.{FormMappings, URIInformation}
import views.helper.PBIKViewSpec
import views.html.exclusion.NinoExclusionSearchForm

class NinoExclusionSearchViewSpec extends PBIKViewSpec {

  val formMappings: FormMappings                           = app.injector.instanceOf[FormMappings]
  val ninoExclusionSearchFormView: NinoExclusionSearchForm = app.injector.instanceOf[NinoExclusionSearchForm]

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val pbikAppConfig: PbikAppConfig                         = app.injector.instanceOf[PbikAppConfig]
  implicit val uriInformation: URIInformation                       = app.injector.instanceOf[URIInformation]
  implicit val appConfig: AppConfig                                 = app.injector.instanceOf[AppConfig]
  implicit val localFormPartialRetriever: LocalFormPartialRetriever = app.injector.instanceOf[LocalFormPartialRetriever]

  def viewWithForm(form: Form[EiLPerson]): Html =
    ninoExclusionSearchFormView(taxYearRange, "cyp1", "30", form, alreadyExists = true, EmpRef("", ""))

  "NinoExclusionSearchView" must {
    implicit def view: Html = viewWithForm(formMappings.exclusionSearchFormWithoutNino)

    behave like pageWithTitle(messages("ExclusionSearch.form.title"))
    behave like pageWithHeader(messages("ExclusionSearch.form.header"))
    behave like pageWithContinueButtonForm("/payrollbik/cyp1/medical/nino/search-for-employee", "Continue")
    behave like pageWithTextBox("nino", messages("Service.field.nino"))
    behave like pageWithTextBox("firstname", messages("Service.field.firstname"))
    behave like pageWithTextBox("surname", messages("Service.field.surname"))
  }

  "check the nino exclusion page for the empty errors" in {
    implicit def view: Html = viewWithForm(
      formMappings.exclusionSearchFormWithNino.bind(
        Map(("nino", ""), ("firstname", ""), ("surname", ""))
      )
    )

    doc must haveErrorSummary(messages("error.empty.nino").replace(".", ""))
    doc must haveErrorNotification(messages("error.empty.nino"))
    doc must haveErrorSummary(messages("error.empty.firstname").replace(".", ""))
    doc must haveErrorNotification(messages("error.empty.firstname"))
    doc must haveErrorSummary(messages("error.empty.lastname").replace(".", ""))
    doc must haveErrorNotification(messages("error.empty.lastname"))
  }

  "check for invalid inputs" in {
    implicit def view: Html = viewWithForm(
      formMappings.exclusionSearchFormWithNino.bind(
        Map(("nino", "1"), ("firstname", "1"), ("surname", "1"))
      )
    )

    doc must haveErrorSummary(messages("error.incorrect.nino").replace(".", ""))
    doc must haveErrorNotification(messages("error.incorrect.nino"))
    doc must haveErrorSummary(messages("error.incorrect.firstname").replace(".", ""))
    doc must haveErrorNotification(messages("error.incorrect.firstname"))
    doc must haveErrorSummary(messages("error.incorrect.lastname").replace(".", ""))
    doc must haveErrorNotification(messages("error.incorrect.lastname"))
  }
}
