/*
 * Copyright 2025 HM Revenue & Customs
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

import models.RegistrationList
import models.auth.AuthenticatedRequest
import models.v1.IabdType
import play.api.data.Form
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.registration.NextTaxYear

class CheckboxIdSpec extends PBIKViewSpec {

  val formMappings: FormMappings   = injected[FormMappings]
  val nextTaxYearView: NextTaxYear = injected[NextTaxYear]

  def viewWithForm(form: Form[RegistrationList])(implicit request: AuthenticatedRequest[_]): Html =
    nextTaxYearView(form, additive = true, taxYearRange, isExhausted = false, Set.empty, Set.empty)

  "first" should {
    "return a link to the first form checkbox" in {
      implicit def view: Html = viewWithForm(
        formMappings.objSelectedForm
          .withError("actives", "error")
          .bind(
            Map[String, String](
              "actives[2].uid" -> IabdType.Telephone.id.toString,
              "actives[0].uid" -> IabdType.VanBenefit.id.toString,
              "actives[1].uid" -> IabdType.Telephone.id.toString
            )
          )
      )(organisationRequest)

      doc must haveLinkWithUrlWithID("error-link", s"checkbox-${IabdType.VanBenefit.id.toString}")
    }

    "return a link to the first form checkbox ignoring id of always last 'Other' field" in {
      implicit def view: Html = viewWithForm(
        formMappings.objSelectedForm
          .withError("actives", "error")
          .bind(
            Map[String, String](
              "actives[0].uid" -> IabdType.OtherItems.id.toString,
              "actives[1].uid" -> IabdType.NonQualifyingRelocationExpenses.id.toString,
              "actives[2].uid" -> IabdType.VanFuelBenefit.id.toString
            )
          )
      )(organisationRequest)

      doc must haveLinkWithUrlWithID("error-link", s"checkbox-${IabdType.NonQualifyingRelocationExpenses.id.toString}")
    }

    "returns an empty link where a form is constructed with 0 elements" in {
      implicit def view: Html = viewWithForm(
        formMappings.objSelectedForm
          .withError("actives", "error")
      )(organisationRequest)

      doc must haveLinkWithUrlWithID("error-link", "")
    }
  }
}
