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

import config.AppConfig
import controllers.FakePBIKApplication
import org.specs2.mock.Mockito
import support.AuthorityUtils._
import play.api.test.Helpers._
import play.api.mvc._
import play.api.test.FakeRequest
import play.filters.csrf.CSRF
import play.filters.csrf.CSRF.UnsignedTokenProvider
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import connectors.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import org.mockito.Mockito._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.Crypto

object UserBuilder {

  val epayeAccount = Some(EpayeAccount(empRef = EmpRef(taxOfficeNumber = "taxOfficeNumber", taxOfficeReference ="taxOfficeReference" ), link =""))
  val accounts = Accounts(epaye = epayeAccount)
  val authority = new Authority("", accounts,None,None, CredentialStrength.None,ConfidenceLevel.L50, None, None, None, legacyOid = "testOId")
  val user = LoggedInUser(userId = "testUserId", None, None, None, CredentialStrength.None, ConfidenceLevel.L50, oid = "testOId")
  val principal = Principal(name = Some("TEST_USER"), accounts)

  def apply() = {
    //User(userId = "testUserId", userAuthority = epayeAuthority("testUserId", "emp/ref"), nameFromGovernmentGateway = Some("TEST_USER"), decryptedToken = None)
    new AuthContext(user, principal, None, None, None, None)
  }

}

class AuthControllerSpec extends PlaySpec with OneAppPerSuite with Mockito with FakePBIKApplication {

  class SetUp {
    implicit val hc = HeaderCarrier()
    implicit def user = UserBuilder()

    def csrfToken = "csrfToken" ->  Crypto.generateToken //"csrfToken"Name -> UnsignedTokenProvider.generateToken
    def fakeRequest = FakeRequest().withSession(csrfToken)
    def fakeAuthenticatedRequest = FakeRequest().withSession(csrfToken).withHeaders()

  }

  class TestController extends  AuthController {
    override lazy val pbikAppConfig = mock[AppConfig]
    when(pbikAppConfig.reportAProblemPartialUrl).thenReturn("")
    override protected implicit def authConnector = FrontendAuthConnector
  }

  "When an invalid user logs in, notAuthorised" should {
    "redirect to the authenticaiton page " in new SetUp  {
      val controller = new TestController()
      val result: Future[Result] = await(Future{controller.notAuthorised().apply(fakeRequest)}(scala.concurrent.ExecutionContext.Implicits.global))
      status(result) must be(SEE_OTHER) //303
      redirectLocation(result).get must include(
        "http://localhost:9025/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9233%2Fpayrollbik%2Fpayrolled-benefits-expenses&origin=pbik-frontend")
      val bodyText: String = contentAsString(result)
      assert(bodyText.contains(""))
    }
  }

  "When an valid user logs in, but their action is not Authorised" should {
    "redirect to the not authorised page " in new SetUp {
      val controller = new TestController()
      implicit val testac = user
      implicit val testRequest = fakeRequest
      val result: Future[Result] = await(Future{controller.notAuthorisedResult}(scala.concurrent.ExecutionContext.Implicits.global))
      status(result) must be(OK) // 200
      val bodyText: String = contentAsString(result)
      assert(bodyText.contains("Enrol to use this service"))
    }
  }

  "When an valid user logs in, and their action is  Authorised" should {
    "be status 200 " in new SetUp {
      val controller = new TestController()
      implicit val testac = user
      implicit val testRequest = fakeRequest
      val result: Future[Result] = await(Future{controller.notAuthorisedResult}(scala.concurrent.ExecutionContext.Implicits.global))
      val bodyText: String = contentAsString(result)
      assert(bodyText.contains("Enrol to use this service"))
      assert(bodyText.contains("You’re signed in to HMRC Online Services but your employer must enrol for employer Pay As You Earn before you can continue."))
      assert(bodyText.contains("To enrol you’ll need:"))
      assert(bodyText.contains("employer PAYE reference"))
      assert(bodyText.contains("Accounts office reference"))
      assert(bodyText.contains("You’ll then be sent an activation code in the post. When you receive it, log on again and use it to confirm your enrolment."))
      assert(bodyText.contains("You’ll then be able to use payrolling benefits and expenses."))
    }
  }

}
