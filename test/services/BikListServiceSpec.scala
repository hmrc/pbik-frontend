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

package services

import base.FakePBIKApplication
import connectors.PbikConnector
import controllers.actions.MinimalAuthAction
import models.auth.AuthenticatedRequest
import models.v1._
import org.mockito.ArgumentMatchers.{any, anyInt}
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.Exceptions.GenericServerErrorException
import utils.{ControllersReferenceData, TestMinimalAuthAction}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class BikListServiceSpec extends FakePBIKApplication {

  private val mockConnector = mock(classOf[PbikConnector])

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[MinimalAuthAction].to(classOf[TestMinimalAuthAction]))
    .overrides(bind[PbikConnector].toInstance(mockConnector))
    .build()

  lazy val bikListService: BikListService                                  = injected[BikListService]
  lazy val controllersReferenceData: ControllersReferenceData              = injected[ControllersReferenceData]
  implicit lazy val aRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = createAuthenticatedRequest(mockRequest)
  implicit val hc: HeaderCarrier                                           = HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  val bikEilCount = 10

  private val employerOptimisticLockResponse = 0
  private val benefitTypes                   = BenefitTypes(Set(IabdType.CarBenefit, IabdType.VanFuelBenefit))
  private val listBikWithCount               = benefitTypes.pbikTypes
    .map(benefitType =>
      BenefitInKindWithCount(
        benefitType,
        bikEilCount
      )
    )
    .toList
  private val benefitListResponse            = BenefitListResponse(Some(listBikWithCount), employerOptimisticLockResponse)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockConnector)
  }

  "BikListService" when {

    ".getRegisteredBenefitsForYear" should {

      "return a BenefitListResponse for the current year" in {
        when(mockConnector.getRegisteredBiks(any(), anyInt())(any())).thenReturn(Future.successful(benefitListResponse))

        val result = Await.result(
          bikListService.getRegisteredBenefitsForYear(controllersReferenceData.yearRange.cyminus1),
          5.seconds
        )

        result mustBe benefitListResponse
      }

      "return a BenefitListResponse for the next year" in {
        when(mockConnector.getRegisteredBiks(any(), anyInt())(any())).thenReturn(Future.successful(benefitListResponse))

        val result = Await.result(
          bikListService.getRegisteredBenefitsForYear(controllersReferenceData.yearRange.cy),
          5.seconds
        )

        result mustBe benefitListResponse
      }

      "return a BenefitListResponse for the next year + 1" in {
        when(mockConnector.getRegisteredBiks(any(), anyInt())(any())).thenReturn(Future.successful(benefitListResponse))

        val exception = intercept[GenericServerErrorException] {
          Await.result(
            bikListService.getRegisteredBenefitsForYear(controllersReferenceData.yearRange.cyplus1),
            5.seconds
          )
        }

        exception.message mustBe s"Invalid year to store registered benefits ${controllersReferenceData.yearRange.cyplus1}"
      }

    }

    ".currentYearList" should {

      "return a BenefitListResponse for the current year" in {
        when(mockConnector.getRegisteredBiks(any(), anyInt())(any())).thenReturn(Future.successful(benefitListResponse))

        val result = Await.result(bikListService.currentYearList, 5.seconds)

        result mustBe benefitListResponse
      }

    }

    ".nextYearList" should {

      "return a BenefitListResponse for the next year" in {
        when(mockConnector.getRegisteredBiks(any(), anyInt())(any())).thenReturn(Future.successful(benefitListResponse))

        val result = Await.result(bikListService.nextYearList, 5.seconds)

        result mustBe benefitListResponse
      }

    }

    ".getAllBenefitsForYear" should {

      "return a set of IabdType for the given year" in {
        when(mockConnector.getAllAvailableBiks(anyInt())(any())).thenReturn(Future.successful(Right(benefitTypes)))

        val result =
          Await.result(bikListService.getAllBenefitsForYear(controllersReferenceData.yearRange.cyminus1), 5.seconds)

        result mustBe benefitTypes.pbikTypes
      }

      "throw a GenericServerErrorException if an error occurs" in {
        val error = NPSErrors(List(NPSError("500", "Internal Server Error")))
        when(mockConnector.getAllAvailableBiks(anyInt())(any())).thenReturn(Future.successful(Left(error)))

        val exception = intercept[GenericServerErrorException] {
          Await.result(bikListService.getAllBenefitsForYear(controllersReferenceData.yearRange.cyminus1), 5.seconds)
        }

        exception.message mustBe error.toString
      }

    }

  }
}
