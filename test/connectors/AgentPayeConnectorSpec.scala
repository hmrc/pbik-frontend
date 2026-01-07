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
import models.PbikSession
import models.agent.Client
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.http.Status.*
import play.api.inject.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, Writes}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.{Application, inject}
import services.SessionService
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentPayeConnectorSpec extends FakePBIKApplication {

  private val mockHttpClient: HttpClientV2          = mock(classOf[HttpClientV2])
  private val mockRequestBuilderGet: RequestBuilder = mock(classOf[RequestBuilder])
  private val mockSessionService: SessionService    = mock(classOf[SessionService])

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(inject.bind[SessionService].toInstance(mockSessionService))
    .build()

  private val configuration: ServicesConfig = injected[ServicesConfig]

  private val agentPayeConnectorWithMockClient: AgentPayeConnector =
    new AgentPayeConnector(mockHttpClient, configuration, mockSessionService)

  private val agentCode: String = "agentCode"
  private val agent             = agentClient.get

  implicit val hc: HeaderCarrier             = HeaderCarrier()
  implicit val crypto: Encrypter & Decrypter = cryptoProvider.getCrypto

  def buildFakeResponseWithBody[A](body: A, status: Int = OK)(implicit w: Writes[A]): HttpResponse =
    HttpResponse(status, Json.toJson(body), Map.empty[String, Seq[String]])

  private def mockExecute(
    builder: RequestBuilder,
    expectedResponse: Future[HttpResponse]
  ) =
    when(builder.execute(using any[HttpReads[HttpResponse]], any())).thenReturn(expectedResponse)

  def mockGetEndpoint(expectedResponse: Future[HttpResponse]) =
    mockExecute(mockRequestBuilderGet, expectedResponse)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient, mockRequestBuilderGet, mockSessionService)

    when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilderGet)
    when(mockRequestBuilderGet.setHeader(any())).thenReturn(mockRequestBuilderGet)
  }

  "AgentPayeConnector" when {
    ".getClient" must {

      "return none if no agent code provided" in {
        await(agentPayeConnectorWithMockClient.getClient(None, empRef)) mustBe None
      }

      "not called if session has cached client" in {
        when(mockSessionService.fetchClientInfo(empRef)).thenReturn(Future.successful(Some(agent)))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe Some(agent)
        verify(mockHttpClient, times(0)).get(any())(any())
      }

      "return none if an exception is received from HttpClient" in {
        when(mockSessionService.fetchClientInfo(empRef)).thenReturn(Future.successful(None))
        mockGetEndpoint(Future.failed(new Exception("test error")))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return none if an invalid json body with OK is received" in {
        when(mockSessionService.fetchClientInfo(empRef)).thenReturn(Future.successful(None))
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("invalid json")
        mockGetEndpoint(Future.successful(fakeResponseWithInvalidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return client if a valid body with OK is received" in {
        when(mockSessionService.fetchClientInfo(empRef)).thenReturn(Future.successful(None))
        val fakeResponseWithValidJson = buildFakeResponseWithBody(agent)
        mockGetEndpoint(Future.successful(fakeResponseWithValidJson))

        when(mockSessionService.storeClientInfo(empRef, agent))
          .thenReturn(Future.successful(PbikSession("sessionId", clientInfo = Map(empRef.value -> agent.encrypt()))))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe Some(agent)
      }

      "return none if ACCEPTED status is received" in {
        when(mockSessionService.fetchClientInfo(empRef)).thenReturn(Future.successful(None))

        val fakeResponse = buildFakeResponseWithBody("", ACCEPTED)
        mockGetEndpoint(Future.successful(fakeResponse))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return none and log a warning if an unexpected status is received" in {
        when(mockSessionService.fetchClientInfo(empRef)).thenReturn(Future.successful(None))

        val fakeResponse = buildFakeResponseWithBody("", INTERNAL_SERVER_ERROR)
        mockGetEndpoint(Future.successful(fakeResponse))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return client from session if already cached" in {
        when(mockSessionService.fetchClientInfo(empRef)).thenReturn(Future.successful(Some(agent)))
        val result = await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef))

        result mustBe Some(agent)
        verify(mockHttpClient, times(0)).get(any())(any())
      }
    }
  }
}
