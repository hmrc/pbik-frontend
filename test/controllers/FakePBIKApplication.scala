/*
 * Copyright 2023 HM Revenue & Customs
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

import akka.stream.Materializer
import controllers.actions.MinimalAuthAction
import models.HeaderTags
import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys
import utils.TestMinimalAuthAction

import java.util.UUID
import scala.reflect.ClassTag

trait FakePBIKApplication extends GuiceOneAppPerSuite {

  this: TestSuite =>

  val configMap: Map[String, Any] = Map(
    "auditing.enabled"            -> false,
    "sessionId"                   -> "a-session-id",
    "pbik.enabled.cy"             -> true,
    "mongodb.timeToLiveInSeconds" -> 3600
  )

  val sessionId = s"session-${UUID.randomUUID}"

  def mockRequest: FakeRequest[AnyContentAsEmpty.type]        =
    FakeRequest().withSession(SessionKeys.sessionId -> sessionId, HeaderTags.ETAG -> "0", HeaderTags.X_TXID -> "0")

  def mockWelshRequest: FakeRequest[AnyContentAsEmpty.type]   =
    FakeRequest("GET", "?lang=cy")
      .withSession(SessionKeys.sessionId -> sessionId, HeaderTags.ETAG -> "0", HeaderTags.X_TXID -> "0")

  def noSessionIdRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession()

  override lazy val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(configMap)
    .overrides(bind[MinimalAuthAction].to(classOf[TestMinimalAuthAction]))
    .build()

  implicit lazy val materializer: Materializer = fakeApplication.materializer

  def injected[T](c: Class[T]): T                    = app.injector.instanceOf(c)
  def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

}
