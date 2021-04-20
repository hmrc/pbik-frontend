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
import models.{AuthenticatedRequest, EiLPerson, EmpRef, UserName}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import support.TestAuthUser
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import scala.collection.immutable
import scala.concurrent.Future

class EilListServiceSpec
    extends WordSpecLike with Matchers with OptionValues with FakePBIKApplication with TestAuthUser {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .overrides(bind[HmrcTierConnector].toInstance(mock(classOf[HmrcTierConnector])))
    .build()

  val MockEiLListService: EiLListService = {

    val els = app.injector.instanceOf[EiLListService]

    when(
      els.tierConnector.genericGetCall[List[EiLPerson]](any[String], any[String], any[EmpRef], any[Int])(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[EiLPerson]]],
        any[Manifest[List[EiLPerson]]])).thenReturn(Future.successful(List.empty[EiLPerson]))

    els
  }

  "When calling the EILService it" should {
    "return an empty list" in {
      val eilService: EiLListService = MockEiLListService
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenicatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      val result = await(eilService.currentYearEiL("5", 2015))
      result.size shouldBe 0
    }

    "return a subset of List(EiL) search results - already excluded" in {
      val eilService = MockEiLListService
      implicit val aRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = createDummyUser(mockrequest)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
      val eiL1 = new EiLPerson("QQ123456", "Humpty", None, "Dumpty", Some("123"), Some("01/01/1980"), None, None)
      val eiL2 = new EiLPerson("QQ123457", "Humpty", None, "Dumpty", Some("789"), Some("01/01/1980"), None, None)
      val searchResultsEiL: List[EiLPerson] = List(eiL1, eiL2)
      val alreadyExcludedEiL: List[EiLPerson] = List(eiL1)

      val result: immutable.Seq[EiLPerson] =
        eilService.searchResultsRemoveAlreadyExcluded(alreadyExcludedEiL, searchResultsEiL)
      result.size shouldBe 1
      result.head shouldBe eiL2
    }
  }

}
