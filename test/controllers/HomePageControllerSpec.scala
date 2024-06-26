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

import connectors.PbikConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BikListService
import support.{StubbedBikListService, TestSplunkLogger}
import uk.gov.hmrc.http.SessionKeys
import utils._

class HomePageControllerSpec extends PlaySpec with FakePBIKApplication with I18nSupport {

  override lazy val fakeApplication: Application     = GuiceApplicationBuilder()
    .overrides(bind[PbikConnector].toInstance(mock(classOf[PbikConnector])))
    .overrides(bind[BikListService].to(classOf[StubbedBikListService]))
    .overrides(bind[SplunkLogger].to(classOf[TestSplunkLogger]))
    .overrides(bind[AuthAction].to(classOf[TestAuthActionOrganisation]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .build()
  private val homePageController: HomePageController = app.injector.instanceOf[HomePageController]

  implicit val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]

  override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "HomePageController" when {
    ".notAuthorised" should {
      "return 401 (UNAUTHORIZED) for a notAuthorised method call" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockRequest
        val result                                                = homePageController.notAuthorised()(request)

        status(result) mustBe UNAUTHORIZED
        contentAsString(result) must include(Messages("ErrorPage.authorisationError"))
      }
    }

    ".onPageLoadCY1" should {
      "return 401 (UNAUTHORIZED) if the session is not authenticated" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withSession(SessionKeys.sessionId -> "hackmeister")
        val result                                                = homePageController.onPageLoadCY1(request)

        status(result) mustBe UNAUTHORIZED
        contentAsString(result) mustBe "Request was not authenticated user should be redirected"
      }

      "logout and redirect to feed back page" in {
        val result = homePageController.signout(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("/feedback/PBIK")
      }

      "display the navigation page" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withSession(SessionKeys.sessionId -> sessionId)
        val result                                                = homePageController.onPageLoadCY1(request)

        status(result) mustBe OK
        contentAsString(result) must include(Messages("StartPage.heading.organisation"))
        contentAsString(result) must include(
          "Is this page not working properly? (opens in new tab)"
        )
      }

      "set the request language and redirect with no referer header" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockWelshRequest
        val result                                                = homePageController.setLanguage()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("https://www.tax.service.gov.uk/payrollbik/payrolled-benefits-expenses")
      }

      "set the request language and reload page based on referer header - Welsh" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockWelshRequest
          .withHeaders("Referer" -> "/payrollbik/payrolled-benefits-expenses")
        val result                                                = homePageController.setLanguage()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/payrollbik/payrolled-benefits-expenses")
      }

      "display the navigation page - Welsh" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockWelshRequest
        val result                                                = homePageController.onPageLoadCY1(request)

        status(result) mustBe OK
        contentAsString(result) must include("Cofrestru buddiant neu draul")
      }
    }

    ".onPageLoadCY" should {
      "return 401 (UNAUTHORIZED) if the session is not authenticated" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withSession(SessionKeys.sessionId -> "hackmeister")
        val result                                                = homePageController.onPageLoadCY(request)

        status(result) mustBe UNAUTHORIZED
        contentAsString(result) mustBe "Request was not authenticated user should be redirected"
      }

      "display the navigation page" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withSession(SessionKeys.sessionId -> sessionId)
        val result                                                = homePageController.onPageLoadCY(request)

        status(result) mustBe OK
        contentAsString(result) must include(Messages("StartPage.heading.organisation"))
        contentAsString(result) must include(
          "Is this page not working properly? (opens in new tab)"
        )
      }

      "set the request language and redirect with no referer header" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockWelshRequest
        val result                                                = homePageController.setLanguage()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("https://www.tax.service.gov.uk/payrollbik/payrolled-benefits-expenses")
      }

      "set the request language and reload page based on referer header - Welsh" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockWelshRequest
          .withHeaders("Referer" -> "/payrollbik/payrolled-benefits-expenses")
        val result                                                = homePageController.setLanguage()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/payrollbik/payrolled-benefits-expenses")
      }

      "display the navigation page - Welsh" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockWelshRequest
        val result                                                = homePageController.onPageLoadCY(request)

        status(result) mustBe OK
        contentAsString(result) must include("Rheoli buddiant neu draul")
      }
    }
  }
}
