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
import connectors.HmrcTierConnector
import controllers.FakePBIKApplication
import models.{AuthenticatedRequest, EiLPerson, EmpRef, UserName}
import org.mockito.Mockito._
import org.scalatest.Matchers
import play.api.libs.json
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.TestAuthUser
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.test.UnitSpec
import utils.ControllersReferenceData

import scala.collection.immutable

class EilListServiceSpec extends UnitSpec with FakePBIKApplication with Matchers
with TestAuthUser with ControllersReferenceData {

  override lazy val pbikAppConfig: AppConfig = mock[AppConfig]

  when(pbikAppConfig.reportAProblemPartialUrl).thenReturn("")

  val MockEiLListService: EiLListService = running(fakeApplication) {
    new EiLListService {
      val pbikAppConfig: AppConfig = mock[AppConfig]
      val tierConnector: HmrcTierConnector = mock[HmrcTierConnector]
      when(tierConnector.genericGetCall[List[EiLPerson]](anyString,
        anyString,any[EmpRef], anyInt)(any[HeaderCarrier],any[Request[_]],
          any[json.Format[List[EiLPerson]]], any[Manifest[List[EiLPerson]]])).thenReturn(List.empty[EiLPerson])
    }
  }


  "When instantiating the EilListService" in {
    running(fakeApplication) {
      val eilService: EiLListService = EiLListService
      assert(eilService.pbikAppConfig != null)
      assert(eilService.tierConnector != null)
    }
  }

  "When calling the EILService it" should {
    "return an empty list" in {
      running(fakeApplication) {
        val eilService: EiLListService =  MockEiLListService
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
        implicit val authenicatedRequest = AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
        val result = await(eilService.currentYearEiL("5", 2015))
        result.size shouldBe 0
      }
    }

    "return a subset of List(EiL) search results - already excluded" in {
      running(fakeApplication) {
        val eilService =  MockEiLListService
        implicit val ac: AuthContext = createDummyUser("VALID_ID")
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
        val eiL1 = new EiLPerson("QQ123456", "Humpty", None, "Dumpty", Some("123"), Some("01/01/1980"), None, None)
        val eiL2 = new EiLPerson("QQ123457", "Humpty", None, "Dumpty", Some("789"), Some("01/01/1980"), None, None)
        val searchResultsEiL: List[EiLPerson] = List(eiL1,eiL2)
        val alreadyExcludedEiL: List[EiLPerson] = List(eiL1)

        implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
        val result: immutable.Seq[EiLPerson] = eilService.searchResultsRemoveAlreadyExcluded(alreadyExcludedEiL, searchResultsEiL)
        result.size shouldBe 1
        result.head shouldBe eiL2
      }
    }
  }


}
