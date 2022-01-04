/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.TestAuthUser
import uk.gov.hmrc.auth.core.retrieve.Name
import utils.{TestAuthAction, TestNoSessionCheckAction}

class StartPageControllerSpec extends PlaySpec with FakePBIKApplication with TestAuthUser with I18nSupport {

  override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val lang: Lang = Lang("en-GB")

  override lazy val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .overrides(bind[AuthAction].to(classOf[TestAuthAction]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .build()

  val startPageController: StartPageController = app.injector.instanceOf[StartPageController]

  "StartPage Controller" must {

    "return OK and the correct view for a GET" in {

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)

      val result = startPageController.onPageLoad().apply(authenticatedRequest)

      status(result) mustEqual OK
      contentAsString(result) must include(messagesApi("StartPage.heading"))
      contentAsString(result) must include(messagesApi("StartPage.p5"))
    }
  }

}
