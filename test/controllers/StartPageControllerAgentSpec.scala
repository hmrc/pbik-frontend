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

import controllers.actions.{AuthAction, NoSessionCheckAction}
import models.{AuthenticatedRequest, EmpRef, UserName}
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.retrieve.Name
import utils.{TestAuthActionAgent, TestNoSessionCheckAction}

class StartPageControllerAgentSpec extends PlaySpec with FakePBIKApplication {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[AuthAction].to(classOf[TestAuthActionAgent]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .build()

  val lang: Lang                                   = Lang("en")
  val cyLang: Lang                                 = Lang("cy")
  val request: FakeRequest[AnyContentAsEmpty.type] = mockRequest

  val agentRequest: AuthenticatedRequest[AnyContentAsEmpty.type]      =
    AuthenticatedRequest(
      EmpRef("taxOfficeNumber", "taxOfficeReference"),
      UserName(Name(None, None)),
      mockRequest,
      agentClient
    )
  val agentRequestWelsh: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(
      EmpRef("taxOfficeNumber", "taxOfficeReference"),
      UserName(Name(None, None)),
      mockWelshRequest,
      agentClient
    )

  private val messages: Messages                       = app.injector.instanceOf[MessagesApi].preferred(Seq(lang))
  private val cyMessages: Messages                     = app.injector.instanceOf[MessagesApi].preferred(Seq(cyLang))
  private val startPageController: StartPageController = app.injector.instanceOf[StartPageController]

  "StartPageController - Agent" must {
    "return OK and the correct view for a GET - English" in {
      val result = startPageController.onPageLoad().apply(agentRequest)

      status(result) mustEqual OK
      contentAsString(result) must include(messages("StartPage.heading." + agentRequest.userType))
      contentAsString(result) must include(messages("StartPage.p5." + agentRequest.userType))
    }

    "return OK and the correct view for a GET - Welsh" in {
      val result = startPageController.onPageLoad().apply(agentRequestWelsh)

      status(result) mustEqual OK
      contentAsString(result) must include(cyMessages("StartPage.heading." + agentRequestWelsh.userType))
      contentAsString(result) must include(cyMessages("StartPage.p5." + agentRequestWelsh.userType))
    }
  }

}
