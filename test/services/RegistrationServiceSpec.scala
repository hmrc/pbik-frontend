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

import config._
import connectors.{HmrcTierConnector, TierConnector}
import controllers.FakePBIKApplication
import models._
import org.mockito.Matchers.{eq => mockEq}
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.libs.json
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.TestAuthUser
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec
import utils.TaxDateUtils

import scala.concurrent.Future

class RegistrationServiceSpec extends UnitSpec with TestAuthUser  with Mockito with FakePBIKApplication {

 object MockRegistrationService extends RegistrationService with TierConnector {
   lazy val CYCache: List[Bik] = List.tabulate(5)(n => Bik("" + (n + 1), 10))
   val tierConnector: HmrcTierConnector = mock[HmrcTierConnector]
   override lazy val pbikAppConfig: AppConfig = mock[AppConfig]
   override val bikListService: BikListService = mock[BikListService]

   when(bikListService.pbikHeaders).thenReturn(Map(HeaderTags.ETAG -> "0", HeaderTags.X_TXID -> "1"))

   when(bikListService.registeredBenefitsList(anyInt,any[EmpRef])(anyString)
     (any[HeaderCarrier], any[Request[_]])).thenReturn(Future.successful(CYCache))

   // Return instance where not all Biks have been registered for CY
   when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
     any[EmpRef], mockEq(TaxDateUtils.getCurrentTaxYear()) )(any[HeaderCarrier], any[Request[_]],
       any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
     Integer.parseInt(x.iabdType) <= 3
   }))

   // Return instance where not all Biks have been registered for CYP1
   when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
     any[EmpRef], mockEq(TaxDateUtils.getCurrentTaxYear()+1) )(any[HeaderCarrier], any[Request[_]],
       any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
     Integer.parseInt(x.iabdType) <= 5
   }))

 }

  "When instantiating the RegistrationService the connectors" should {
   " not be null" in {
      running(fakeApplication) {
        val regService:RegistrationService = RegistrationService
        assert(regService.bikListService != null)
      }
    }
  }

  "When instantiating the RegistrationService the config" should {
   " not be null" in {
      running(fakeApplication) {
        val regService:RegistrationService = RegistrationService
        assert(regService.pbikAppConfig != null)
      }
    }
  }

  "When generating a page which allows registrations, the service" should {
   " return the selection page" in {
      running(fakeApplication) {
        implicit val context: PbikContext = PbikContextImpl
        val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
        implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
          EmpRef("taxOfficeNumber", "taxOfficeReference"),
          UserName(Name(None, None)),
          request)
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
        val YEAR_RANGE = TaxDateUtils.getTaxYearRange()
        val regService:RegistrationService = MockRegistrationService
        val result = await(regService.generateViewForBikRegistrationSelection(YEAR_RANGE.cyminus1,
          "add", views.html.registration.nextTaxYear(_, true, YEAR_RANGE, _, _, _, _, _, EmpRef.empty)))
        status(result) shouldBe 200
        bodyOf(result) should include(Messages("AddBenefits.Heading"))
        bodyOf(result) should include(Messages("BenefitInKind.label.37"))
      }
    }
  }

}
