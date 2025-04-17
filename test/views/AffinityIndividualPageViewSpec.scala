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

import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import views.helper.PBIKViewSpec
import views.html.AffinityIndividualPage

class AffinityIndividualPageViewSpec extends PBIKViewSpec {

  val request: Request[AnyContentAsEmpty.type] = FakeRequest()

  val viewViaApply: HtmlFormat.Appendable  = injected[AffinityIndividualPage].apply()(
    request = request,
    messages = messages
  )
  val viewViaRender: HtmlFormat.Appendable = injected[AffinityIndividualPage].render(
    request = request,
    messages = messages
  )

  val viewViaF: HtmlFormat.Appendable = injected[AffinityIndividualPage].ref.f()(request, messages)

  val affinityIndividualPageView: AffinityIndividualPage = injected[AffinityIndividualPage]

  "affinityIndividualPageViewSpec" must {
    implicit val view: Html = affinityIndividualPageView()(organisationRequest, messages)

    behave like pageWithTitle(messages("ErrorPage.title.individual"))
    behave like pageWithHeader(messages("ErrorPage.title.individual"))
    behave like pageWithLink(messages("timeout.signOut"), href = "/payrollbik/individual-signout")
  }
}
