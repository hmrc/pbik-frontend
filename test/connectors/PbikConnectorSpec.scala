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

import config.Service
import controllers.FakePBIKApplication
import models._
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsResultException, Json, Writes}
import play.api.mvc.{Request, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import support.TestAuthUser
import uk.gov.hmrc.http._
import utils.Exceptions.GenericServerErrorException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class PbikConnectorSpec extends AnyWordSpec with Matchers with FakePBIKApplication with TestAuthUser with Results {

  private val mockHttpClient: HttpClient                 = mock[HttpClient]
  private val configuration: Configuration               = app.injector.instanceOf[Configuration]
  private val pbikConnectorWithMockClient: PbikConnector = new PbikConnector(mockHttpClient, configuration)

  implicit val hc: HeaderCarrier           = HeaderCarrier()
  implicit val request: Request[List[Bik]] = FakeRequest().asInstanceOf[Request[List[Bik]]]

  private val fakeResponse: HttpResponse           = HttpResponse(OK, "")
  private val pbikErrorResponse: PbikError         = new PbikError("64990")
  private val responseHeaders: Map[String, String] = Map(
    HeaderTags.ETAG   -> "0",
    HeaderTags.X_TXID -> "1"
  )
  private val baseUrl: String                      = s"${configuration.get[Service]("microservice.services.pbik")}/epaye"

  private val year: Int              = 2015
  private val bikStatus30: Int       = 30
  private val bikStatus40: Int       = 40
  private val bikEilCount: Int       = 10
  private val empRef: EmpRef         = EmpRef("780", "MODES16")
  private val (iabdType, iabdString) = ("31", "car")

  private val listBiks: List[Bik] =
    List(Bik(iabdType, bikStatus30, bikEilCount), Bik("36", bikStatus40, bikEilCount))
  private val listOfEiLPerson     = List(
    EiLPerson(
      nino = "AB123456C",
      firstForename = "John",
      secondForename = Some("Smith"),
      surname = "Smith",
      worksPayrollNumber = Some("123/AB123456C"),
      dateOfBirth = None,
      gender = None,
      status = None,
      perOptLock = 1
    ),
    EiLPerson("QQ123456", "Humpty", None, "Dumpty", Some("123"), Some("01/01/1980"), None, None)
  )

  def buildFakeResponseWithBody[A](body: A, status: Int = OK)(implicit w: Writes[A]): HttpResponse =
    HttpResponse(status, Json.toJson(body), Map.empty[String, Seq[String]])

  "PbikConnector" when {
    ".getRegisteredBiks" must {
      "return a list of benefits for an organisation on a specific year" in {
        val fakeResponseWithListOfBiks = buildFakeResponseWithBody(listBiks)

        when(
          mockHttpClient.GET(
            eqTo(s"$baseUrl/${empRef.encodedEmpRef}/$year"),
            any[Seq[(String, String)]],
            any[Seq[(String, String)]]
          )(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponseWithListOfBiks))

        pbikConnectorWithMockClient.getRegisteredBiks(empRef, year).futureValue mustBe BikResponse(
          responseHeaders,
          listBiks
        )
      }

      "throw an exception when a pbik error code is received" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikErrorResponse)

        when(
          mockHttpClient.GET(
            eqTo(s"$baseUrl/${empRef.encodedEmpRef}/$year"),
            any[Seq[(String, String)]],
            any[Seq[(String, String)]]
          )(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient.getRegisteredBiks(empRef, year)
          )
        }

        result.message mustBe pbikErrorResponse.errorCode

      }

      "throw an exception when invalid json is received" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("invalid json")

        when(
          mockHttpClient.GET(
            eqTo(s"$baseUrl/${empRef.encodedEmpRef}/$year"),
            any[Seq[(String, String)]],
            any[Seq[(String, String)]]
          )(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponseWithInvalidJson))

        val result = intercept[JsResultException] {
          await(
            pbikConnectorWithMockClient.getRegisteredBiks(empRef, year)
          )
        }

        result.errors.flatMap(_._2.map(_.message)) mustBe Seq("error.expected.jsarray")

      }
    }

    ".getAllAvailableBiks" must {
      "return a list of all benefits available to register on a specific year" in {
        val fakeResponseWithListOfBiks: HttpResponse = buildFakeResponseWithBody(listBiks)

        when(
          mockHttpClient.GET(
            eqTo(s"$baseUrl/$year/getbenefittypes"),
            any[Seq[(String, String)]],
            any[Seq[(String, String)]]
          )(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponseWithListOfBiks))

        pbikConnectorWithMockClient.getAllAvailableBiks(year).futureValue mustBe listBiks
      }
    }

    ".getAllExcludedEiLPersonForBik" must {
      "return a list of all employees in an organisation for a specific benefit on a specific year" in {
        val fakeResponseWithListOfEiLs = buildFakeResponseWithBody(listOfEiLPerson)

        when(
          mockHttpClient.GET(
            eqTo(s"$baseUrl/${empRef.encodedEmpRef}/$year/$iabdType/exclusion"),
            any[Seq[(String, String)]],
            any[Seq[(String, String)]]
          )(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponseWithListOfEiLs))

        pbikConnectorWithMockClient
          .getAllExcludedEiLPersonForBik(iabdString, empRef, year)
          .futureValue mustBe listOfEiLPerson
      }
    }

    ".excludeEiLPersonFromBik" must {
      "excludes an individual from for a specific benefit on a specific year and return the current list of excluded employees" in {
        val fakeResponseWithListOfEiLs = buildFakeResponseWithBody(listOfEiLPerson)

        when(
          mockHttpClient.POST(
            eqTo(s"$baseUrl/${empRef.encodedEmpRef}/$year/$iabdType/exclusion/update"),
            any[List[Bik]],
            eqTo(responseHeaders.toSeq)
          )(any[Writes[List[Bik]]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponseWithListOfEiLs))

        pbikConnectorWithMockClient
          .excludeEiLPersonFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          .futureValue mustBe EiLResponse(OK, listOfEiLPerson)
      }

      "return no excluded individual matches on the requested benefit" in {
        when(
          mockHttpClient.POST(
            eqTo(s"$baseUrl/${empRef.encodedEmpRef}/$year/$iabdType/exclusion/update"),
            any[List[Bik]],
            eqTo(responseHeaders.toSeq)
          )(any[Writes[List[Bik]]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(buildFakeResponseWithBody(List.empty[EiLPerson], OK)))

        pbikConnectorWithMockClient
          .excludeEiLPersonFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          .futureValue mustBe EiLResponse(OK, List.empty)
      }

      "throw an exception when the response received is BAD_REQUEST (400) or higher" in {
        when(
          mockHttpClient.POST(
            eqTo(s"$baseUrl/${empRef.encodedEmpRef}/$year/$iabdType/exclusion/update"),
            any[List[Bik]],
            eqTo(responseHeaders.toSeq)
          )(any[Writes[List[Bik]]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(buildFakeResponseWithBody(listBiks, BAD_REQUEST)))

        val exception = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .excludeEiLPersonFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        Json.parse(exception.message) mustBe Json.toJson(listBiks)
      }
    }

    ".removeEiLPersonExclusionFromBik" must {
      "return OK (200) when an individual is removed from a benefit" in {

        when(
          mockHttpClient.POST(
            eqTo(s"$baseUrl/${empRef.encodedEmpRef}/$year/$iabdType/exclusion/remove"),
            any[EiLPerson],
            eqTo(responseHeaders.toSeq)
          )(any[Writes[EiLPerson]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponse))

        pbikConnectorWithMockClient
          .removeEiLPersonExclusionFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          .futureValue mustBe OK
      }

      "throw an exception when the response received is BAD_REQUEST (400) or higher" in {
        when(
          mockHttpClient.POST(
            eqTo(s"$baseUrl/${empRef.encodedEmpRef}/$year/$iabdType/exclusion/remove"),
            any[EiLPerson],
            eqTo(responseHeaders.toSeq)
          )(any[Writes[EiLPerson]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(buildFakeResponseWithBody(listOfEiLPerson, BAD_REQUEST)))

        val exception = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .removeEiLPersonExclusionFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        Json.parse(exception.message) mustBe Json.toJson(listOfEiLPerson)
      }
    }

    ".updateOrganisationsRegisteredBiks" must {
      "return OK (200) when successfully changing an organisations registered benefits" in {
        when(
          mockHttpClient.POST(
            eqTo(s"$baseUrl/${empRef.encodedEmpRef}/$year/updatebenefittypes"),
            any[List[Bik]],
            eqTo(responseHeaders.toSeq)
          )(any[Writes[List[Bik]]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(fakeResponse))

        pbikConnectorWithMockClient
          .updateOrganisationsRegisteredBiks(empRef, year, listBiks)
          .futureValue mustBe OK
      }
    }

  }

}
