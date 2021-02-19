/*
 * Copyright 2021 HM Revenue & Customs
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

import config.{AppConfig, LocalFormPartialRetriever, PbikAppConfig, PbikContext}
import controllers.ExternalUrls
import models.{EmpRef, TaxYearRange}
import org.jsoup.Jsoup
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.{FormMappings, TaxDateUtils, URIInformation}
import views.helper.PBIKViewSpec
import views.html.exclusion.ExclusionNinoOrNoNinoForm

class ExclusionNinoOrNoNinoViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val formMappings = app.injector.instanceOf[FormMappings]
  val exclusionNinoOrNoNinoFormView = app.injector.instanceOf[ExclusionNinoOrNoNinoForm]

  def taxYearRange = TaxYearRange(2018, 2019, 2020)

  override def view: Html = viewWithForm()

  implicit val context: PbikContext = app.injector.instanceOf[PbikContext]
  implicit val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]
  implicit val pbikAppConfig: PbikAppConfig = app.injector.instanceOf[PbikAppConfig]
  implicit val uriInformation: URIInformation = app.injector.instanceOf[URIInformation]
  implicit val externalURLs: ExternalUrls = app.injector.instanceOf[ExternalUrls]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val localFormPartialRetriever: LocalFormPartialRetriever = app.injector.instanceOf[LocalFormPartialRetriever]

  def viewWithForm(): Html =
    exclusionNinoOrNoNinoFormView(taxYearRange, "cyp1", "30", "", formMappings.binaryRadioButton, EmpRef("", ""))

  def viewWithFormWithErrors(): Html =
    exclusionNinoOrNoNinoFormView(
      taxYearRange,
      "cyp1",
      "30",
      "",
      formMappings.binaryRadioButton.withError("test", "error"),
      EmpRef("", ""))

  "exclusionNinoOrNoNinoPage" must {
    behave like pageWithTitle(messages("ExclusionNinoDecision.title"))
    behave like pageWithHeader(messages("ExclusionNinoDecision.title"))
    behave like pageWithContinueButtonForm("/payrollbik/cyp1/medical/exclude-employee-search", "Continue")
    behave like pageWithYesNoRadioButton("button-nino", "button-no-nino")

    "check the add benefit page for the errors" in {

      val doc = Jsoup.parse(viewWithFormWithErrors().toString())
      doc must haveErrorSummary(messages("ExclusionDecision.noselection.error"))
      doc must haveErrorNotification(messages("ExclusionDecision.noselection.error"))
    }

  }

}
