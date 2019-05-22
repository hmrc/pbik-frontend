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

package controllers

import java.util.UUID

import akka.stream.Materializer
import models.HeaderTags
import org.scalatest.TestSuite
import org.scalatestplus.play.OneAppPerSuite
import org.specs2.mock.Mockito
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys

import scala.reflect.ClassTag

trait FakePBIKApplication extends Mockito with OneAppPerSuite {

  this: TestSuite =>

  val config: Map[String, Any] = Map("application.secret" -> "Its secret",
                    "csrf.sign.tokens" -> false,
                    "microservice.services.contact-frontend.host" -> "localhost",
                    "microservice.services.contact-frontend.port" -> "9250",
                    "auditing.enabled" -> false,
                    "sessionId" -> "a-session-id")

  val sessionId = s"session-${UUID.randomUUID}"
  val userId = s"user-${UUID.randomUUID}"

  def mockrequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionKeys.sessionId -> sessionId,
    SessionKeys.token -> "RANDOMTOKEN",
    SessionKeys.userId -> userId,
    HeaderTags.ETAG -> "0",
    HeaderTags.X_TXID -> "0")

  def mockWelshrequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "?lang=cy").withSession(
    SessionKeys.sessionId -> sessionId,
    SessionKeys.token -> "RANDOMTOKEN",
    SessionKeys.userId -> userId,
    HeaderTags.ETAG -> "0",
    HeaderTags.X_TXID -> "0")

  def noSessionIdRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(SessionKeys.userId -> userId)

  override val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .build()

  implicit lazy val materializer: Materializer = fakeApplication.materializer

  def injected[T](c: Class[T]): T = app.injector.instanceOf(c)
  def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

}
