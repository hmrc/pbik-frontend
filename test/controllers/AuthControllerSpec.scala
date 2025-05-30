/*
 * Copyright 2025 HM Revenue & Customs
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

import base.FakePBIKApplication
import play.api.libs.crypto.CSRFTokenSigner
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AuthControllerSpec extends FakePBIKApplication {

  implicit val hc: HeaderCarrier               = HeaderCarrier()
  private val csrfTokenSigner: CSRFTokenSigner = injected[CSRFTokenSigner]
  private val controller: AuthController       = injected[AuthController]

  private def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(csrfToken)

  private def csrfToken: (String, String) = "csrfToken" -> csrfTokenSigner.generateToken

  "When an valid user logs in, and their action is not authorised the controller" should {
    "return a 401 status with enrolment message" in {
      val result: Future[Result] = controller.notAuthorised()(fakeRequest)

      status(result) mustBe UNAUTHORIZED
      contentAsString(result) must include("Enrol to use this service")
      contentAsString(result) must include(
        "You’re signed in to HMRC Online Services but your employer must enrol for employer Pay As You Earn before you can continue."
      )
      contentAsString(result) must include("To enrol you’ll need:")
      contentAsString(result) must include("employer PAYE reference")
      contentAsString(result) must include("Accounts office reference")
      contentAsString(result) must include(
        "You’ll then be sent an activation code in the post. When you receive it, log on again and use it to confirm your enrolment."
      )
      contentAsString(result) must include("You’ll then be able to use payrolling benefits and expenses.")
    }
  }

  "When an user logs in with an individual account, and their action is not authorised the controller" should {
    "return a 500 status with an IllegalArgumentException message" in {
      val result: Future[Result] = controller.affinityIndividual()(fakeRequest)

      status(result) mustBe UNAUTHORIZED
      contentAsString(result) must include("There is a problem")
      contentAsString(result) must include(
        "You signed in with a Government Gateway user ID for an individual."
      )
      contentAsString(result) must include("To use this service you need to:")
      contentAsString(result) must include("sign out")
      contentAsString(result) must include(
        "sign in with the Government Gateway user ID that you use to access the PAYE for employers online service"
      )
    }
  }
}
