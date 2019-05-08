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

package controllers.actions

import java.util.UUID

import akka.util.Timeout
import models.AuthenticatedRequest
import org.scalatestplus.play.PlaySpec
import play.api.mvc.{Action, AnyContent, Controller}
import play.api.test.{FakeRequest, ResultExtractors}
import play.api.test.Helpers.{OK, SEE_OTHER, status}
import uk.gov.hmrc.http.SessionKeys
import play.api.http.{HeaderNames, MimeTypes, Status}
import utils.{TestAuthAction, TestMinimalAuthAction}

import scala.concurrent.duration._
import scala.language.postfixOps

class NoSessionCheckActionSpec extends PlaySpec
  with ResultExtractors
  with Status
  with MimeTypes
  with HeaderNames{

  class Harness(noSessionCheckAction: NoSessionCheckAction) extends Controller {
    def onPageLoad(): Action[AnyContent] = (new TestAuthAction andThen noSessionCheckAction) { request:AuthenticatedRequest[_] => Ok }
  }

  implicit val timeout:Timeout = 5 seconds

  "No Session Check Action" when {
    "there is a session" must{
      "show the correct page" in {
        val requestWithSessionID = FakeRequest("", "").withSession(SessionKeys.sessionId -> s"session-${UUID.randomUUID}")
        val controller = new Harness(new NoSessionCheckActionImpl)
        val result = controller.onPageLoad()(requestWithSessionID)
        status(result) mustBe OK
      }
    }

    "the session is not set" must {
      "show the start page " in {
        val requestWithSessionID = FakeRequest("", "").withSession(SessionKeys.sessionId -> s"session-${UUID.randomUUID}")
        val controller = new Harness(new NoSessionCheckActionImpl)
        val result = controller.onPageLoad()(requestWithSessionID)

        status(result) mustBe SEE_OTHER

        val resultHeaders:Map[String, String] = headers(result)
        resultHeaders.getOrElse("Location", "") must include("/payrollbik/payrolled-benefits-expenses")
        resultHeaders.getOrElse("Set-Cookie", "") must include("mdtp=")
        resultHeaders.getOrElse("Set-Cookie", "") must include("Path=/; HTTPOnly")
      }
    }
  }

}
