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

package views.templatemodels

import models.{EmpRef, RegistrationList}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.registration.NextTaxYear

class CheckboxIdSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi     = app.injector.instanceOf[MessagesApi]
  val formMappings: FormMappings   = app.injector.instanceOf[FormMappings]
  val nextTaxYearView: NextTaxYear = app.injector.instanceOf[NextTaxYear]

  def viewWithForm(form: Form[RegistrationList]): Html =
    nextTaxYearView(form, additive = true, taxYearRange, List(), List(), List(), List(), Some(1), EmpRef("", ""))

  "first" should {
    "return a link to the first form checkbox" in {
      implicit def view: Html = viewWithForm(
        formMappings.objSelectedForm
          .withError("actives", "error")
          .bind(
            Map[String, String](
              "actives[2].uid" -> "32",
              "actives[0].uid" -> "37",
              "actives[1].uid" -> "32"
            )
          )
      )

      doc must haveLinkWithUrlWithID("error-link", "checkbox-37")
    }

    "return a link to the first form checkbox ignoring id of always last 'Other' field" in {
      implicit def view: Html = viewWithForm(
        formMappings.objSelectedForm
          .withError("actives", "error")
          .bind(
            Map[String, String](
              "actives[0].uid" -> "47",
              "actives[1].uid" -> "2",
              "actives[2].uid" -> "37"
            )
          )
      )

      doc must haveLinkWithUrlWithID("error-link", "checkbox-2")
    }

    "returns an empty link where a form is constructed with 0 elements" in {
      implicit def view: Html = viewWithForm(
        formMappings.objSelectedForm
          .withError("actives", "error")
      )

      doc must haveLinkWithUrlWithID("error-link", "")
    }
  }
}
