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

import config.{AppConfig, PbikAppConfig}
import connectors.HmrcTierConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Cookie}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{BikListService, RegistrationService}
import support.{StubbedBikListService, StubbedRegistrationService, TestAuthUser}
import uk.gov.hmrc.http.SessionKeys
import utils.{TestAuthAction, TestNoSessionCheckAction}

class LanguageSupportSpec extends PlaySpec with TestAuthUser with FakePBIKApplication {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .overrides(bind[AppConfig].to(classOf[PbikAppConfig]))
    .overrides(bind[HmrcTierConnector].toInstance(mock(classOf[HmrcTierConnector])))
    .overrides(bind[BikListService].to(classOf[StubbedBikListService]))
    .overrides(bind[RegistrationService].to(classOf[StubbedRegistrationService]))
    .overrides(bind[AuthAction].to(classOf[TestAuthAction]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .build()

  "The Homepage Controller" should {
    "set the request language and reload page based on referer header" in {
      val mockController                                        = app.injector.instanceOf[HomePageController]
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockWelshrequest
        .withHeaders("Referer" -> "/payrollbik/payrolled-benefits-expenses")

      val result                                                = mockController.setLanguage(request)
      (scala.concurrent.ExecutionContext.Implicits.global)
      status(result)               must be(SEE_OTHER)
      redirectLocation(result).get must be("/payrollbik/payrolled-benefits-expenses")
    }
  }

  "HomePageController" should {
    "display the navigation page" in {
      val homePageController                                    = app.injector.instanceOf[HomePageController]
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
        .withSession(SessionKeys.sessionId -> sessionId)
        .withCookies(Cookie("PLAY_LANG", "cy"))

      val result = homePageController.onPageLoad(request)

      status(result)          must be(OK)
      contentAsString(result) must include("Cyfeirnod TWE y cyflogwr")
    }
  }

}
