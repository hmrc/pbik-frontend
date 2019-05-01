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
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import support.AuthorityUtils._
import support.TestAuthUser
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._

class TestEPayeUser extends EpayeUser

class EpayeUserSpec extends PlaySpec with FakePBIKApplication with TestAuthUser {

  def v(): AuthContext = {
    val epayeAccount = Some(EpayeAccount(empRef = EmpRef(taxOfficeNumber = "123", taxOfficeReference ="45678" ), link =""))
    val accounts = Accounts(epaye = epayeAccount)
    val authority = epayeAuthority("testUserId", "123/45678")
    val user = LoggedInUser(userId = "testUserId", None, None, None, CredentialStrength.None, ConfidenceLevel.L50, oid = "testOId")
    val principal = Principal(name = Some("TEST_USER"), accounts)
    new AuthContext(user, principal, None, None, None, None)
  }

  def i(): AuthContext = {
    val epayeAccount = None
    val accounts = Accounts(epaye = epayeAccount)
    val authority = ctAuthority("testUserId", "UTREF")
    val user = LoggedInUser(userId = "testUserId", None, None, None, CredentialStrength.None, ConfidenceLevel.L50, oid = "testOId")
    val principal = Principal(name = Some("TEST_USER"), accounts)
    new AuthContext(user, principal, None, None, None, None)
  }

  "When accessing the PBIKEpayeRegime the unauthorisedLandingPage " should {
    "  be set correctly " in {
      val ob = PBIKEpayeRegime
      assert(ob.unauthorisedLandingPage.contains("/payrollbik/not-authorised"))
    }
  }

  "When accessing the PBIKEpayeRegime the authenticationType " should {
    " should not be null " in {
      val ob = PBIKEpayeRegime
      assert(ob.authenticationType != null)
    }
  }

  "When accessing the PBIKEpayeRegime a valid account  " should {
    " return true " in {
      val ob = PBIKEpayeRegime
      assert(ob.isAuthorised(createDummyUser("VALID").principal.accounts))
    }
  }

  "When accessing the PBIKEpayeRegime a no EPaye account  " should {
    " return false " in {
      val ob = PBIKEpayeRegime
      assert(ob.isAuthorised(createDummyNonEpayeUser("INVALID").principal.accounts) == false)
    }
  }

  "When an EpayeUser is created a valid EmpRef " should {
    " be available " in {
      val controller = new TestEPayeUser()
      val result = controller.ePayeUtr(v)
      assert(result.taxOfficeNumber == "123")
      assert(result.taxOfficeReference == "45678")
    }
  }

  "When an EpayeUser is created without a valid EmpRef it " should {
    " not return an EmpRef " in {
      val controller = new TestEPayeUser()
      val result = controller.ePayeUtr(i)
      assert(result == null)
    }
  }

}
