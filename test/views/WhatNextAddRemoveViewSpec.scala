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

import models.{EmpRef, RegistrationList, TaxYearRange}
import org.jsoup.Jsoup
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec


class WhatNextAddRemoveViewSpec extends PBIKViewSpec with FormMappings {

  def taxYearRange = TaxYearRange(2018, 2019, 2020)

  override def view: Html = viewWithForm(objSelectedForm)

  def viewWithForm(form: Form[RegistrationList]): Html = {
    val boundForm = form.bind(Map("actives[0].uid" -> "abc"))
    views.html.registration.whatNextAddRemove(true, taxYearRange, true, boundForm, EmpRef("", ""))
  }

  "whatNextAddRemove" must {
    behave like pageWithTitle(messages("whatNext.add.heading"))
    behave like pageWithHeader(messages("whatNext.add.heading"))
    behave like pageWithLink(messages("Service.back.overview.whatNext"), "/payrollbik/payrolled-benefits-expenses")

  }
}
