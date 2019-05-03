/*
 * Copyright 2019 HM Revenue & Customs
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

import config.AppConfig
import connectors.{HmrcTierConnector, TierConnector}
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models.TaxYearRange
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.HttpEntity.Strict
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BikListService
import support.TestAuthUser
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.ws.WSHttp
import utils.{FormMappings, TaxDateUtils, TestAuthAction, TestNoSessionCheckAction}

import scala.concurrent.Future
import scala.concurrent.duration._

class HelpAndContactControllerSpec extends PlaySpec with FakePBIKApplication
  with TestAuthUser with FormMappings {

  override val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(config)
    .build()

  implicit val ac: AuthContext = createDummyUser("testid")
  val timeoutValue: FiniteDuration = 10 seconds

  def YEAR_RANGE: TaxYearRange = TaxDateUtils.getTaxYearRange()

  val helpForm = Form(
    tuple(
      a1 = "contact-name" -> text,
      a2 = "contact-email" -> text,
      a3 = "contact-comments" -> text
    )
  )

  class MockHelpAndContactController extends HelpAndContactController with TierConnector {
    override lazy val pbikAppConfig: AppConfig = mock[AppConfig]
    override val authenticate: AuthAction = new TestAuthAction
    override val noSessionCheck: NoSessionCheckAction = new TestNoSessionCheckAction

    override def bikListService: BikListService = mock[BikListService]

    override val tierConnector: HmrcTierConnector = mock[HmrcTierConnector]
    override val httpPost: WSHttp = mock[WSHttp]
    override val contactFrontendPartialBaseUrl: String = pbikAppConfig.contactFrontendService
    override val contactFormServiceIdentifier: String = pbikAppConfig.contactFormServiceIdentifier

    override def AuthorisedForPbik(body: AuthContext => Request[AnyContent] => Future[Result]): Action[AnyContent] = {
      val ac = createDummyUser("testid")
      Action.async { implicit request =>
        if (request.session.get("sessionId").getOrElse("").startsWith("session")) {
          body(ac)(request)
        } else {
          Future(Unauthorized("Request was not authenticated user should be redirected"))
        }
      }
    }

    when(httpPost.POSTForm[HttpResponse](anyString, any)(any, any, any))
      .thenReturn(Future.successful(HttpResponse(OK, None, Map(), Some("form submitted"))))

    when(httpPost.GET[HttpResponse](anyString)(any, any, any))
      .thenReturn(Future.successful(HttpResponse(OK, None, Map(), Some("form submitted"))))
  }

  "When using help / contact hmrc, the HelpAndContactController " should {
    "get 500 response when there is an empty form" in {
      val mockHelpController = new MockHelpAndContactController
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val result = await(mockHelpController.submitContactHmrcForm(request))
      result.header.status must be(INTERNAL_SERVER_ERROR) // 500
      result.body.asInstanceOf[Strict].data.utf8String must include("")
    }

    "be able to submit the contact form successfully " in {
      val mockHelpController = new MockHelpAndContactController
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val mockRequestForm = mockrequest.withFormUrlEncodedBody(helpForm.data.toSeq: _*)
      val result = await(mockHelpController.submitContactHmrcForm(mockRequestForm))
      result.header.status must be(SEE_OTHER) // 303

      val nextUrl = redirectLocation(Future.successful(result)).getOrElse("")

      nextUrl mustBe "/payrollbik/help-and-contact-confirmed"
    }
  }
}
