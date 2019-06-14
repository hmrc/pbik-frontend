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

package views

import models.EmpRef
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.MaintenancePage

class MaintenancePageViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val MaintenancePageView = app.injector.instanceOf[MaintenancePage]
  override def view: Html = MaintenancePageView("", Some(EmpRef("", "")))

  "MaintenancePageView Welsh" must {

    behave like pageWithHeader(messages("ErrorPage.title"))
    behave like pageWithIdAndText(messages("ErrorPage.try.later"), "tryLater")
    behave like pageWithIdAndText(messages("ErrorPage.contact.helpline") + " " + messages("ErrorPage.if.you.need.to.speak.to.someone"), "contactHelpLine")
    behave like pageWithLink(messages("ErrorPage.contact.helpline"), messages("ErrorPage.contact.link"))

  }
}
