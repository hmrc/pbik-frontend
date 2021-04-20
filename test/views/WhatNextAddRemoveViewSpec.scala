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

import models.{EmpRef, RegistrationItem, RegistrationList, TaxYearRange}
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.registration.WhatNextAddRemove

class WhatNextAddRemoveViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val formMappings = app.injector.instanceOf[FormMappings]
  val whatNextAddRemoveView = app.injector.instanceOf[WhatNextAddRemove]

  def taxYearRange = TaxYearRange(2018, 2019, 2020)

  override def view: Html = {
    val regList = RegistrationList(active = List(RegistrationItem("30", true, true)))
    whatNextAddRemoveView(isCurrentYear = true, taxYearRange, additive = true, regList, EmpRef("", ""))
  }

  "whatNextAddRemove" must {
    behave like pageWithTitle(messages("whatNext.add.heading"))
    behave like pageWithHeader(messages("whatNext.add.heading"))
    behave like pageWithLink(messages("Service.back.overview.whatNext"), "/payrollbik/payrolled-benefits-expenses")

  }
}
