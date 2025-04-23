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
import models.auth.AuthenticatedRequest
import models.v1.exclusion.PbikExclusions
import models.v1.trace.TracePersonResponse
import models.v1.{IabdType, NPSError, NPSErrors}
import org.mockito.ArgumentMatchers.{any, anyInt}
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.Exceptions.GenericServerErrorException

import scala.concurrent.Future

class ExclusionServiceSpec extends FakePBIKApplication {

  private val mockConnector = mock(classOf[PbikConnector])

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[PbikConnector].toInstance(mockConnector))
    .build()

  val exclusionService: ExclusionService = injected[ExclusionService]

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockConnector)

    when(mockConnector.getAllExcludedEiLPersonForBik(any(), any(), anyInt())(any()))
      .thenReturn(Future.successful(Right(PbikExclusions(0, None))))
  }

  "When calling the EILService it" should {
    "return an empty list" in {
      when(mockConnector.getAllExcludedEiLPersonForBik(any(), any(), anyInt())(any()))
        .thenReturn(Future.successful(Right(PbikExclusions(0, None))))

      val year                                                                        = 2015
      val eilService: ExclusionService                                                = exclusionService
      implicit val hc: HeaderCarrier                                                  = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
        createAuthenticatedRequest(mockRequest)

      val result = await(eilService.exclusionListForYear(IabdType.Mileage, year, authenticatedRequest.empRef))

      result.getPBIKExclusionList.size mustBe 0
    }

    "return a subset of List(EiL) search results - already excluded" in {
      when(mockConnector.getAllExcludedEiLPersonForBik(any(), any(), anyInt())(any()))
        .thenReturn(Future.successful(Right(PbikExclusions(0, None))))

      val eilService         = exclusionService
      val exclusionPerson1   = TracePersonResponse("QQ123456", "Humpty", None, "Dumpty", Some("123"), 22)
      val exclusionPerson2   = TracePersonResponse("QQ123456", "Humpty", None, "Dumpty", Some("789"), 22)
      val searchResultsEiL   = List(exclusionPerson1, exclusionPerson2)
      val alreadyExcludedEiL = List(exclusionPerson1)

      val result = eilService.searchResultsRemoveAlreadyExcluded(alreadyExcludedEiL, searchResultsEiL)

      result mustBe List(exclusionPerson2)
    }

    "throw exception when NPSError" in {
      when(mockConnector.getAllExcludedEiLPersonForBik(any(), any(), anyInt())(any()))
        .thenReturn(Future.successful(Left(NPSErrors(Seq(NPSError("test error", "test error 2"))))))

      val year                                                                        = 2015
      val eilService: ExclusionService                                                = exclusionService
      implicit val hc: HeaderCarrier                                                  = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
        createAuthenticatedRequest(mockRequest)

      val result = intercept[GenericServerErrorException] {
        await(eilService.exclusionListForYear(IabdType.Mileage, year, authenticatedRequest.empRef))
      }

      result.message mustBe s"Error getting pbik exclusions for ${IabdType.Mileage.toString} and $year"
    }
  }

}
