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

package handlers

import base.FakePBIKApplication
import play.api.i18n.{Messages, MessagesApi}
import views.html.{ErrorTemplate, page_not_found_template}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ErrorHandlerSpec extends FakePBIKApplication {

  private val errorHandler            = injected[ErrorHandler]
  private val errorTemplateView       = injected[ErrorTemplate]
  private val page_not_found_template = injected[page_not_found_template]

  val messages: Messages = injected[MessagesApi].preferred(Seq(lang))

  "ErrorHandler" should {

    "handle notFoundTemplate" in {
      val result = Await.result(errorHandler.notFoundTemplate(mockRequest), Duration.Inf)

      result mustBe page_not_found_template()(mockRequest, messages)
    }

    "handle standardErrorTemplate" in {
      val result =
        Await.result(errorHandler.standardErrorTemplate("title", "heading", "msg test")(mockRequest), Duration.Inf)

      result mustBe errorTemplateView("title", "heading", "msg test")(messages, mockRequest)
    }

  }
}
