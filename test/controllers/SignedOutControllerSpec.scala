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

package controllers

import base.FakePBIKApplication
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.SignedOut

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class SignedOutControllerSpec extends FakePBIKApplication {

  implicit val ec: ExecutionContextExecutor                    = ExecutionContext.global
  private val messagesActionBuilder: MessagesActionBuilder     =
    new DefaultMessagesActionBuilderImpl(stubBodyParser[AnyContent](), stubMessagesApi())
  private val cc: ControllerComponents                         = stubControllerComponents()
  private val mockMCC: MessagesControllerComponents            = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication.injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ec
  )
  private val signedOutView: SignedOut                         = app.injector.instanceOf[SignedOut]
  private val signedOutController                              = new SignedOutController(signedOutView, mockMCC, ec)
  private val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/")

  "SignedOutController" when {
    "keepAlive" must {
      "return NoContent" in {
        status(signedOutController.keepAlive().apply(fakeRequest)) mustBe NO_CONTENT
      }
    }

    "signedOut" must {
      "return OK" in {
        status(signedOutController.signedOut().apply(fakeRequest)) mustBe OK
      }
    }
  }
}
