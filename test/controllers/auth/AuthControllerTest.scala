/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.auth

import controllers.FakePBIKApplication
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Action, Results}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import org.scalatest.Matchers
import org.specs2.mock.Mockito
import play.api.test.Helpers._
import support.TestAuthUser
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.{TaxRegime}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.test.UnitSpec

class AuthControllerTest extends UnitSpec with FakePBIKApplication with Matchers
                          with TestAuthUser with Results {


  "When initialising the connectors " should {
    " not be null " in {
      running(fakeApplication) {
        new {
          val traitname = "auth-connector"
        } with AuthenticationConnector {
          assert(auditConnector != null)
        }
      }
    }
  }

  "When initialising the AuthController's config " should {
    " not be null " in {
      running(fakeApplication) {
        assert(AuthController.pbikAppConfig != null)
      }
    }
  }

  class StubRegime extends TaxRegime {

    override def isAuthorised(accounts: Accounts):Boolean = true
    override val unauthorisedLandingPage = Some(routes.AuthController.notAuthorised().url)
    override val authenticationType = PBIKGovernmentGateway
  }

  class PbikActionTestController extends PbikActions with EpayeUser with Mockito {
    override lazy val authConnector = mock[AuthConnector]
    override val getAuthorisedForPolicy = new StubRegime

  }

  "PbikActions " should {
    "show Unauthorised if the session is not authenticated" in {
      running(fakeApplication) {
        val controller = new PbikActionTestController()
        val ac: AuthContext = createDummyUser("VALID_ID")
        val result = await(controller.noSessionCheck(implicit ac => implicit request => Ok("Passed Test"))(ac, mockrequest))
        status(result) shouldBe 200
        bodyOf(result) should include("Passed Test")
      }
    }
  }


//  "PbikActions Authentocation " should {
//    "not mutate the action body when successfully authenticationg" in {
//      running(fakeApplication) {
//        val controller = new PbikActionTestController()
//        val user:User = createDummyUser("VALID_ID")
//        val result:Action[AnyContent] = await(controller.AuthorisedForPbik(implicit user => implicit request => Ok("Passed Test")))
//        val r = await( result.apply(mockrequest) )
//      }
//    }
//  }

  "PbikActions " should {
    "show the start page if the session is not set" in {
      running(fakeApplication) {
        val controller = new PbikActionTestController()
        val ac: AuthContext = createDummyUser("VALID_ID")
        val result = await(controller.noSessionCheck(implicit ac => implicit request => Ok("Shouldnt get here..."))(ac, noSessionIdRequest))
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/payrollbik/payrolled-benefits-expenses")
      }
    }
  }

}
