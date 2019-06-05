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

import config.{AppConfig, LocalFormPartialRetriever}
import controllers.ExternalUrls
import models.{EmpRef, RegistrationList, TaxYearRange}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.Flash
import play.twirl.api.Html
import utils.{FormMappings, URIInformation}
import views.helper.PBIKViewSpec
import views.html.registration.ConfirmUpdateNextTaxYear

class ConfirmNextYearViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val formMappings: FormMappings = app.injector.instanceOf[FormMappings]
  val confirmUpdateNextTaxYearView: ConfirmUpdateNextTaxYear = app.injector.instanceOf[ConfirmUpdateNextTaxYear]

  def taxYearRange = TaxYearRange(2018, 2019, 2020)

  implicit val flash: Flash = new Flash

  override def view: Html = viewWithForm(formMappings.objSelectedForm)

  implicit val uriInformation: URIInformation = app.injector.instanceOf[URIInformation]
  implicit val externalURLs: ExternalUrls = app.injector.instanceOf[ExternalUrls]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val localFormPartialRetriever: LocalFormPartialRetriever = app.injector.instanceOf[LocalFormPartialRetriever]

  def viewWithForm(form: Form[RegistrationList]): Html =
    confirmUpdateNextTaxYearView(form, additive = true, taxYearRange, EmpRef("", ""))

  "nextYearPage" must {
    behave like pageWithTitle(messages("AddBenefits.Confirm.Single.Title"))
    behave like pageWithHeader(messages("AddBenefits.Confirm.Single.Title"))
    behave like pageWithContinueButtonForm("/payrollbik/cy1/add-benefit-expense-confirmed", "Register this benefit")

  }

}
