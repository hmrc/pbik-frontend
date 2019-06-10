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

import config.Service
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Mode.Mode
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.{Application, Configuration, Environment}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class ContactFrontendConnectorSpec extends PlaySpec with OneAppPerSuite with MockitoSugar
  with BeforeAndAfterEach {

  override val fakeApplication: Application = GuiceApplicationBuilder()
    .overrides(bind[HttpClient].toInstance(mock[HttpClient]))
    .build()


  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val testConnector = app.injector.instanceOf[ContactFrontendConnector]
  val testConfiguration = app.injector.instanceOf[Configuration]


  override def beforeEach(): Unit = {
    reset(testConnector.client)
  }

  "ContactFrontendConnector" must {

    val dummyResponseHtml = "<div id=\"contact-partial\"></div>"
    lazy val serviceBase = s"${testConfiguration.get[Service]("microservice.services.contact-frontend")}/contact"
    lazy val serviceUrl = s"$serviceBase/problem_reports"

    "contact the front end service to download the 'get help' partial" in {

      val response = HttpResponse(200, responseString = Some(dummyResponseHtml))

      when(testConnector.client.GET[HttpResponse](meq(serviceUrl))(any(), any[HeaderCarrier], any[ExecutionContext])) thenReturn Future.successful(response)

      await(testConnector.getHelpPartial)

      verify(testConnector.client).GET(meq(serviceUrl))(any(), any[HeaderCarrier], any[ExecutionContext])
    }

    "return an empty string if a BadGatewayException is encountered" in {

      when(testConnector.client.GET[HttpResponse](meq(serviceUrl))(any(), any[HeaderCarrier], any[ExecutionContext])) thenReturn
        Future.failed(new BadGatewayException("Phony exception"))

      val result = await(testConnector.getHelpPartial)

      result mustBe ""
    }
  }

}
