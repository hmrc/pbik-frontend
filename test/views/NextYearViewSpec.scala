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

import config.{AppConfig, LocalFormPartialRetriever, PbikContext}
import controllers.ExternalUrls
import models.{EmpRef, RegistrationList, TaxYearRange}
import org.jsoup.Jsoup
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.registration.NextTaxYear

class NextYearViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val formMappings = app.injector.instanceOf[FormMappings]
  val nextTaxYearView = app.injector.instanceOf[NextTaxYear]

  def taxYearRange = TaxYearRange(2018, 2019, 2020)

  override def view: Html = viewWithForm(formMappings.objSelectedForm)

  implicit val context: PbikContext = app.injector.instanceOf[PbikContext]
  implicit val externalURLs: ExternalUrls = app.injector.instanceOf[ExternalUrls]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val localFormPartialRetriever: LocalFormPartialRetriever = app.injector.instanceOf[LocalFormPartialRetriever]

  def viewWithForm(form: Form[RegistrationList]): Html =
    nextTaxYearView(form, additive = true, taxYearRange, List(), List(), List(), List(), Some(1), EmpRef("", ""))

  "nextYearPage" must {
    behave like pageWithTitle(messages("AddBenefits.Heading"))
    behave like pageWithHeader(messages("AddBenefits.Heading"))
    behave like pageWithContinueButtonForm("/payrollbik/cy1/choose-benefit-expense", "Continue")

    "check the add benefit page for the errors" in {
      val view = viewWithForm(formMappings.objSelectedForm.bind(Map[String, String]()))
      val doc = Jsoup.parse(view.toString())

      doc must haveErrorSummary(messages("AddBenefits.noselection.error"))
      doc must haveErrorNotification(messages("AddBenefits.noselection.error"))
    }

  }

}
