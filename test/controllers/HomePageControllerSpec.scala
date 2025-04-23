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

package controllers

import base.FakePBIKApplication
import connectors.PbikConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models.v1.{BenefitInKindWithCount, BenefitListResponse, IabdType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BikListService
import support.TestSplunkLogger
import uk.gov.hmrc.http.SessionKeys
import utils._

import scala.concurrent.Future

class HomePageControllerSpec extends FakePBIKApplication {

  private val mockConnector: PbikConnector       = mock(classOf[PbikConnector])
  private val mockBikListService: BikListService = mock(classOf[BikListService])

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .overrides(bind[PbikConnector].toInstance(mockConnector))
    .overrides(bind[BikListService].to(mockBikListService))
    .overrides(bind[SplunkLogger].to(classOf[TestSplunkLogger]))
    .overrides(bind[AuthAction].to(classOf[TestAuthActionOrganisation]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .build()

  private val homePageController: HomePageController = injected[HomePageController]
  private val messages: Messages                     = injected[MessagesApi].preferred(Seq(lang))

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockConnector)
    reset(mockBikListService)

    when(mockBikListService.currentYearList(any(), any()))
      .thenReturn(
        Future.successful(
          BenefitListResponse(
            Some(List(BenefitInKindWithCount(IabdType.CarBenefit, 34))),
            5
          )
        )
      )
    when(mockBikListService.nextYearList(any(), any()))
      .thenReturn(
        Future.successful(
          BenefitListResponse(
            Some(List(BenefitInKindWithCount(IabdType.MedicalInsurance, 35))),
            5
          )
        )
      )

    when(mockBikListService.getAllBenefitsForYear(any())(any()))
      .thenReturn(Future.successful(IabdType.values))
  }

  "HomePageController" when {
    ".notAuthorised" should {
      "return 401 (UNAUTHORIZED) for a notAuthorised method call" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockRequest
        val result                                                = homePageController.notAuthorised()(request)

        status(result) mustBe UNAUTHORIZED
        contentAsString(result) must include(messages("ErrorPage.authorisationError"))
      }
    }

    ".onPageLoadCY1" should {
      "return 401 (UNAUTHORIZED) if the session is not authenticated" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withSession(SessionKeys.sessionId -> "test-session-id")
        val result                                                = homePageController.onPageLoadCY1(request)

        status(result) mustBe UNAUTHORIZED
        contentAsString(result) mustBe "Request was not authenticated user should be redirected"
      }

      "logout and redirect to feed back page" in {
        val result = homePageController.signout(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("/feedback/PBIK")
      }

      "logout and redirect to sign out page" in {
        val result = homePageController.signOutNoSurvey(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("we-signed-you-out")
      }

      "display the navigation page" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withSession(SessionKeys.sessionId -> sessionId)
        val result                                                = homePageController.onPageLoadCY1(request)

        status(result) mustBe OK
        contentAsString(result) must include(messages("StartPage.heading.organisation"))
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
        contentAsString(result) must include(messages("StartPage.heading.organisation"))
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
