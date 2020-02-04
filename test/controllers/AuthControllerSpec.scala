/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers

import org.scalatestplus.play.PlaySpec
import play.api.libs.crypto.CSRFTokenSigner
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AuthControllerSpec extends PlaySpec with FakePBIKApplication {

  class SetUp {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val csrfTokenSigner: CSRFTokenSigner = app.injector.instanceOf[CSRFTokenSigner]

    def csrfToken: (String, String) = "csrfToken" -> csrfTokenSigner.generateToken

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(csrfToken)

    def fakeAuthenticatedRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest().withSession(csrfToken).withHeaders()
  }

  "When an valid user logs in, and their action is Authorised" should {
    "be status 200" in new SetUp {
      val controller: AuthController = app.injector.instanceOf[AuthController]
      implicit val testRequest: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
      val result: Future[Result] = controller.notAuthorised()(fakeRequest)
      status(result) must be(OK) // 200
      val bodyText: String = contentAsString(result)
      assert(bodyText.contains("Enrol to use this service"))
      assert(bodyText.contains(
        "You’re signed in to HMRC Online Services but your employer must enrol for employer Pay As You Earn before you can continue."))
      assert(bodyText.contains("To enrol you’ll need:"))
      assert(bodyText.contains("employer PAYE reference"))
      assert(bodyText.contains("Accounts office reference"))
      assert(bodyText.contains(
        "You’ll then be sent an activation code in the post. When you receive it, log on again and use it to confirm your enrolment."))
      assert(bodyText.contains("You’ll then be able to use payrolling benefits and expenses."))
    }
  }
}
