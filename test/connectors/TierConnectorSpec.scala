/*
 * Copyright 2023 HM Revenue & Customs
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
import models.{Bik, EmpRef, HeaderTags, PbikError}
import org.apache.http.HttpStatus
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsArray, JsResultException, Json, Writes}
import play.api.mvc.{Request, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import support.TestAuthUser
import uk.gov.hmrc.http._
import utils.Exceptions.GenericServerErrorException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class TierConnectorSpec extends PlaySpec with FakePBIKApplication with TestAuthUser with Results {
  implicit val hc: HeaderCarrier           = HeaderCarrier()
  implicit val request: Request[List[Bik]] = FakeRequest().asInstanceOf[Request[List[Bik]]]

  private val fakeResponse: HttpResponse       = HttpResponse(OK, "")
  private val pbikErrorResp: PbikError         = new PbikError("64990")
  private val fakeSevereResponse: HttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, "A severe server error")
  private val hmrcTierConnector                = app.injector.instanceOf[HmrcTierConnector]
  private val mockHttpClient: HttpClient       = mock[HttpClient]
  private val hmrcTierConnectorWithMockClient  = new HmrcTierConnector(mockHttpClient)
  private val year                             = 2015
  private val bikStatus30                      = 30
  private val bikStatus40                      = 40
  private val bikEilCount                      = 10
  private val listBiks                         =
    List(Bik("Car & Car Fuel", bikStatus30, bikEilCount), Bik("Van Fuel", bikStatus40, bikEilCount))

  def buildFakeResponseWithBody[A](body: A)(implicit w: Writes[A]): HttpResponse =
    HttpResponse(OK, Json.toJson(body), Map.empty[String, Seq[String]])

  "When creating a GET URL with an orgainsation needing encoding it" should {
    " encode the slash properly" in {

      val result: String =
        hmrcTierConnector.createGetUrl("theBaseUrl", "theURIExtension", EmpRef("780", "MODES16"), year)
      assert(result == "theBaseUrl/780%2FMODES16/2015/theURIExtension")
    }
  }

  "When creating a GET URL with no organisation it" should {
    " omit the organisation" in {
      val result: String = hmrcTierConnector.createGetUrl("theBaseUrl", "theURIExtension", EmpRef.empty, year)
      assert(result == "theBaseUrl/2015/theURIExtension")
    }
  }

  "When creating a POST URL with an organisation which needs encoding it" should {
    " be properly formed with the %2F encoding" in {
      val result: String =
        hmrcTierConnector.createPostUrl("theBaseUrl", "theURIExtension", EmpRef("780", "MODES16"), year)
      assert(result == "theBaseUrl/780%2FMODES16/2015/theURIExtension")
    }
  }

  "When processing a response if the status is greater than 400 it" should {
    " throw a GenericServerErrorException" in {
      intercept[GenericServerErrorException] {
        hmrcTierConnector.processResponse(fakeSevereResponse)
      }
    }
  }

  "When processing a response if the status is less than 400 it" should {
    " return the response" in {
      val resp = hmrcTierConnector.processResponse(fakeResponse)
      assert(resp.status == OK)
    }
  }

  "When processing a response if there is a PBIK error code" should {
    " throw a GenericServerErrorException" in {
      val fakeResponseWithError = buildFakeResponseWithBody(pbikErrorResp)
      intercept[GenericServerErrorException] {
        hmrcTierConnector.processResponse(fakeResponseWithError)
      }
    }
  }

  "When processing genericPostCall" should {
    "return valid list of PBIKs in the response for a valid request" in {
      val url: String                = hmrcTierConnector.createPostUrl("theBaseUrl", "theURIExtension", EmpRef("780", "MODES16"), year)
      val fakeResponseWithListOfBiks = buildFakeResponseWithBody(listBiks)

      // first setup a mock api call to return a stubbed response
      when(
        mockHttpClient.POST(ArgumentMatchers.eq(url), any[List[Bik]], any[Seq[(String, String)]])(
          any[Writes[List[Bik]]],
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      )
        .thenReturn(Future.successful(fakeResponseWithListOfBiks))

      val result =
        await(
          hmrcTierConnectorWithMockClient.genericPostCall[List[Bik]](
            "theBaseUrl",
            "theURIExtension",
            EmpRef("780", "MODES16"),
            year,
            listBiks
          )
        )

      assert(Json.parse(result.body).as[JsArray].value.size == 2)
      assert(result.status == HttpStatus.SC_OK)
      assert(hmrcTierConnectorWithMockClient.pbikHeaders == Map.empty)
    }
  }

  "When processing GenericGetCall" should {
    "return PBIK error" in {
      val url: String           = hmrcTierConnector.createGetUrl("theBaseUrl", "theURIExtension", EmpRef("780", "MODES16"), year)
      val fakeResponseWithError = buildFakeResponseWithBody(pbikErrorResp)

      // first setup a mock api call to return a stubbed response
      when(
        mockHttpClient.GET(ArgumentMatchers.eq(url), any[Seq[(String, String)]], any[Seq[(String, String)]])(
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      )
        .thenReturn(Future.successful(fakeResponseWithError))

      val result = intercept[GenericServerErrorException] {
        await(
          hmrcTierConnectorWithMockClient.genericGetCall[List[Bik]](
            "theBaseUrl",
            "theURIExtension",
            EmpRef("780", "MODES16"),
            year
          )
        )
      }

      assert(result.message == pbikErrorResp.errorCode)

      assert(
        hmrcTierConnectorWithMockClient.pbikHeaders == Map(
          HeaderTags.ETAG   -> "0",
          HeaderTags.X_TXID -> "1"
        )
      )
    }

    "return HttpResponse with String body" in {
      val url: String           = hmrcTierConnector.createGetUrl("theBaseUrl", "theURIExtension", EmpRef("780", "MODES16"), year)
      val fakeResponseWithError = buildFakeResponseWithBody("Test")

      // first setup a mock api call to return a stubbed response
      when(
        mockHttpClient.GET(ArgumentMatchers.eq(url), any[Seq[(String, String)]], any[Seq[(String, String)]])(
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      )
        .thenReturn(Future.successful(fakeResponseWithError))

      val result = intercept[JsResultException] {
        await(
          hmrcTierConnectorWithMockClient.genericGetCall[List[Bik]](
            "theBaseUrl",
            "theURIExtension",
            EmpRef("780", "MODES16"),
            year
          )
        )
      }

      assert(result.errors.flatMap(_._2.map(_.message)) == Seq("error.expected.jsarray"))

      assert(
        hmrcTierConnectorWithMockClient.pbikHeaders == Map(
          HeaderTags.ETAG   -> "0",
          HeaderTags.X_TXID -> "1"
        )
      )
    }

    "return HttpResponse with a list of Biks in the body" in {
      val url: String                = hmrcTierConnector.createGetUrl("theBaseUrl", "theURIExtension", EmpRef("780", "MODES16"), year)
      val fakeResponseWithListOfBiks = buildFakeResponseWithBody(listBiks)

      // first setup a mock api call to return a stubbed response
      when(
        mockHttpClient.GET(ArgumentMatchers.eq(url), any[Seq[(String, String)]], any[Seq[(String, String)]])(
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      )
        .thenReturn(Future.successful(fakeResponseWithListOfBiks))

      val result =
        await(
          hmrcTierConnectorWithMockClient.genericGetCall[List[Bik]](
            "theBaseUrl",
            "theURIExtension",
            EmpRef("780", "MODES16"),
            year
          )
        )

      assert(result == listBiks)

      assert(
        hmrcTierConnectorWithMockClient.pbikHeaders == Map(
          HeaderTags.ETAG   -> "0",
          HeaderTags.X_TXID -> "1"
        )
      )
    }

    "return HttpResponse with empty list of Biks in the body" in {
      val url: String               = hmrcTierConnector.createGetUrl("theBaseUrl", "theURIExtension", EmpRef("780", "MODES16"), year)
      val fakeResponseWithEmptyBiks = buildFakeResponseWithBody(List.empty[Bik])

      // first setup a mock api call to return a stubbed response
      when(
        mockHttpClient.GET(ArgumentMatchers.eq(url), any[Seq[(String, String)]], any[Seq[(String, String)]])(
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      )
        .thenReturn(Future.successful(fakeResponseWithEmptyBiks))

      val result =
        await(
          hmrcTierConnectorWithMockClient.genericGetCall[List[Bik]](
            "theBaseUrl",
            "theURIExtension",
            EmpRef("780", "MODES16"),
            year
          )
        )

      assert(result == Seq.empty)

      assert(
        hmrcTierConnectorWithMockClient.pbikHeaders == Map(
          HeaderTags.ETAG   -> "0",
          HeaderTags.X_TXID -> "1"
        )
      )
    }
  }

}
