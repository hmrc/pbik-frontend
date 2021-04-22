/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.HmrcTierConnector
import controllers.FakePBIKApplication
import controllers.actions.MinimalAuthAction
import models._
import org.mockito.ArgumentMatchers.{eq => argEq, any}
import org.mockito.Mockito.{reset, when, _}
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Format
import play.api.mvc.{AnyContentAsEmpty, Request}
import support.{StubBikListService, TestAuthUser}
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import utils.TestMinimalAuthAction

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class BikListServiceSpec
    extends WordSpecLike with Matchers with OptionValues with TestAuthUser with FakePBIKApplication
    with BeforeAndAfterEach {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .overrides(bind[MinimalAuthAction].to(classOf[TestMinimalAuthAction]))
    .overrides(bind[HmrcTierConnector].toInstance(mock(classOf[HmrcTierConnector])))
    .build()

  lazy val bikListService: BikListService = app.injector.instanceOf[StubBikListService]
  implicit lazy val aRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = createDummyUser(mockrequest)
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit =
    reset(bikListService.tierConnector)

  "The BIK service" should {

    "Be able to get the BIKS for the current year - 2 returned" in {
      val listBiks = List(Bik("Car & Car Fuel", 30, 10), Bik("Van Fuel", 40, 10))

      when(
        bikListService.tierConnector.genericGetCall[List[Bik]](any[String], any[String], any[EmpRef], any[Int])(
          any[HeaderCarrier],
          any[Request[_]],
          any[Format[List[Bik]]],
          any[Manifest[List[Bik]]])).thenReturn(Future.successful(listBiks))

      val result: (Map[String, String], List[Bik]) = Await.result(bikListService.currentYearList, 10 seconds)
      result._2 shouldBe listBiks
    }

    "Be able to get the BIKS for the current year - no biks returned" in {
      when(
        bikListService.tierConnector.genericGetCall[List[Bik]](any[String], any[String], any[EmpRef], any[Int])(
          any[HeaderCarrier],
          any[Request[_]],
          any[Format[List[Bik]]],
          any[Manifest[List[Bik]]])).thenThrow(new IllegalStateException())
      // Intercept exception
      intercept[IllegalStateException] {
        bikListService.currentYearList
      }
    }

    "Be able to get the BIKS for the next year - 2 returned" in {
      val listBiks = List(Bik("Car & Car Fuel", 30, 10), Bik("Van Fuel", 40, 10))

      when(
        bikListService.tierConnector.genericGetCall[List[Bik]](any[String], any[String], any[EmpRef], any[Int])(
          any[HeaderCarrier],
          any[Request[_]],
          any[Format[List[Bik]]],
          any[Manifest[List[Bik]]])).thenReturn(Future.successful(listBiks))
      implicit val hc: HeaderCarrier = HeaderCarrier()

      val result = Await.result(bikListService.nextYearList, 10 seconds)

      result._2 shouldBe listBiks
    }

    "Be able to get the BIKS for the next year - no biks returned" in {
      when(
        bikListService.tierConnector.genericGetCall[List[Bik]](any[String], any[String], any[EmpRef], any[Int])(
          any[HeaderCarrier],
          any[Request[_]],
          any[Format[List[Bik]]],
          any[Manifest[List[Bik]]])).thenThrow(new IllegalStateException())

      intercept[IllegalStateException] {
        bikListService.nextYearList
      }
    }

  }
}
