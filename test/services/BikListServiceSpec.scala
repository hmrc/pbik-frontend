/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers.FakePBIKApplication
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers.{eq => Meq, _}
import org.mockito.Matchers.any
import org.mockito.Mockito.{mock, reset, verify, when}
import org.specs2.mock.Mockito
import support.TestAuthUser
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.Helpers._
import play.api.test.FakeRequest
import models.{Bik, HeaderTags}
import play.api.mvc.AnyContentAsEmpty

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

class BikListServiceSpec extends UnitSpec with TestAuthUser  with Mockito with FakePBIKApplication {

  "The BIK service " should {
    val mockTierConnector: HmrcTierConnector = mock[HmrcTierConnector]
    val headers: Map[String, String] = Map(HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "1")

    val bikListService = running(fakeApplication) {
       new BikListService {
         override lazy val pbikAppConfig: AppConfig = mock[AppConfig]
         override lazy val pbikHeaders:Map[String,String] = headers
         override val tierConnector: HmrcTierConnector = mockTierConnector
      }
    }
    val user: AuthContext = createDummyUser("user001")
    val hc: HeaderCarrier = HeaderCarrier()

    "Be able to get the BIKS for the current year - 2 returned" in {
      val listBiks = List(Bik("Car & Car Fuel", 30, 10), Bik("Van Fuel", 40, 10))

      when(mockTierConnector.genericGetCall[List[Bik]](any, any, any, any)(any, any, any, any)).thenReturn(Future.successful(listBiks))
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val result: (Map[String, String], List[Bik]) = Await.result(bikListService.currentYearList(user, hc, request), 10 seconds)

      result shouldBe (null, listBiks)
    }

    "Be able to get the BIKS for the current year - no biks returned" in {
      when(mockTierConnector.genericGetCall[List[Bik]](any, any, any, any)(any, any, any, any)).thenThrow(new IllegalStateException())
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      // Intercept exception
      try {
        val result: Future[(Map[String, String], List[Bik])] = bikListService.currentYearList(user, hc, request)
      } catch {
        case illegal: IllegalStateException => illegal.getMessage// Expected, so pass the test
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
