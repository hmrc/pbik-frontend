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

import base.FakePBIKApplication
import models.auth.AuthenticatedRequest
import models.v1._
import models.v1.exclusion._
import models.v1.trace.{TracePeopleByNinoRequest, TracePeopleByPersonalDetailsRequest, TracePersonListResponse, TracePersonResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, UNPROCESSABLE_ENTITY}
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import utils.Exceptions.GenericServerErrorException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PbikConnectorSpec extends FakePBIKApplication {
  val request: FakeRequest[AnyContentAsEmpty.type]     = mockRequest
  private val mockHttpClient: HttpClientV2             = mock(classOf[HttpClientV2])
  private val mockRequestBuilderGet: RequestBuilder    = mock(classOf[RequestBuilder])
  private val mockRequestBuilderPost: RequestBuilder   = mock(classOf[RequestBuilder])
  private val mockRequestBuilderDelete: RequestBuilder = mock(classOf[RequestBuilder])

  private val connector: PbikConnector                                   = new PbikConnector(mockHttpClient, pbikAppConfig)
  private val fakeResponse: HttpResponse                                 = HttpResponse(OK, "")
  private val employerOptimisticLockResponse                             = 0
  private def benefitListUpdateRequest(request: AuthenticatedRequest[_]) = BenefitListUpdateRequest(
    List(
      BenefitInKindRequest(IabdType.CarBenefit, PbikAction.ReinstatePayrolledBenefitInKind, request.isAgent),
      BenefitInKindRequest(IabdType.VanFuelBenefit, PbikAction.RemovePayrolledBenefitInKind, request.isAgent)
    ),
    EmployerOptimisticLockRequest(3)
  )
  private val pbikErrorResponseCode: String                              = "64990"
  private val year: Int                                                  = 2015
  private val bikEilCount: Int                                           = 10
  private val iabdType                                                   = IabdType.PaymentsOnEmployeeBehalf
  private val benefitTypes                                               = BenefitTypes(Set(IabdType.CarBenefit, IabdType.VanFuelBenefit))
  implicit val hc: HeaderCarrier                                         = HeaderCarrier()
  implicit val authenticatedRequestOrg: AuthenticatedRequest[_]          =
    AuthenticatedRequest[Any](empRef, Some("username"), request, None)
  private val authenticatedRequestAgent: AuthenticatedRequest[_]         =
    AuthenticatedRequest[Any](empRef, Some("username"), request, agentClient)
  private val listBikWithCount                                           = benefitTypes.pbikTypes
    .map(benefitType =>
      BenefitInKindWithCount(
        benefitType,
        bikEilCount
      )
    )
    .toList
  private val pbikExclusions                                             = PbikExclusions(
    10,
    Some(
      List(
        PbikExclusionPerson("AB123456C", "John", Some("Smith"), "Smith", Some("123/AB123456C"), 1),
        PbikExclusionPerson("QQ123456", "Humpty", None, "Dumpty", Some("123"), 2)
      )
    )
  )
  private val tracePersonResponseJohn                                    =
    TracePersonResponse("AB123456C", "John", Some("Smith"), "Smith", Some("123/AB123456C"), 1)
  private val tracePersonListResponse                                    =
    TracePersonListResponse(11, List(tracePersonResponseJohn))

  private val traceByPersonalDetailsRequest = TracePeopleByPersonalDetailsRequest(
    IabdType.Mileage,
    Some(tracePersonResponseJohn.firstForename),
    tracePersonResponseJohn.surname,
    "1980-01-01",
    Gender.Male
  )
  private val traceByNinoRequest            = TracePeopleByNinoRequest(
    IabdType.Mileage,
    tracePersonResponseJohn.firstForename,
    tracePersonResponseJohn.surname,
    tracePersonResponseJohn.nationalInsuranceNumber
  )

  private val updateExclusionPersonForABenefitRequest = UpdateExclusionPersonForABenefitRequest(
    11,
    PbikExclusionPersonAddRequest(iabdType, "AB123456C", "John", "Smith", 92)
  )

  private val pbikExclusionPersonWithBenefitRequest = PbikExclusionPersonWithBenefitRequest(
    pbikExclusions.currentEmployerOptimisticLock,
    pbikExclusions.exclusions.head
  )

  def buildFakeResponseWithBody[A](body: A, status: Int = OK)(implicit w: Writes[A]): HttpResponse =
    HttpResponse(status, Json.toJson(body), Map.empty[String, Seq[String]])

  private def pbikNpsErrorResponse(pbikErrorCode: String = pbikErrorResponseCode) =
    NPSErrors(Seq(NPSError("test reason", pbikErrorCode)))

  private def mockExecute(
    builder: RequestBuilder,
    expectedResponse: Future[HttpResponse]
  ): OngoingStubbing[Future[HttpResponse]] =
    when(builder.execute(any[HttpReads[HttpResponse]], any())).thenReturn(expectedResponse)

  def mockGetEndpoint(expectedResponse: Future[HttpResponse]): OngoingStubbing[Future[HttpResponse]] =
    mockExecute(mockRequestBuilderGet, expectedResponse)

  def mockPostEndpoint(expectedResponse: Future[HttpResponse]): OngoingStubbing[RequestBuilder] = {
    mockExecute(mockRequestBuilderPost, expectedResponse)
    when(mockRequestBuilderPost.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilderPost)
  }

  def mockDeleteEndpoint(expectedResponse: Future[HttpResponse]): OngoingStubbing[RequestBuilder] = {
    mockExecute(mockRequestBuilderDelete, expectedResponse)
    when(mockRequestBuilderDelete.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilderDelete)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockRequestBuilderGet)
    reset(mockRequestBuilderPost)
    reset(mockRequestBuilderDelete)

    when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilderGet)
    when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilderPost)
    when(mockHttpClient.delete(any())(any())).thenReturn(mockRequestBuilderDelete)
  }

  "PbikConnector" when {

    ".getRegisteredBiks" must {

      "return a list of benefits for an organisation on a specific year" in {
        val payload      = BenefitListResponse(Some(listBikWithCount), employerOptimisticLockResponse)
        val fakeResponse =
          buildFakeResponseWithBody(BenefitListResponse(Some(listBikWithCount), employerOptimisticLockResponse))

        mockGetEndpoint(Future.successful(fakeResponse))

        await(connector.getRegisteredBiks(empRef, year)) mustBe payload
      }

      "throw an exception when unexpected status INTERNAL_SERVER_ERROR is returned" in {
        val fakeResponse = buildFakeResponseWithBody("invalid json", INTERNAL_SERVER_ERROR)

        mockGetEndpoint(Future.successful(fakeResponse))

        val result = intercept[GenericServerErrorException] {
          await(
            connector.getRegisteredBiks(empRef, year)
          )
        }

        result.message mustBe s"Failed to get registered benefits for $year, status: $INTERNAL_SERVER_ERROR"
      }

      "throw an exception when unexpected invalid non json body returned" in {
        val fakeResponse = buildFakeResponseWithBody("invalid json")

        mockGetEndpoint(Future.successful(fakeResponse))

        val result = intercept[GenericServerErrorException] {
          await(
            connector.getRegisteredBiks(empRef, year)
          )
        }

        result.message mustBe s"Failed to get registered benefits for $year, status: $OK"
      }

    }

    ".getAllAvailableBiks" must {
      "return a list of all benefits available to register on a specific year" in {
        val fakeResponseWithListOfBiks: HttpResponse = buildFakeResponseWithBody(benefitTypes)

        mockGetEndpoint(Future.successful(fakeResponseWithListOfBiks))

        await(connector.getAllAvailableBiks(year)) mustBe Right(benefitTypes)
      }

      "return a error when body dont match expected object" in {
        val fakeResponseWithListOfBiks: HttpResponse = buildFakeResponseWithBody("Random test string")

        mockGetEndpoint(Future.successful(fakeResponseWithListOfBiks))

        intercept[GenericServerErrorException] {
          await(connector.getAllAvailableBiks(year))
        }
      }

      "return a NPSErrors when BAD_REQUEST" in {
        val npsErrors                                = pbikNpsErrorResponse("xx_test_xx")
        val fakeResponseWithListOfBiks: HttpResponse =
          buildFakeResponseWithBody(npsErrors, BAD_REQUEST)

        mockGetEndpoint(Future.successful(fakeResponseWithListOfBiks))

        await(connector.getAllAvailableBiks(year)) mustBe Left(npsErrors)
      }

      "return an exception when BAD_REQUEST with non NPSError" in {
        val fakeResponseWithListOfBiks: HttpResponse =
          buildFakeResponseWithBody("random test string", BAD_REQUEST)

        mockGetEndpoint(Future.successful(fakeResponseWithListOfBiks))

        intercept[GenericServerErrorException] {
          await(connector.getAllAvailableBiks(year))
        }
      }

      "return an exception when INTERNAL_SERVER_ERROR" in {
        val fakeResponseWithListOfBiks: HttpResponse =
          buildFakeResponseWithBody("random test string", INTERNAL_SERVER_ERROR)

        mockGetEndpoint(Future.successful(fakeResponseWithListOfBiks))

        intercept[GenericServerErrorException] {
          await(connector.getAllAvailableBiks(year))
        }
      }
    }

    ".getAllExcludedEiLPersonForBik" must {
      "return a list of all employees in an organisation for a specific benefit on a specific year" in {
        val fakeResponseWithListOfEiLs = buildFakeResponseWithBody(pbikExclusions)

        mockGetEndpoint(Future.successful(fakeResponseWithListOfEiLs))

        await(
          connector
            .getAllExcludedEiLPersonForBik(iabdType, empRef, year)
        ) mustBe Right(pbikExclusions)
      }

      "throw an exception when an error is received if http response status OK and non json body" in {
        val fakeResponseWithListOfEiLs = buildFakeResponseWithBody("invalid json", OK)

        mockGetEndpoint(Future.successful(fakeResponseWithListOfEiLs))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .getAllExcludedEiLPersonForBik(iabdType, empRef, year)
          )
        }

        result.message mustBe "Failed to get excluded persons, status: " + OK
      }

      "throw an exception when an error is received if http response status UNPROCESSABLE_ENTITY and non json body" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody("invalid json", UNPROCESSABLE_ENTITY)

        mockGetEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .getAllExcludedEiLPersonForBik(iabdType, empRef, year)
          )
        }

        result.message mustBe s"Failed to get excluded persons, status: $UNPROCESSABLE_ENTITY"
      }

      "throw an exception when an error is received if http response status BAD_REQUEST" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody("invalid json", BAD_REQUEST)

        mockGetEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .getAllExcludedEiLPersonForBik(iabdType, empRef, year)
          )
        }

        result.message mustBe s"Failed to get available EilPerson, status: $BAD_REQUEST"
      }

      "return a NPSErrors if http response status UNPROCESSABLE_ENTITY" in {
        val npsErrors                     = pbikNpsErrorResponse()
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(npsErrors, UNPROCESSABLE_ENTITY)

        mockGetEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        await(
          connector
            .getAllExcludedEiLPersonForBik(iabdType, empRef, year)
        ) mustBe Left(npsErrors)
      }
    }

    ".excludeEiLPersonFromBik" must {

      "excludes an individual from for a specific benefit on a specific year and return the status" in {
        val fakeResponseWithListOfEiLs = buildFakeResponseWithBody(pbikExclusions)

        mockPostEndpoint(Future.successful(fakeResponseWithListOfEiLs))

        await(
          connector
            .excludeEiLPersonFromBik(empRef, year, updateExclusionPersonForABenefitRequest)
        ) mustBe Right(OK)
      }

      "throw an exception when an error is received if http response status UNPROCESSABLE_ENTITY and non json body" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody("invalid json", UNPROCESSABLE_ENTITY)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .excludeEiLPersonFromBik(empRef, year, updateExclusionPersonForABenefitRequest)
          )
        }

        result.message mustBe s"Failed to exclude person, status: $UNPROCESSABLE_ENTITY"
      }

      "throw an exception when an error is received if http response status BAD_REQUEST" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody("invalid json", BAD_REQUEST)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .excludeEiLPersonFromBik(empRef, year, updateExclusionPersonForABenefitRequest)
          )
        }

        result.message mustBe s"Failed to exclude person, status: $BAD_REQUEST"
      }

      "return a NPSErrors if http response status UNPROCESSABLE_ENTITY" in {
        val npsErrors                     = pbikNpsErrorResponse()
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(npsErrors, UNPROCESSABLE_ENTITY)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        await(
          connector
            .excludeEiLPersonFromBik(empRef, year, updateExclusionPersonForABenefitRequest)
        ) mustBe Left(npsErrors)
      }

    }

    ".removeEiLPersonExclusionFromBik" must {
      "return OK (200) when an individual is removed from a benefit" in {
        val fakeResponseWithListOfEiLs = buildFakeResponseWithBody(pbikExclusions)

        mockDeleteEndpoint(Future.successful(fakeResponseWithListOfEiLs))

        await(
          connector
            .removeEiLPersonExclusionFromBik(iabdType, empRef, year, pbikExclusionPersonWithBenefitRequest)
        ) mustBe Right(OK)
      }

      "throw an exception when an error is received if http response status UNPROCESSABLE_ENTITY and non json body" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody("invalid json", UNPROCESSABLE_ENTITY)

        mockDeleteEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .removeEiLPersonExclusionFromBik(iabdType, empRef, year, pbikExclusionPersonWithBenefitRequest)
          )
        }

        result.message mustBe s"Failed to remove excluded person from benefit, status: $UNPROCESSABLE_ENTITY"
      }

      "throw an exception when an error is received if http response status BAD_REQUEST" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody("invalid json", BAD_REQUEST)

        mockDeleteEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .removeEiLPersonExclusionFromBik(iabdType, empRef, year, pbikExclusionPersonWithBenefitRequest)
          )
        }

        result.message mustBe s"Failed to remove excluded person from benefit, status: $BAD_REQUEST"
      }

      "return a NPSErrors if http response status UNPROCESSABLE_ENTITY" in {
        val npsErrors                     = pbikNpsErrorResponse()
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(npsErrors, UNPROCESSABLE_ENTITY)

        mockDeleteEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        await(
          connector
            .removeEiLPersonExclusionFromBik(iabdType, empRef, year, pbikExclusionPersonWithBenefitRequest)
        ) mustBe Left(npsErrors)
      }

    }

    ".updateOrganisationsRegisteredBiks" must {
      "return OK when successfully changing an organisations registered benefits" in {
        mockPostEndpoint(Future.successful(fakeResponse))

        await(
          connector
            .updateOrganisationsRegisteredBiks(year, benefitListUpdateRequest(authenticatedRequestOrg))(
              hc,
              authenticatedRequestOrg
            )
        ) mustBe OK
      }

      "return OK when successfully changing a agent registered benefits" in {
        mockPostEndpoint(Future.successful(fakeResponse))

        await(
          connector
            .updateOrganisationsRegisteredBiks(year, benefitListUpdateRequest(authenticatedRequestAgent))(
              hc,
              authenticatedRequestAgent
            )
        ) mustBe OK
      }

      "throw an exception when an error is received if http response status BAD_REQUEST" in {
        mockPostEndpoint(Future.successful(buildFakeResponseWithBody("invalid json", BAD_REQUEST)))
        val payload = benefitListUpdateRequest(authenticatedRequestAgent)

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .updateOrganisationsRegisteredBiks(year, payload)(hc, authenticatedRequestAgent)
          )
        }

        result.message mustBe s"Failed to update benefit list, status: $BAD_REQUEST"
      }
    }

    ".findPersonByPersonalDetails" must {
      "return a list of persons when valid response" in {
        val response = buildFakeResponseWithBody(tracePersonListResponse)

        mockPostEndpoint(Future.successful(response))

        await(
          connector
            .findPersonByPersonalDetails(empRef, year, traceByPersonalDetailsRequest)
        ) mustBe Right(tracePersonListResponse)
      }

      "throw an exception when invalid non json return back" in {
        val response = buildFakeResponseWithBody("invalid json")

        mockPostEndpoint(Future.successful(response))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .findPersonByPersonalDetails(empRef, year, traceByPersonalDetailsRequest)
          )
        }

        result.message mustBe s"Failed to find person by personal OR nino details, status: $OK"
      }

      "throw an exception when an error is received if http response status UNPROCESSABLE_ENTITY and non json body" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody("invalid json", UNPROCESSABLE_ENTITY)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .findPersonByPersonalDetails(empRef, year, traceByPersonalDetailsRequest)
          )
        }

        result.message mustBe s"Failed to find person by personal details OR nino details, status: $UNPROCESSABLE_ENTITY"
      }

      "throw an exception when an error is received if http response status BAD_REQUEST" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody("invalid json", BAD_REQUEST)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .findPersonByPersonalDetails(empRef, year, traceByPersonalDetailsRequest)
          )
        }

        result.message mustBe s"Failed to find person by personal details OR nino details, status: $BAD_REQUEST"
      }

      "return a NPSErrors if http response status UNPROCESSABLE_ENTITY" in {
        val npsErrors                     = pbikNpsErrorResponse()
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(npsErrors, UNPROCESSABLE_ENTITY)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        await(
          connector
            .findPersonByPersonalDetails(empRef, year, traceByPersonalDetailsRequest)
        ) mustBe Left(npsErrors)
      }
    }

    ".findPersonByNino" must {
      "return a list of persons when valid response" in {
        val response = buildFakeResponseWithBody(tracePersonListResponse)

        mockPostEndpoint(Future.successful(response))

        await(
          connector
            .findPersonByNino(empRef, year, traceByNinoRequest)
        ) mustBe Right(tracePersonListResponse)
      }

      "throw an exception when invalid non json return back" in {
        val response = buildFakeResponseWithBody("invalid json")

        mockPostEndpoint(Future.successful(response))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .findPersonByNino(empRef, year, traceByNinoRequest)
          )
        }

        result.message mustBe s"Failed to find person by personal OR nino details, status: $OK"
      }

      "throw an exception when an error is received if http response status UNPROCESSABLE_ENTITY and non json body" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody("invalid json", UNPROCESSABLE_ENTITY)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .findPersonByNino(empRef, year, traceByNinoRequest)
          )
        }

        result.message mustBe s"Failed to find person by personal details OR nino details, status: $UNPROCESSABLE_ENTITY"
      }

      "throw an exception when an error is received if http response status BAD_REQUEST" in {
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody("invalid json", BAD_REQUEST)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        val result = intercept[GenericServerErrorException] {
          await(
            connector
              .findPersonByNino(empRef, year, traceByNinoRequest)
          )
        }

        result.message mustBe s"Failed to find person by personal details OR nino details, status: $BAD_REQUEST"
      }

      "return a NPSErrors if http response status UNPROCESSABLE_ENTITY" in {
        val npsErrors                     = pbikNpsErrorResponse()
        val fakeResponseWithPbikErrorCode = buildFakeResponseWithBody(npsErrors, UNPROCESSABLE_ENTITY)

        mockPostEndpoint(Future.successful(fakeResponseWithPbikErrorCode))

        await(
          connector
            .findPersonByNino(empRef, year, traceByNinoRequest)
        ) mustBe Left(npsErrors)
      }
    }

  }

}
