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
import models.v1._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json._
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import utils.Exceptions.GenericServerErrorException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PbikConnectorSpec extends PlaySpec with FakePBIKApplication with BeforeAndAfterEach {
  val request: Request[List[Bik]]                    = FakeRequest().asInstanceOf[Request[List[Bik]]]
  private val mockHttpClient: HttpClientV2           = mock(classOf[HttpClientV2])
  private val mockRequestBuilderGet: RequestBuilder  = mock(classOf[RequestBuilder])
  private val mockRequestBuilderPost: RequestBuilder = mock(classOf[RequestBuilder])

  private val configuration: Configuration                               = app.injector.instanceOf[Configuration]
  private val pbikConnectorWithMockClient: PbikConnector                 = new PbikConnector(mockHttpClient, configuration)
  private val fakeResponse: HttpResponse                                 = HttpResponse(OK, "")
  private val employerOptimisticLockResponse                             = EmployerOptimisticLockResponse(0)
  private val fakePostResponseUpdateBenefits: HttpResponse               =
    HttpResponse(OK, Json.toJson(BenefitListUpdateResponse(employerOptimisticLockResponse)).toString())
  private val pbikErrorResponseCode: String                              = "64990"
  private val pbikError: PbikError                                       = PbikError(pbikErrorResponseCode)
  private val responseHeaders: Map[String, String]                       = HeaderTags.createResponseHeaders()
  private val year: Int                                                  = 2015
  private val bikEilCount: Int                                           = 10
  private val (iabdType, iabdString)                                     = (IabdType.CarBenefit.id.toString, "car")
  private val listBiks: List[Bik]                                        =
    List(
      Bik(iabdType, PbikAction.ReinstatePayrolledBenefitInKind.id, bikEilCount),
      Bik(IabdType.VanFuelBenefit.id.toString, PbikAction.RemovePayrolledBenefitInKind.id, bikEilCount)
    )
  implicit val hc: HeaderCarrier                                         = HeaderCarrier()
  implicit val authenticatedRequestOrg: AuthenticatedRequest[List[Bik]]  =
    AuthenticatedRequest[List[Bik]](empRef, username, request, None)
  private val authenticatedRequestAgent: AuthenticatedRequest[List[Bik]] =
    AuthenticatedRequest[List[Bik]](empRef, username, request, agentClient)
  private val listBikWithCount                                           = listBiks.map(bik =>
    BenefitInKindWithCount(
      IabdType(bik.iabdType.toInt),
      PbikStatus.ValidPayrollingBenefitInKind,
      bik.eilCount
    )
  )
  private val listOfEiLPerson                                            = List(
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

  private def pbikNpsErrorResponse(statusCode: Int, pbikErrorCode: String = pbikErrorResponseCode): NPSErrors =
    NPSErrors(Seq(NPSError("reason", s"$statusCode.$pbikErrorCode")))

  private def mockExecute(
    builder: RequestBuilder,
    expectedResponse: Future[HttpResponse]
  ): OngoingStubbing[Future[HttpResponse]] =
    when(builder.execute(any[HttpReads[HttpResponse]], any())).thenReturn(expectedResponse)

  def mockGetEndpoint(expectedResponse: Future[HttpResponse]): OngoingStubbing[Future[HttpResponse]] =
    mockExecute(mockRequestBuilderGet, expectedResponse)

  def mockPostEndpoint(expectedResponse: Future[HttpResponse]): OngoingStubbing[RequestBuilder] = {
    mockExecute(mockRequestBuilderPost, expectedResponse)
    when(mockRequestBuilderPost.withBody(any[JsValue])(any(), any(), any())).thenReturn(mockRequestBuilderPost)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockRequestBuilderGet)
    reset(mockRequestBuilderPost)

    when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilderGet)
    when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilderPost)

    when(mockRequestBuilderGet.setHeader(any())).thenReturn(mockRequestBuilderGet)
    when(mockRequestBuilderPost.setHeader(any())).thenReturn(mockRequestBuilderPost)
  }

  "PbikConnector" when {
    ".getRegisteredBiks" must {
      "return a list of benefits for an organisation on a specific year" in {
        val fakeResponseWithListOfBiks =
          buildFakeResponseWithBody(BenefitListResponse(Some(listBikWithCount), employerOptimisticLockResponse))

        mockGetEndpoint(Future.successful(fakeResponseWithListOfBiks))

        await(pbikConnectorWithMockClient.getRegisteredBiks(empRef, year)) mustBe BikResponse(
          responseHeaders,
          listBiks
        )
      }

      "return an empty list of benefits for an organisation on a specific year" in {
        val fakeResponseWithListOfBiks =
          buildFakeResponseWithBody(BenefitListResponse(None, employerOptimisticLockResponse))

        mockGetEndpoint(Future.successful(fakeResponseWithListOfBiks))

        await(pbikConnectorWithMockClient.getRegisteredBiks(empRef, year)) mustBe BikResponse(
          responseHeaders,
          List()
        )
      }

      "throw an exception when a PbikError is received" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikError)

        mockGetEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient.getRegisteredBiks(empRef, year)
          )
        }

        result.message must include(pbikErrorResponseCode)
      }

      "throw an exception when invalid json is received" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("invalid json")

        mockGetEndpoint(Future.successful(fakeResponseWithInvalidJson))

        val result = intercept[JsResultException] {
          await(
            pbikConnectorWithMockClient.getRegisteredBiks(empRef, year)
          )
        }

        result.errors.flatMap(_._2.map(_.message)) mustBe Seq("error.path.missing")
      }

      "throw an exception when invalid json is received in BAD_REQUEST" in {
        val fakeResponseWithInvalidJson = buildFakeResponseWithBody("invalid json", BAD_REQUEST)

        mockGetEndpoint(Future.successful(fakeResponseWithInvalidJson))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient.getRegisteredBiks(empRef, year)
          )
        }

        result.message mustBe JsString("invalid json").toString()
      }
    }

    ".getAllAvailableBiks" must {
      "return a list of all benefits available to register on a specific year" in {
        val fakeResponseWithListOfBiks: HttpResponse = buildFakeResponseWithBody(listBiks)

        mockGetEndpoint(Future.successful(fakeResponseWithListOfBiks))

        await(pbikConnectorWithMockClient.getAllAvailableBiks(year)) mustBe listBiks
      }
    }

    ".getAllExcludedEiLPersonForBik" must {
      "return a list of all employees in an organisation for a specific benefit on a specific year" in {
        val fakeResponseWithListOfEiLs = buildFakeResponseWithBody(listOfEiLPerson)

        mockGetEndpoint(Future.successful(fakeResponseWithListOfEiLs))

        await(
          pbikConnectorWithMockClient
            .getAllExcludedEiLPersonForBik(iabdString, empRef, year)
        ) mustBe listOfEiLPerson
      }

      "throw an exception when a PbikError is received if http response status OK" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikError, OK)

        mockGetEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .getAllExcludedEiLPersonForBik(iabdString, empRef, year)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a PbikError is received if http response status BAD_REQUEST" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikError, BAD_REQUEST)

        mockGetEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .getAllExcludedEiLPersonForBik(iabdString, empRef, year)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a PbikError is received if http response status INTERNAL_SERVER_ERROR" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikError, INTERNAL_SERVER_ERROR)

        mockGetEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .getAllExcludedEiLPersonForBik(iabdString, empRef, year)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a NPSErrors is received if http response status OK" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikNpsErrorResponse(OK), OK)

        mockGetEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .getAllExcludedEiLPersonForBik(iabdString, empRef, year)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a NPSErrors is received if http response status BAD_REQUEST" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikNpsErrorResponse(BAD_REQUEST), BAD_REQUEST)

        mockGetEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .getAllExcludedEiLPersonForBik(iabdString, empRef, year)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a NPSErrors is received if http response status INTERNAL_SERVER_ERROR" in {
        val fakeResponseWithPbikErrorCode =
          buildFakeResponseWithBody(pbikNpsErrorResponse(INTERNAL_SERVER_ERROR), INTERNAL_SERVER_ERROR)

        mockGetEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .getAllExcludedEiLPersonForBik(iabdString, empRef, year)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when 2 PbikErrors are received if http response status OK" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(Seq(pbikError), OK)

        mockGetEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .getAllExcludedEiLPersonForBik(iabdString, empRef, year)
          )
        }

        result.message mustBe Json.prettyPrint(Json.toJson(Seq(pbikError)))
      }

      "throw an exception when a NPSError is received if http response status OK" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikNpsErrorResponse(OK).failures.head, OK)

        mockGetEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .getAllExcludedEiLPersonForBik(iabdString, empRef, year)
          )
        }

        result.message mustBe Json.prettyPrint(Json.toJson(pbikNpsErrorResponse(OK).failures.head))
      }
    }

    ".excludeEiLPersonFromBik" must {
      "excludes an individual from for a specific benefit on a specific year and return the current list of excluded employees" in {
        val fakeResponseWithListOfEiLs = buildFakeResponseWithBody(listOfEiLPerson)

        mockPostEndpoint(Future.successful(fakeResponseWithListOfEiLs))

        await(
          pbikConnectorWithMockClient
            .excludeEiLPersonFromBik(iabdString, empRef, year, listOfEiLPerson.head)
        ) mustBe EiLResponse(OK, listOfEiLPerson)
      }

      "return no excluded individual matches on the requested benefit" in {
        val fakeResponseWithListOfEiLs = buildFakeResponseWithBody(List.empty[EiLPerson], OK)

        mockPostEndpoint(Future.successful(fakeResponseWithListOfEiLs))

        await(
          pbikConnectorWithMockClient
            .excludeEiLPersonFromBik(iabdString, empRef, year, listOfEiLPerson.head)
        ) mustBe EiLResponse(OK, List.empty)
      }

      "throw an exception when a PbikError is received if http response status OK" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikError, OK)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .excludeEiLPersonFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a PbikError is received if http response status BAD_REQUEST" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikError, BAD_REQUEST)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .excludeEiLPersonFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a PbikError is received if http response status INTERNAL_SERVER_ERROR" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikError, INTERNAL_SERVER_ERROR)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .excludeEiLPersonFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a NPSErrors is received if http response status OK" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikNpsErrorResponse(OK), OK)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .excludeEiLPersonFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a NPSErrors is received if http response status BAD_REQUEST" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikNpsErrorResponse(BAD_REQUEST), BAD_REQUEST)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .excludeEiLPersonFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a NPSErrors is received if http response status INTERNAL_SERVER_ERROR" in {
        val fakeResponseWithPbikErrorCode =
          buildFakeResponseWithBody(pbikNpsErrorResponse(INTERNAL_SERVER_ERROR), INTERNAL_SERVER_ERROR)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .excludeEiLPersonFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }
    }

    ".removeEiLPersonExclusionFromBik" must {
      "return OK (200) when an individual is removed from a benefit" in {
        mockPostEndpoint(Future.successful(fakeResponse))

        await(
          pbikConnectorWithMockClient
            .removeEiLPersonExclusionFromBik(iabdString, empRef, year, listOfEiLPerson.head)
        ) mustBe OK
      }

      "throw an exception when a PbikError is received if http response status OK" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikError, OK)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .removeEiLPersonExclusionFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a PbikError is received if http response status BAD_REQUEST" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikError, BAD_REQUEST)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .removeEiLPersonExclusionFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a PbikError is received if http response status INTERNAL_SERVER_ERROR" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikError, INTERNAL_SERVER_ERROR)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .removeEiLPersonExclusionFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a NPSErrors is received if http response status OK" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikNpsErrorResponse(OK), OK)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .removeEiLPersonExclusionFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a NPSErrors is received if http response status BAD_REQUEST" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(pbikNpsErrorResponse(BAD_REQUEST), BAD_REQUEST)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .removeEiLPersonExclusionFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }

      "throw an exception when a NPSErrors is received if http response status INTERNAL_SERVER_ERROR" in {
        val fakeResponseWithPbikErrorCode =
          buildFakeResponseWithBody(pbikNpsErrorResponse(INTERNAL_SERVER_ERROR), INTERNAL_SERVER_ERROR)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            pbikConnectorWithMockClient
              .removeEiLPersonExclusionFromBik(iabdString, empRef, year, listOfEiLPerson.head)
          )
        }

        result.message mustBe pbikErrorResponseCode
      }
    }

    ".updateOrganisationsRegisteredBiks" must {
      "return OK (200) when successfully changing an organisations registered benefits" in {
        mockPostEndpoint(Future.successful(fakePostResponseUpdateBenefits))

        await(
          pbikConnectorWithMockClient
            .updateOrganisationsRegisteredBiks(year, listBiks)(hc, authenticatedRequestOrg)
        ) mustBe employerOptimisticLockResponse.currentOptimisticLock
      }

      "return OK (200) when successfully changing a agent registered benefits" in {
        mockPostEndpoint(Future.successful(fakePostResponseUpdateBenefits))

        await(
          pbikConnectorWithMockClient
            .updateOrganisationsRegisteredBiks(year, listBiks)(hc, authenticatedRequestAgent)
        ) mustBe employerOptimisticLockResponse.currentOptimisticLock
      }
    }
  }
}
