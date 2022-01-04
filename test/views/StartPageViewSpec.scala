/*
 * Copyright 2022 HM Revenue & Customs
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

import models.EmpRef
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.StartPage

class StartPageViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val startPageView: StartPage = app.injector.instanceOf[StartPage]

  override def view: Html = startPageView(EmpRef("", ""))

  "startPage" must {
    behave like pageWithTitle(messages("StartPage.heading"))
    behave like pageWithHeader(messages("StartPage.heading"))
    behave like pageWithLink(messages("StartPage.link"), "/payrollbik/registered-benefits-expenses")
  }

}
