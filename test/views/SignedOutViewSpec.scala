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

import play.api.i18n.MessagesApi
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.SignedOut

class SignedOutViewSpec extends PBIKViewSpec {

  val signedOutView: SignedOut = app.injector.instanceOf[SignedOut]

  override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  override val view: Html = signedOutView()

  "signedOutView" must {
    behave like pageWithTitle(messages("signedOut.title"))
    behave like pageWithHeader(messages("signedOut.title"))
    behave like pageWithLink(messages("signedOut.signIn"), href = "/payrollbik/payrolled-benefits-expenses")
  }

}
