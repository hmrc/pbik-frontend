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

package controllers

import config.{AppConfig, PbikAppConfig}
import connectors.HmrcTierConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models._
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.http.HttpEntity.Strict
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BikListService
import support.{CYEnabledSetup, StubbedBikListService, TestAuthUser, TestSplunkLogger}
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import utils._

import scala.concurrent.duration._

class HomePageControllerSpec extends PlaySpec with FakePBIKApplication with TestAuthUser with I18nSupport {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val formMappings: FormMappings = app.injector.instanceOf[FormMappings]
  val timeoutValue: FiniteDuration = 10 seconds

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .overrides(bind[AppConfig].to(classOf[PbikAppConfig]))
    .overrides(bind[HmrcTierConnector].toInstance(mock(classOf[HmrcTierConnector])))
    .overrides(bind[BikListService].to(classOf[StubbedBikListService]))
    .overrides(bind[SplunkLogger].to(classOf[TestSplunkLogger]))
    .overrides(bind[AuthAction].to(classOf[TestAuthAction]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .build()

  implicit val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]
  def YEAR_RANGE: TaxYearRange = taxDateUtils.getTaxYearRange()

  "When checking if from YTA referer ends /account" in {
    val homePageController = app.injector.instanceOf[HomePageController]
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(
      "referer" -> "/account"
    )
    val result = homePageController.isFromYTA
    result must be(true)
  }

  "When checking if from YTA referer ends /business-account" in {
    val homePageController = app.injector.instanceOf[HomePageController]
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(
      "referer" -> "/business-account"
    )
    val result = homePageController.isFromYTA
    result must be(true)
  }

  "When checking if from YTA referer ends /someother" in {
    val homePageController = app.injector.instanceOf[HomePageController]
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(
      "referer" -> "/someother"
    )
    val result = homePageController.isFromYTA
    result must be(false)
  }

  "HomePageController" should {
    "show Unauthorised if the session is not authenticated" in {
      val homePageController = app.injector.instanceOf[HomePageController]
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
        SessionKeys.sessionId -> "hackmeister",
        SessionKeys.token     -> "RANDOMTOKEN",
        SessionKeys.userId    -> userId)
      val result = homePageController.onPageLoad.apply(request)
      status(result) must be(UNAUTHORIZED) //401
    }

    "logout and redirect to feed back page" in {
      val homePageController = app.injector.instanceOf[HomePageController]
      val result = homePageController.signout.apply(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get must include("/feedback/PBIK")
    }
  }

  "When a valid user loads the CY warning but CY is disabled the HomePageController" should {
    "show the CY disabled error page" in {
      val homePageController = app.injector.instanceOf[HomePageController]

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
      val result = await(homePageController.loadCautionPageForCY.apply(request))
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.10003.1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.10003.2"))
    }
  }

  "When a valid user loads the CY warning page and CY mode is enabled the HomePageController" should {
    "show the page" in new CYEnabledSetup {

      val injector: Injector = new GuiceApplicationBuilder()
        .overrides(GuiceTestModule)
        .injector()

      val homePageController = injector.instanceOf[HomePageController]
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
      val result = await(homePageController.loadCautionPageForCY.apply(request))
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("AddBenefits.CY.Caution.Title"))
    }
  }

  "HomePageController" should {
    "display the navigation page" in {
      val homePageController = app.injector.instanceOf[HomePageController]
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
        SessionKeys.sessionId -> sessionId,
        SessionKeys.token     -> "RANDOMTOKEN",
        SessionKeys.userId    -> userId)
      implicit val timeout: akka.util.Timeout = timeoutValue
      val result = await(homePageController.onPageLoad(request))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("Overview.heading"))
      result.body.asInstanceOf[Strict].data.utf8String must include(
        Messages("Overview.next.heading", "" + YEAR_RANGE.cy, "" + YEAR_RANGE.cyplus1))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("Overview.table.heading.1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(
        Messages("Overview.current.heading", "" + YEAR_RANGE.cyminus1, "" + YEAR_RANGE.cy))
      result.body.asInstanceOf[Strict].data.utf8String must include(
        "Help improve digital services by joining the HMRC user panel <span class=\"visuallyhidden\">(opens in new window)</span></a>")
      result.body.asInstanceOf[Strict].data.utf8String must include("No thanks")
    }
  }

}
