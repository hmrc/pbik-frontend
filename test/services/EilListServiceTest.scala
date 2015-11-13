/*
 * Copyright 2015 HM Revenue & Customs
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

import config.{PbikAppConfig, AppConfig}
import connectors.HmrcTierConnector
import controllers.FakePBIKApplication
import models.EiLPerson
import org.scalatest.Matchers
import org.mockito.Mockito._
import play.api.libs.json
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.EiLListService
import support.TestAuthUser
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import utils.ControllersReferenceData

class EilListServiceTest extends UnitSpec with FakePBIKApplication with Matchers
with TestAuthUser with ControllersReferenceData with WithFakeApplication{

  override lazy val pbikAppConfig = mock[AppConfig]

  val MockEiLListService = running(fakeApplication) {
    new EiLListService {
      def pbikAppConfig = pbikAppConfig
      val tierConnector = mock[HmrcTierConnector]
      when(tierConnector.genericGetCall[List[EiLPerson]](anyString,
        anyString,anyString, anyInt)(any[HeaderCarrier],any[Request[_]],
          any[json.Format[List[EiLPerson]]], any[Manifest[List[EiLPerson]]])).thenReturn(List.empty[EiLPerson])
    }
  }


  "When instantiating the EilListService " in {
    running(fakeApplication) {
      val eilService:EiLListService = EiLListService
      assert(eilService.pbikAppConfig != null)
      assert(eilService.tierConnector != null)
    }
  }

  "When calling the EILService it " should {
    "return an empty list" in {
      running(fakeApplication) {
        val eilService =  MockEiLListService
        implicit val ac: AuthContext = createDummyUser("VALID_ID")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))
        implicit val request = mockrequest
        val result = await(eilService.currentYearEiL("5", 2015))
        result.size shouldBe 0
      }
    }
  }
}
