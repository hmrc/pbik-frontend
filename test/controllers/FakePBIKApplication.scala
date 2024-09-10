/*
 * Copyright 2024 HM Revenue & Customs
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

import controllers.actions.MinimalAuthAction
import models.agent.{AccountsOfficeReference, Client}
import models.{EmpRef, HeaderTags, UserName}
import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Lang
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Cookie}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.SessionKeys
import utils.TestMinimalAuthAction

import java.util.UUID
import scala.reflect.ClassTag

trait FakePBIKApplication extends GuiceOneAppPerSuite {

  this: TestSuite =>

  val lang: Lang     = Lang("en")
  val cyLang: Lang   = Lang("cy")
  val empRef: EmpRef = EmpRef("780", "MODES16")

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[MinimalAuthAction].to(classOf[TestMinimalAuthAction]))
    .build()
  val configMap: Map[String, Any]                = Map(
    "auditing.enabled"            -> false,
    "sessionId"                   -> "a-session-id",
    "pbik.enabled.cy"             -> true,
    "mongodb.timeToLiveInSeconds" -> 3600
  )
  val sessionId                                  = s"session-${UUID.randomUUID}"
  val username: UserName                         = UserName(Name(Some("test"), Some("tester")))
  val organisationClient: Option[Client]         = None
  val agentClient: Option[Client]                = Some(
    Client(
      uk.gov.hmrc.domain.EmpRef(empRef.taxOfficeNumber, empRef.taxOfficeReference),
      AccountsOfficeReference("123AB12345678", "123", "A", "12345678"),
      Some("client test name"),
      lpAuthorisation = false,
      None,
      None
    )
  )

  def mockRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      HeaderTags.ETAG       -> HeaderTags.ETAG_DEFAULT_VALUE
    )

  def mockWelshRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", "?lang=cy")
      .withCookies(Cookie("PLAY_LANG", "cy"))
      .withSession(
        SessionKeys.sessionId -> sessionId,
        HeaderTags.ETAG       -> HeaderTags.ETAG_DEFAULT_VALUE
      )

  def noSessionIdRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession()

  def injected[T](c: Class[T]): T = app.injector.instanceOf(c)

  def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

}
