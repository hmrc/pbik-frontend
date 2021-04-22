/*
 * Copyright 2021 HM Revenue & Customs
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
import config.{PbikAppConfig, PbikSessionCache}
import controllers.actions.MinimalAuthAction
import models.{Bik, EiLPerson, HeaderTags, PbikSession, RegistrationItem, RegistrationList}
import org.scalatest.TestSuite
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.SessionService
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.TestMinimalAuthAction

import scala.reflect.ClassTag

trait FakePBIKApplication extends GuiceOneAppPerSuite {

  this: TestSuite =>

  val config: Map[String, Any] = Map(
    "application.secret"                          -> "Its secret",
    "csrf.sign.tokens"                            -> false,
    "microservice.services.contact-frontend.host" -> "localhost",
    "microservice.services.contact-frontend.port" -> "9250",
    "auditing.enabled"                            -> false,
    "sessionId"                                   -> "a-session-id"
  )

  val sessionId = s"session-${UUID.randomUUID}"

  def mockrequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(SessionKeys.sessionId -> sessionId, HeaderTags.ETAG -> "0", HeaderTags.X_TXID -> "0")

  def mockWelshrequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", "?lang=cy")
      .withSession(SessionKeys.sessionId -> sessionId, HeaderTags.ETAG -> "0", HeaderTags.X_TXID -> "0")

  def noSessionIdRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession()

  override lazy val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .overrides(bind[MinimalAuthAction].to(classOf[TestMinimalAuthAction]))
    .build()

  implicit lazy val materializer: Materializer = fakeApplication.materializer

  def injected[T](c: Class[T]): T = app.injector.instanceOf(c)
  def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

  val mockSessionService: SessionService = mock[SessionService]
  val mockHttp: DefaultHttpClient = mock[DefaultHttpClient]
  val mockPbikSessionCache: PbikSessionCache = mock[PbikSessionCache]
  val mockAppConfig: PbikAppConfig = mock[PbikAppConfig]

  val cleanRegistrationList: Option[RegistrationList] = Some(RegistrationList(None, List.empty[RegistrationItem], None))
  val cleanBikRemoved: Option[RegistrationItem] = Some(RegistrationItem("", false, false))
  val cleanListOfMatches: Option[List[EiLPerson]] = Some(List.empty[EiLPerson])
  val cleanEiLPerson: Option[EiLPerson] = Some(EiLPerson("", "", None, "", None, None, None, None))
  val cleanBikList: Option[List[Bik]] = Some(List.empty[Bik])
  val cleanSession: PbikSession =
    PbikSession(
      cleanRegistrationList,
      cleanBikRemoved,
      cleanListOfMatches,
      cleanEiLPerson,
      cleanListOfMatches,
      cleanBikList,
      cleanBikList
    )
}
