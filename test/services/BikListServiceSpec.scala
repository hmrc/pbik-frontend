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

package services

import connectors.PbikConnector
import controllers.FakePBIKApplication
import controllers.actions.MinimalAuthAction
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import support.TestAuthUser
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestMinimalAuthAction

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class BikListServiceSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with TestAuthUser
    with FakePBIKApplication
    with BeforeAndAfterEach {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[MinimalAuthAction].to(classOf[TestMinimalAuthAction]))
    .overrides(bind[PbikConnector].toInstance(mock(classOf[PbikConnector])))
    .build()

  lazy val bikListService: BikListService                                  = app.injector.instanceOf[BikListService]
  implicit lazy val aRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = createDummyUser(mockRequest)
  implicit val hc: HeaderCarrier                                           = HeaderCarrier()

  val responseHeaders: Map[String, String] = HeaderTags.createResponseHeaders()

  val bikStatus30 = 30
  val bikStatus40 = 40
  val bikEilCount = 10

  override def beforeEach(): Unit =
    reset(bikListService.tierConnector)

  "The BIK service" should {

    "Be able to get the BIKS for the current year - 2 returned" in {
      val listBiks = List(Bik("31", bikStatus30, bikEilCount), Bik("36", bikStatus40, bikEilCount))

      when(
        bikListService.tierConnector.getRegisteredBiks(any[EmpRef], any[Int])(
          any[HeaderCarrier],
          any[AuthenticatedRequest[_]]
        )
      ).thenReturn(Future.successful(BikResponse(responseHeaders, listBiks)))

      val result: BikResponse = Await.result(bikListService.currentYearList, 10 seconds)
      result.bikList shouldBe listBiks
    }

    "Be able to get the BIKS for the current year - no biks returned" in {
      when(
        bikListService.tierConnector.getRegisteredBiks(any[EmpRef], any[Int])(
          any[HeaderCarrier],
          any[AuthenticatedRequest[_]]
        )
      ).thenThrow(new IllegalStateException())
      // Intercept exception
      intercept[IllegalStateException] {
        bikListService.currentYearList
      }
    }

    "Be able to get the BIKS for the next year - 2 returned" in {
      val listBiks = List(Bik("31", bikStatus30, bikEilCount), Bik("36", bikStatus40, bikEilCount))

      when(
        bikListService.tierConnector.getRegisteredBiks(any[EmpRef], any[Int])(
          any[HeaderCarrier],
          any[AuthenticatedRequest[_]]
        )
      ).thenReturn(Future.successful(BikResponse(responseHeaders, listBiks)))
      implicit val hc: HeaderCarrier = HeaderCarrier()

      val result = Await.result(bikListService.nextYearList, 10 seconds)

      result.bikList shouldBe listBiks
    }

    "Be able to get the BIKS for the next year - no biks returned" in {
      when(
        bikListService.tierConnector.getRegisteredBiks(any[EmpRef], any[Int])(
          any[HeaderCarrier],
          any[AuthenticatedRequest[_]]
        )
      ).thenThrow(new IllegalStateException())

      intercept[IllegalStateException] {
        bikListService.nextYearList
      }
    }

  }
}
