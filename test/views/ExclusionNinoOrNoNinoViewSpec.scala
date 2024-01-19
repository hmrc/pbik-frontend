/*
 * Copyright 2024 HM Revenue & Customs
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
import models.EmpRef
import org.jsoup.Jsoup
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.{FormMappings, TaxDateUtils}
import views.helper.PBIKViewSpec
import views.html.exclusion.ExclusionNinoOrNoNinoForm

class ExclusionNinoOrNoNinoViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi                                 = app.injector.instanceOf[MessagesApi]
  val formMappings: FormMappings                               = app.injector.instanceOf[FormMappings]
  val exclusionNinoOrNoNinoFormView: ExclusionNinoOrNoNinoForm = app.injector.instanceOf[ExclusionNinoOrNoNinoForm]

  implicit def view: Html = viewWithForm()

  implicit val taxDateUtils: TaxDateUtils                           = app.injector.instanceOf[TaxDateUtils]
  implicit val pbikAppConfig: PbikAppConfig                         = app.injector.instanceOf[PbikAppConfig]
  implicit val appConfig: AppConfig                                 = app.injector.instanceOf[AppConfig]
  implicit val localFormPartialRetriever: LocalFormPartialRetriever = app.injector.instanceOf[LocalFormPartialRetriever]

  def viewWithForm(): Html =
    exclusionNinoOrNoNinoFormView(taxYearRange, "cyp1", "medical", "", formMappings.binaryRadioButton, EmpRef("", ""))

  def viewWithFormWithErrors(): Html =
    exclusionNinoOrNoNinoFormView(
      taxYearRange,
      "cyp1",
      "medical",
      "",
      formMappings.binaryRadioButton.withError("test", "error"),
      EmpRef("", "")
    )

  "exclusionNinoOrNoNinoPage" must {
    behave like pageWithTitle(messages("ExclusionNinoDecision.title"))
    behave like pageWithHeader(messages("ExclusionNinoDecision.title"))
    behave like pageWithContinueButtonForm("/payrollbik/cyp1/medical/employee-national-insurance-number", "Continue")
    behave like pageWithYesNoRadioButton("button-nino", "button-no-nino")

    "check the add benefit page for the errors" in {

      val doc = Jsoup.parse(viewWithFormWithErrors().toString())
      doc must haveErrorSummary(messages("ExclusionDecision.noselection.error"))
      doc must haveErrorNotification(messages("ExclusionDecision.noselection.error"))
    }
  }

}
