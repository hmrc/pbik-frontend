/*
 * Copyright 2016 HM Revenue & Customs
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

import config.AppConfig
import connectors.{HmrcTierConnector, TierConnector}
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers.{eq => Meq, _}
import org.mockito.Matchers.any
import org.mockito.Mockito.{when, verify, reset}
import org.specs2.mock.Mockito
import support.TestAuthUser
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, FakeApplication}
import models.{HeaderTags, Bik}
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


import scala.concurrent.Await

class BikListServiceTest extends UnitSpec with TestAuthUser  with Mockito with WithFakeApplication{

  "The BIK service " should {
    val mockTierConnector = mock[HmrcTierConnector]
    val headers = Map(HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "1")

    val bikListService = running(FakeApplication()) {
       new BikListService {
         override lazy val pbikAppConfig = mock[AppConfig]
         pbikHeaders = headers
         override val tierConnector: HmrcTierConnector = mockTierConnector
      }
    }
    val user = createDummyUser("user001")
    val hc = new HeaderCarrier()

    "Be able to get the BIKS for the current year - 2 returned" in {
      val listBiks = List(Bik("Car & Car Fuel", 30, 10), Bik("Van Fuel", 40, 10))

      when(mockTierConnector.genericGetCall[List[Bik]](any, any, any, any)(any, any, any, any)).thenReturn(Future.successful(listBiks))
      val request = FakeRequest()

      val result = Await.result(bikListService.currentYearList(user, hc, request), 10 seconds)

      result shouldBe (null, listBiks)
    }

    "Be able to get the BIKS for the current year - no biks returned" in {
      when(mockTierConnector.genericGetCall[List[Bik]](any, any, any, any)(any, any, any, any)).thenThrow(new IllegalStateException())
      val request = FakeRequest()

      // Intercept exception
      try {
        val result = bikListService.currentYearList(user, hc, request)
      } catch {
        case illegal: IllegalStateException => // Expected, so pass the test
        case e: Exception =>  fail ("Did not match expected exception, was expecting IllegalStateException")
      }

      reset(mockTierConnector) // Reset the mock object so next test can run
    }

    "Be able to get the BIKS for the next year - 2 returned" in {
      val listBiks = List(Bik("Car & Car Fuel", 30, 10), Bik("Van Fuel", 40, 10))

      when(mockTierConnector.genericGetCall[List[Bik]](any, any, any, any)(any, any, any, any)).thenReturn(Future.successful(listBiks))
      val request = FakeRequest()

      val result = Await.result(bikListService.nextYearList(user, hc, request), 10 seconds)

      result shouldBe (null, listBiks)
    }

    "Be able to get the BIKS for the next year - no biks returned" in {
      when(mockTierConnector.genericGetCall[List[Bik]](any, any, any, any)(any, any, any, any)).thenThrow(new IllegalStateException())
      val request = FakeRequest()

      // Intercept exception
      try {
        val result = bikListService.nextYearList(user, hc, request)
      } catch {
        case illegal: IllegalStateException => // Expected, so pass the test
        case e: Exception =>  fail ("Did not match expected exception, was expecting IllegalStateException")
      }

      reset(mockTierConnector) // Reset the mock object so next test can run
    }

  }
}
