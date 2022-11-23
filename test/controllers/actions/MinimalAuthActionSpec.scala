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

package controllers.actions

import akka.util.Timeout
import config.AppConfig
import org.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status._
import play.api.mvc.{AbstractController, Action, AnyContent, BodyParsers, ControllerComponents}
import play.api.test.Helpers.{redirectLocation, status}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.MissingBearerToken
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class MinimalAuthActionSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  class Harness(authAction: MinimalAuthAction, cc: ControllerComponents = Helpers.stubMessagesControllerComponents())
      extends AbstractController(cc) {
    def onPageLoad(): Action[AnyContent] = authAction {
      Ok
    }
  }

  implicit val timeout: Timeout = 5 seconds

  "Minimal Auth Action" when {
    "the user is not logged in" must {
      "redirect the user to log in" in {
        val minimalAuthAction = new MinimalAuthActionImpl(
          new BrokenAuthConnector(new MissingBearerToken, mock[HttpClient], app.injector.instanceOf[Configuration]),
          app.injector.instanceOf[BodyParsers.Default],
          app.injector.instanceOf[AppConfig]
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

class BrokenAuthConnector @Inject() (exception: Throwable, httpClient: HttpClient, configuration: Configuration)
    extends AuthConnector(httpClient, configuration) {
  override val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] =
    Future.failed(exception)
}
