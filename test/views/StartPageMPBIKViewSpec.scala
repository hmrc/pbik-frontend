/*
 * Copyright 2026 HM Revenue & Customs
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
import views.html.{StartPageMpbik, StartPageMpbik2}

class StartPageMPBIKViewSpec extends PBIKViewSpec {

  val startPageView: StartPageMpbik        = injected[StartPageMpbik]
  val startPageMPBIK2View: StartPageMpbik2 = injected[StartPageMpbik2]

  def view()(implicit request: AuthenticatedRequest[?]): Html       = startPageView(true)
  def viewPhase2()(implicit request: AuthenticatedRequest[?]): Html = startPageMPBIK2View()

  "startPage - organisation" must {
    if (mpbikPhase2Toggle) {
      implicit val html: Html = viewPhase2()(organisationRequest)

      behave like pageWithTitle(messages(s"StartPageMPBIK2.heading.${organisationRequest.userType}"))
      behave like pageWithHeader(messages(s"StartPageMPBIK2.heading.${organisationRequest.userType}"))
      behave like pageWithLink(
        messages(s"StartPageMPBIK2.link.${organisationRequest.userType}"),
        "/payrollbik/registered-benefits-expenses"
      )
    } else {
      implicit val html: Html = view()(organisationRequest)

      behave like pageWithTitle(messages(s"StartPageMPBIK.heading.${organisationRequest.userType}"))
      behave like pageWithHeader(messages(s"StartPageMPBIK.heading.${organisationRequest.userType}"))
      behave like pageWithLink(
        messages(s"StartPageMPBIK.link.${organisationRequest.userType}"),
        "/payrollbik/registered-benefits-expenses"
      )
    }
  }

  "startPage - agent" must {
    if (mpbikPhase2Toggle) {
      implicit val html: Html = view()(agentRequest)

      behave like pageWithTitle(messages(s"StartPageMPBIK2.heading.${agentRequest.userType}"))
      behave like pageWithHeader(messages(s"StartPageMPBIK2.heading.${agentRequest.userType}"))
      behave like pageWithLink(
        messages(s"StartPageMPBIK2.link.${agentRequest.userType}"),
        "/payrollbik/registered-benefits-expenses"
      )
    } else {
      implicit val html: Html = view()(agentRequest)

      behave like pageWithTitle(messages(s"StartPageMPBIK.heading.${agentRequest.userType}"))
      behave like pageWithHeader(messages(s"StartPageMPBIK.heading.${agentRequest.userType}"))
      behave like pageWithLink(
        messages(s"StartPageMPBIK.link.${agentRequest.userType}"),
        "/payrollbik/registered-benefits-expenses"
      )
    }
  }

}
