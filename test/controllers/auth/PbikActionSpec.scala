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

package controllers.auth

import controllers.FakePBIKApplication
import org.scalatestplus.play.PlaySpec
import org.specs2.mock.Mockito
import play.api.http.HttpEntity.Strict
import play.api.mvc.Results
import play.api.test.Helpers._
import support.TestAuthUser
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.{AuthContext, TaxRegime}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PbikActionSpec extends PlaySpec with FakePBIKApplication
                          with TestAuthUser with Results {


  class StubRegime extends TaxRegime {

    override def isAuthorised(accounts: Accounts):Boolean = true
    override val unauthorisedLandingPage = Some(routes.AuthController.notAuthorised().url)
    override val authenticationType: PBIKGovernmentGateway.type = PBIKGovernmentGateway
  }

  class PbikActionTestController extends PbikActions with Mockito {

    //TODO: Make sure this is using new Auth
    lazy val authConnector: AuthConnector = mock[AuthConnector]
    override val getAuthorisedForPolicy = new StubRegime

  }

  "PbikActions" should {
    "show Unauthorised if the session is not authenticated" in {
      val controller = new PbikActionTestController()
      val ac: AuthContext = createDummyUser("VALID_ID")
      val result = await(controller.noSessionCheck(implicit ac => implicit request => Future(Ok("Passed Test")))(ac, mockrequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include("Passed Test")
    }
  }

  "PbikActions" should {
    "show the start page if the session is not set" in {
      val controller = new PbikActionTestController()
      val ac: AuthContext = createDummyUser("VALID_ID")
      val result = await(controller.noSessionCheck(implicit ac => implicit request => Future(Ok("Shouldnt get here...")))(ac, noSessionIdRequest))
      result.header.status must be(SEE_OTHER) // 303
      result.header.headers.getOrElse("Location","") must include("/payrollbik/payrolled-benefits-expenses")
      result.header.headers.getOrElse("Set-Cookie","") must include("mdtp=")
      result.header.headers.getOrElse("Set-Cookie","") must include("Path=/; HTTPOnly")
    }
  }

}
