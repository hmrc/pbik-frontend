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

import models.auth.AuthenticatedRequest
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.SelectYearPage

class SelectYearPageViewSpec extends PBIKViewSpec {

  private lazy val selectYearPageView: SelectYearPage = app.injector.instanceOf[SelectYearPage]
  private val formMappings: FormMappings              = app.injector.instanceOf[FormMappings]

  def view()(implicit request: AuthenticatedRequest[_]): Html =
    selectYearPageView(taxYearRange, formMappings.selectYearForm)

  "startPage - organisation" must {
    implicit val html: Html = view()(organisationRequest)

    behave like pageWithTitle(messages(s"SelectYear.title"))
    behave like pageWithHeader(messages(s"SelectYear.heading"))
    behave like pageWithIdAndText(
      messages(s"SelectYear.option1", taxYearRange.cy.toString),
      "cyp1-label"
    )
    behave like pageWithIdAndText(messages(s"SelectYear.option2"), "cy-label")
  }

  "startPage - agent" must {
    implicit val html: Html = view()(agentRequest)

    behave like pageWithTitle(messages(s"SelectYear.title"))
    behave like pageWithHeader(messages(s"SelectYear.heading"))
    behave like pageWithIdAndText(
      messages(s"SelectYear.option1", taxYearRange.cy.toString),
      "cyp1-label"
    )
    behave like pageWithIdAndText(messages(s"SelectYear.option2"), "cy-label")
  }

}
