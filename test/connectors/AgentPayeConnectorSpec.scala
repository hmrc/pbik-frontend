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

package connectors

import base.FakePBIKApplication
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.http.Status._
import play.api.libs.json.{Json, Writes}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentPayeConnectorSpec extends FakePBIKApplication {

  private val mockHttpClient: HttpClientV2          = mock(classOf[HttpClientV2])
  private val mockRequestBuilderGet: RequestBuilder = mock(classOf[RequestBuilder])

  private val configuration: ServicesConfig                        = injected[ServicesConfig]
  private val agentPayeConnectorWithMockClient: AgentPayeConnector =
    new AgentPayeConnector(mockHttpClient, configuration)

  private val agentCode: String = "agentCode"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def buildFakeResponseWithBody[A](body: A, status: Int = OK)(implicit w: Writes[A]): HttpResponse =
    HttpResponse(status, Json.toJson(body), Map.empty[String, Seq[String]])

  private def mockExecute(
    builder: RequestBuilder,
    expectedResponse: Future[HttpResponse]
  ): OngoingStubbing[Future[HttpResponse]] =
    when(builder.execute(using any[HttpReads[HttpResponse]], any())).thenReturn(expectedResponse)

  def mockGetEndpoint(expectedResponse: Future[HttpResponse]): OngoingStubbing[Future[HttpResponse]] =
    mockExecute(mockRequestBuilderGet, expectedResponse)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockHttpClient)
    reset(mockRequestBuilderGet)

    when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilderGet)
    when(mockRequestBuilderGet.setHeader(any())).thenReturn(mockRequestBuilderGet)

  }

  "AgentPayeConnector" when {
    ".getClient" must {
      "return none if no agent code provided" in {
        await(agentPayeConnectorWithMockClient.getClient(None, empRef)) mustBe None
      }

      "return none if an exception is received" in {
        mockGetEndpoint(Future.failed(new Exception("test error")))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return none if an invalid json body with OK is received" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("invalid json")

        mockGetEndpoint(Future.successful(fakeResponseWithInvalidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return none if an invalid empty body with OK is received" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("")

        mockGetEndpoint(Future.successful(fakeResponseWithInvalidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return none if an invalid empty body with ACCEPTED is received" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("", ACCEPTED)

        mockGetEndpoint(Future.successful(fakeResponseWithInvalidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return none if an invalid empty body with NOT_FOUND is received" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("", NOT_FOUND)

        mockGetEndpoint(Future.successful(fakeResponseWithInvalidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return none if an invalid empty body with INTERNAL_SERVER_ERROR is received" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("", INTERNAL_SERVER_ERROR)

        mockGetEndpoint(Future.successful(fakeResponseWithInvalidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return client if a valid body with OK is received" in {
        val fakeResponseWithValidJson = buildFakeResponseWithBody(agentClient)

        mockGetEndpoint(Future.successful(fakeResponseWithValidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe agentClient
      }
    }
  }
}
