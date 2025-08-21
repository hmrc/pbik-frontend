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

package base

import config.PbikAppConfig
import controllers.actions.MinimalAuthAction
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Lang
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, BodyParsers, Cookie}
import play.api.test.FakeRequest
import support.AuthenticatedRequestSupport
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.http.SessionKeys
import utils.TestMinimalAuthAction

import java.util.UUID
import scala.reflect.ClassTag

abstract class FakePBIKApplication
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with OptionValues
    with AuthenticatedRequestSupport {

  val lang: Lang     = Lang("en")
  val cyLang: Lang   = Lang("cy")
  val empRef: EmpRef = createEmpRef()

  val configMap: Map[String, Any] = Map(
    "metrics.jvm"                     -> false,
    "metrics.enabled"                 -> false,
    "auditing.enabled"                -> false,
    "play.i18n.langs.0"               -> "en",
    "play.i18n.langs.1"               -> "cy",
    "features.welsh-language-support" -> true
  )

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[MinimalAuthAction].to(classOf[TestMinimalAuthAction]))
    .build()

  val sessionId = s"session-${UUID.randomUUID}"

  def mockRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(SessionKeys.sessionId -> sessionId)

  def mockWelshRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", "?lang=cy")
      .withCookies(Cookie("PLAY_LANG", "cy"))
      .withSession(SessionKeys.sessionId -> sessionId)

  def noSessionIdRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession()

  def injected[T](c: Class[T]): T = app.injector.instanceOf(c)

  def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

  lazy val pbikAppConfig: PbikAppConfig     = injected[PbikAppConfig]
  lazy val bodyParsers: BodyParsers.Default = injected[BodyParsers.Default]

  override def beforeEach(): Unit = super.beforeEach()

  override def afterAll(): Unit = super.afterAll()

  override def afterEach(): Unit = super.afterEach()

  override def beforeAll(): Unit = super.beforeAll()

}
