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

package controllers.actions

import base.FakePBIKApplication
import config.PbikAppConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status._
import play.api.mvc._
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.MissingBearerToken
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MinimalAuthActionSpec extends FakePBIKApplication {

  private class Harness(
    authAction: MinimalAuthAction,
    cc: ControllerComponents = Helpers.stubMessagesControllerComponents()
  ) extends AbstractController(cc) {
    def onPageLoad(): Action[AnyContent] = authAction {
      Ok
    }
  }

  "Minimal Auth Action" when {
    "the user is logged in" must {
      "return OK" in {
        val mockAuthConnector: AuthConnector = mock(classOf[AuthConnector])

        when(mockAuthConnector.authorise[Unit](any(), any())(any(), any()))
          .thenReturn(Future.successful(()))

        val minimalAuthAction = new MinimalAuthActionImpl(
          authConnector = mockAuthConnector,
          parser = injected[BodyParsers.Default],
          config = injected[PbikAppConfig]
        )
        val controller        = new Harness(minimalAuthAction)
        val result            = controller.onPageLoad()(FakeRequest("", ""))

        status(result) mustBe OK
      }
    }

    "the user is not logged in" must {
      "redirect the user to log in" in {
        val minimalAuthAction = new MinimalAuthActionImpl(
          new BrokenAuthConnector(
            new MissingBearerToken,
            mock(classOf[HttpClientV2]),
            injected[PbikAppConfig]
          ),
          injected[BodyParsers.Default],
          injected[PbikAppConfig]
        )
        val controller        = new Harness(minimalAuthAction)
        val result            = controller.onPageLoad()(FakeRequest("", ""))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must endWith(
          "sign-in?continue=http%3A%2F%2Flocalhost%3A9233%2Fpayrollbik%2Fstart-payrolling-benefits-expenses&origin=pbik-frontend"
        )
      }
    }
  }

}
