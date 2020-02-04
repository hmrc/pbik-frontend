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

package controllers.actions

import java.util.UUID

import akka.util.Timeout
import models.{AuthenticatedRequest, EmpRef, UserName}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class NoSessionCheckActionSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite {

  class Harness() extends NoSessionCheckActionImpl {
    def callTransform[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthenticatedRequest[A]]] =
      refine(request)
  }

  implicit val timeout: Timeout = 5 seconds

  "No Session Check Action" when {
    "there is a session" must {
      "leave the request unfiltered" in {
        val requestWithSessionID = AuthenticatedRequest(
          EmpRef.empty,
          UserName(Name(None, None)),
          FakeRequest("", "")
            .withSession(SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
        val result = new Harness().callTransform(requestWithSessionID)
        whenReady(result) {
          _ mustBe Right(requestWithSessionID)
        }
      }
    }

    "the session is not set" must {
      "redirect user to home page controller " in {
        val request = AuthenticatedRequest(EmpRef.empty, UserName(Name(None, None)), FakeRequest("", ""))
        val result = new Harness().callTransform(request)

        whenReady(result) { call: Either[Result, AuthenticatedRequest[_]] =>
          call match {
            case Left(callResult) =>
              val headers: Map[String, String] = callResult.header.headers
              headers.getOrElse("Location", "") must include("/payrollbik/payrolled-benefits-expenses")
            case Right(_) => fail("Result not a Left")
          }
        }
      }
    }
  }

}
