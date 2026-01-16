/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.actions

import base.FakePBIKApplication
import models.auth.AuthenticatedRequest
import play.api.mvc._
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class NoSessionCheckActionSpec extends FakePBIKApplication {

  class Harness extends NoSessionCheckActionImpl {
    def callTransform[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthenticatedRequest[A]]] =
      refine(request)
  }

  "No Session Check Action" when {
    "there is a session" must {
      "leave the request unfiltered" in {
        val requestWithSessionID = createAuthenticatedRequest(mockRequest)

        val result = Await.result(new Harness().callTransform(requestWithSessionID), 5.seconds)
        result mustBe Right(requestWithSessionID)
      }
    }

    "the session is not set" must {
      "redirect user to home page controller " in {
        val request = createAuthenticatedRequest(FakeRequest("", ""))

        val result                                        = new Harness().callTransform(request)
        val call: Either[Result, AuthenticatedRequest[?]] = Await.result(result, 5.seconds)
        call match {
          case Left(callResult) =>
            val headers: Map[String, String] = callResult.header.headers
            headers.getOrElse("Location", "") must include("/payrollbik/start-payrolling-benefits-expenses")
          case Right(_)         => fail("Result not a Left")
        }
      }
    }
  }

}
