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

package controllers

import java.util.UUID

import org.specs2.mock.Mockito
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.test.WithFakeApplication
import org.scalatest.Suite
import play.api.test.{FakeRequest, FakeApplication}

trait FakePBIKApplication extends WithFakeApplication with Mockito {
  this: Suite =>
  val config = Map("application.secret" -> "Its secret",
                    "csrf.sign.tokens" -> false,
                    "microservice.services.contact-frontend.host" -> "localhost",
                    "microservice.services.contact-frontend.port" -> "9250",
                    "auditing.enabled" -> false,
                    "sessionId" -> "a-session-id")

  val sessionId = s"session-${UUID.randomUUID}"
  val userId = s"user-${UUID.randomUUID}"

  def mockrequest = FakeRequest().withSession(
    SessionKeys.sessionId -> sessionId,
    SessionKeys.token -> "RANDOMTOKEN",
    SessionKeys.userId -> userId)

  def noSessionIdRequest = FakeRequest().withSession(
    SessionKeys.userId -> userId)

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = config)
}
