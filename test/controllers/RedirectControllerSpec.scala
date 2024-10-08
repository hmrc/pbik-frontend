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
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class RedirectControllerSpec extends FakePBIKApplication {

  private val messagesControllerComponents: MessagesControllerComponents =
    injected[MessagesControllerComponents]
  private val redirectController: RedirectController                     = new RedirectController(messagesControllerComponents)
  private val fakeRequest: FakeRequest[AnyContentAsEmpty.type]           = FakeRequest("GET", "/")

  "RedirectController" when {
    def test(method: String, result: Future[Result]): Unit =
      s"$method" must {
        "return a 303" in {
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.StartPageController.onPageLoad.url)
        }
      }

    val inputArgs = Seq(
      ("redirectIfFromStart", redirectController.redirectIfFromStart()(fakeRequest)),
      ("redirectIfFromOldOverview", redirectController.redirectIfFromOldOverview()(fakeRequest)),
      ("redirectIfFromRoot", redirectController.redirectIfFromRoot()(fakeRequest))
    )

    inputArgs.foreach(args => (test _).tupled(args))
  }
}
