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

import config.{AppConfig, LocalFormPartialRetriever, PbikAppConfig}
import controllers.ExternalUrls
import models._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.{FormMappings, URIInformation}
import views.helper.{PBIKViewBehaviours, PBIKViewSpec}
import views.html.exclusion.NinoExclusionSearchForm

class NinoExclusionSearchViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val formMappings = app.injector.instanceOf[FormMappings]
  val ninoExclusionSearchFormView = app.injector.instanceOf[NinoExclusionSearchForm]

  def taxYearRange = TaxYearRange(2018, 2019, 2020)

  override def view: Html = viewWithForm(formMappings.exclusionSearchFormWithoutNino)

  implicit val pbikAppConfig: PbikAppConfig = app.injector.instanceOf[PbikAppConfig]
  implicit val uriInformation: URIInformation = app.injector.instanceOf[URIInformation]
  implicit val externalURLs: ExternalUrls = app.injector.instanceOf[ExternalUrls]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val localFormPartialRetriever: LocalFormPartialRetriever = app.injector.instanceOf[LocalFormPartialRetriever]

  def viewWithForm(form: Form[EiLPerson]): Html =
    ninoExclusionSearchFormView(taxYearRange, "cyp1", "30", form, alreadyExists = true, EmpRef("", ""))

  "ninoExclusionSearchPage" must {
    behave like pageWithTitle(messages("ExclusionSearch.form.title"))
    behave like pageWithHeader(messages("ExclusionSearch.form.title"))
    behave like pageWithContinueButtonForm("/payrollbik/cyp1/medical/nino/exclude-employee-form", "Continue")
    behave like pageWithTextBox("nino", messages("Service.field.nino"))
    behave like pageWithTextBox("firstname", messages("Service.field.firstname"))
    behave like pageWithTextBox("surname", messages("Service.field.surname"))
  }

  "check the nino exclusion page for the empty errors" in new PBIKViewBehaviours {

    val view: Html = viewWithForm(
      formMappings.exclusionSearchFormWithNino.bind(
        Map[String, String](("nino", ""), ("firstname", ""), ("surname", ""))))

    doc must haveErrorSummary(messages("error.empty.nino").replace(".", ""))
    doc must haveErrorNotification(messages("error.empty.nino"))
    doc must haveErrorSummary(messages("error.empty.firstname").replace(".", ""))
    doc must haveErrorNotification(messages("error.empty.firstname"))
    doc must haveErrorSummary(messages("error.empty.lastname").replace(".", ""))
    doc must haveErrorNotification(messages("error.empty.lastname"))
  }

  "check the nino exclusion page for the already exist errors" in new PBIKViewBehaviours {

    val view = viewWithForm(
      formMappings.exclusionSearchFormWithNino
        .bind(Map[String, String](("nino", "AA111111"), ("firstname", "John"), ("surname", "Smith")))
        .withError("status", messages("ExclusionSearch.Fail.Exists.P")))

    doc must haveErrorSummary(messages("ExclusionSearch.Fail.Exists.P").replace(".", ""))
    doc must haveErrorNotification(messages("ExclusionSearch.Fail.Exists.P"))
  }

  "check the nino exclusion page for incorrect details errors" in new PBIKViewBehaviours {

    val view = viewWithForm(
      formMappings.exclusionSearchFormWithNino
        .bind(Map[String, String](("nino", "AA123456"), ("firstname", "John"), ("surname", "Smith")))
        .withError("status", messages("ExclusionSearch.Fail.P")))

    doc must haveErrorSummary(messages("ExclusionSearch.Fail.P").replace(".", ""))
    doc must haveErrorNotification(messages("ExclusionSearch.Fail.P"))
  }

}
