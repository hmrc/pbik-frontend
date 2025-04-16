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

package views

import models.auth.AuthenticatedRequest
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.StartPage

class StartPageViewSpec extends PBIKViewSpec {

  val startPageView: StartPage = injected[StartPage]

  def view()(implicit request: AuthenticatedRequest[_]): Html = startPageView()

  "startPage - organisation" must {
    implicit val html: Html = view()(organisationRequest)

    behave like pageWithTitle(messages(s"StartPage.heading.${organisationRequest.userType}"))
    behave like pageWithHeader(messages(s"StartPage.heading.${organisationRequest.userType}"))
    behave like pageWithLink(
      messages(s"StartPage.link.${organisationRequest.userType}"),
      "/payrollbik/select-year"
    )
  }

  "startPage - agent" must {
    implicit val html: Html = view()(agentRequest)

    behave like pageWithTitle(messages(s"StartPage.heading.${agentRequest.userType}"))
    behave like pageWithHeader(messages(s"StartPage.heading.${agentRequest.userType}"))
    behave like pageWithLink(
      messages(s"StartPage.link.${agentRequest.userType}"),
      "/payrollbik/select-year"
    )
  }

}
