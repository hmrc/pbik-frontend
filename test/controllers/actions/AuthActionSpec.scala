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

import akka.util.Timeout
import controllers.ExternalUrls
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import play.api.http.Status._
import play.api.mvc.{Action, AnyContent, Controller}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status}
import uk.gov.hmrc.auth.core.{InsufficientEnrolments, MissingBearerToken}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class AuthActionSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  class Harness(authAction: AuthAction) extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction { request => Ok }
  }

  implicit val timeout:Timeout = 5 seconds

  "Auth Action" when {
    "the user is not logged in" must {
      "redirect the user to log in" in {
        val authAction = new AuthActionImpl(
          new BrokenAuthConnector(new MissingBearerToken,
                                    mock[HttpClient],
                                    app.injector.instanceOf[Configuration],
                                    app.injector.instanceOf[Environment]
                                  ),
          app.injector.instanceOf[ExternalUrls])
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest("", ""))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must endWith("sign-in?continue=http%3A%2F%2Flocalhost%3A9233%2Fpayrollbik%2Fpayrolled-benefits-expenses&origin=pbik-frontend")

      }
    }
    "the user has an Insufficient Enrolments " must {
      "redirect the user to a page to enroll" in {
        val authAction = new AuthActionImpl(
          new BrokenAuthConnector(new InsufficientEnrolments,
                                    mock[HttpClient],
                                    app.injector.instanceOf[Configuration],
                                    app.injector.instanceOf[Environment]
                                  ),
          app.injector.instanceOf[ExternalUrls])
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest("", ""))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.AuthController.notAuthorised().url)

      }
    }
  }
}
