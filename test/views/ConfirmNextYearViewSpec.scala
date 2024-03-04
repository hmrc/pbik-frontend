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

import config.PbikAppConfig
import models.{EmpRef, RegistrationItem, RegistrationList}
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.registration.ConfirmUpdateNextTaxYear

class ConfirmNextYearViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi                               = app.injector.instanceOf[MessagesApi]
  val formMappings: FormMappings                             = app.injector.instanceOf[FormMappings]
  val confirmUpdateNextTaxYearView: ConfirmUpdateNextTaxYear = app.injector.instanceOf[ConfirmUpdateNextTaxYear]

  implicit def view: Html = confirmUpdateNextTaxYearView(
    bikList,
    taxYearRange,
    EmpRef("", "")
  )

  implicit val appConfig: PbikAppConfig    = app.injector.instanceOf[PbikAppConfig]
  val bikList: RegistrationList            = RegistrationList(active = List.empty[RegistrationItem])
  val removalBik: Option[RegistrationItem] = Some(RegistrationItem("30", active = true, enabled = true))

  "nextYearPage" must {
    behave like pageWithTitle(messages("AddBenefits.Confirm.Multiple.Title"))
    behave like pageWithHeader(
      messages("Overview.next.heading", taxYearRange.cy.toString, taxYearRange.cyplus1.toString)
        + " " + messages("AddBenefits.Confirm.Multiple.Title")
    )
    behave like pageWithContinueButtonForm("/payrollbik/cy1/check-the-benefits", "Confirm and continue")
  }
}
