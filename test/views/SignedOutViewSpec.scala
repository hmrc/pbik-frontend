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

import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import views.helper.PBIKViewSpec
import views.html.SignedOut

class SignedOutViewSpec extends PBIKViewSpec {

  val request: Request[AnyContentAsEmpty.type] = FakeRequest()

  val viewViaApply: HtmlFormat.Appendable  = injected[SignedOut].apply()(
    request = request,
    messages = messages
  )
  val viewViaRender: HtmlFormat.Appendable = injected[SignedOut].render(
    request = request,
    messages = messages
  )

  val viewViaF: HtmlFormat.Appendable = injected[SignedOut].ref.f()(request, messages)

  val signedOutView: SignedOut = injected[SignedOut]

  "signedOutView - organisation" must {
    implicit val view: Html = signedOutView()(organisationRequest, messages)

    behave like pageWithTitle(messages("signedOut.title"))
    behave like pageWithHeader(messages("signedOut.title"))
    behave like pageWithLink(messages("signedOut.signIn"), href = "/payrollbik/start-payrolling-benefits-expenses")
  }

  "signedOutView - agent" must {
    implicit val view: Html = signedOutView()(organisationRequest, messages)

    behave like pageWithTitle(messages("signedOut.title"))
    behave like pageWithHeader(messages("signedOut.title"))
    behave like pageWithLink(messages("signedOut.signIn"), href = "/payrollbik/start-payrolling-benefits-expenses")
  }

}
