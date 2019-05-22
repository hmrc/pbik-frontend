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

package connectors

import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Mode.Mode
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class ContactFrontendConnectorSpec extends PlaySpec with OneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach with ServicesConfig {

  override protected def mode: Mode = app.injector.instanceOf[Mode]
  override protected def runModeConfiguration: Configuration = app.injector.instanceOf[Configuration]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  object TestConnector extends ContactFrontendConnector(
    app.injector.instanceOf[HttpClient],
    app.injector.instanceOf[Configuration],
    app.injector.instanceOf[Environment],
    app.injector.instanceOf[PBIKHeaderCarrierForPartialsConverter]
  ) {
    val http: HttpGet = mock[HttpGet]
  }

  override def beforeEach(): Unit = {
    reset(TestConnector.http)
  }

  "ContactFrontendConnector" must {

    val dummyResponseHtml = "<div id=\"contact-partial\"></div>"
    lazy val serviceBase = s"${baseUrl("contact-frontend")}/contact"
    lazy val serviceUrl = s"$serviceBase/problem_reports"

    "contact the front end service to download the 'get help' partial" in {

      val response = HttpResponse(200, responseString = Some(dummyResponseHtml))

      when(TestConnector.http.GET[HttpResponse](meq(serviceUrl))(any(), any[HeaderCarrier], any[ExecutionContext])) thenReturn Future.successful(response)

      await(TestConnector.getHelpPartial)

      verify(TestConnector.http).GET(meq(serviceUrl))(any(), any[HeaderCarrier], any[ExecutionContext])
    }

    "return an empty string if a BadGatewayException is encountered" in {

      when(TestConnector.http.GET[HttpResponse](meq(serviceUrl))(any(), any[HeaderCarrier], any[ExecutionContext])) thenReturn
        Future.failed(new BadGatewayException("Phony exception"))

      val result = await(TestConnector.getHelpPartial)

      result mustBe ""
    }
  }

}
