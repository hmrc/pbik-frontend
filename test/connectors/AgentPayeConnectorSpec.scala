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

package connectors

import controllers.FakePBIKApplication
import models._
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status._
import play.api.libs.json.{Json, Writes}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AgentPayeConnectorSpec extends AnyWordSpec with Matchers with FakePBIKApplication {

  private val mockHttpClient: HttpClient                           = mock[HttpClient]
  private val configuration: ServicesConfig                        = app.injector.instanceOf[ServicesConfig]
  private val agentPayeConnectorWithMockClient: AgentPayeConnector =
    new AgentPayeConnector(mockHttpClient, configuration)

  private val baseUrl: String   = s"${configuration.baseUrl("agent-paye")}/agent"
  private val agentCode: String = "agentCode"
  private val empRef: EmpRef    = EmpRef("780", "MODES16")

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def buildFakeResponseWithBody[A](body: A, status: Int = OK)(implicit w: Writes[A]): HttpResponse =
    HttpResponse(status, Json.toJson(body), Map.empty[String, Seq[String]])

  "AgentPayeConnector" when {
    ".getClient" must {
      "return none if no agent code provided" in {
        await(agentPayeConnectorWithMockClient.getClient(None, empRef)) mustBe None
      }

      "return none if an exception is received" in {
        when(
          mockHttpClient.GET(
            eqTo(s"$baseUrl/$agentCode/client/${empRef.encodedEmpRef}"),
            any[Seq[(String, String)]],
            any[Seq[(String, String)]]
          )(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.failed(new Exception("test error")))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return none if an invalid json body with OK is received" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("invalid json")

        when(
          mockHttpClient.GET(
            eqTo(s"$baseUrl/$agentCode/client/${empRef.encodedEmpRef}"),
            any[Seq[(String, String)]],
            any[Seq[(String, String)]]
          )(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponseWithInvalidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return none if an invalid empty body with OK is received" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("")

        when(
          mockHttpClient.GET(
            eqTo(s"$baseUrl/$agentCode/client/${empRef.encodedEmpRef}"),
            any[Seq[(String, String)]],
            any[Seq[(String, String)]]
          )(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponseWithInvalidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return none if an invalid empty body with ACCEPTED is received" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("", ACCEPTED)

        when(
          mockHttpClient.GET(
            eqTo(s"$baseUrl/$agentCode/client/${empRef.encodedEmpRef}"),
            any[Seq[(String, String)]],
            any[Seq[(String, String)]]
          )(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponseWithInvalidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return none if an invalid empty body with NOT_FOUND is received" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("", NOT_FOUND)

        when(
          mockHttpClient.GET(
            eqTo(s"$baseUrl/$agentCode/client/${empRef.encodedEmpRef}"),
            any[Seq[(String, String)]],
            any[Seq[(String, String)]]
          )(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponseWithInvalidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return none if an invalid empty body with INTERNAL_SERVER_ERROR is received" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("", INTERNAL_SERVER_ERROR)

        when(
          mockHttpClient.GET(
            eqTo(s"$baseUrl/$agentCode/client/${empRef.encodedEmpRef}"),
            any[Seq[(String, String)]],
            any[Seq[(String, String)]]
          )(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponseWithInvalidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe None
      }

      "return client if a valid body with OK is received" in {
        val fakeResponseWithValidJson = buildFakeResponseWithBody(agentClient)

        when(
          mockHttpClient.GET(
            eqTo(s"$baseUrl/$agentCode/client/${empRef.encodedEmpRef}"),
            any[Seq[(String, String)]],
            any[Seq[(String, String)]]
          )(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponseWithValidJson))

        await(agentPayeConnectorWithMockClient.getClient(Some(agentCode), empRef)) mustBe agentClient
      }
    }
  }

}
