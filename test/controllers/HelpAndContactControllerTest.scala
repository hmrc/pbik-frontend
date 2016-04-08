/*
 * Copyright 2016 HM Revenue & Customs
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
import models.TaxYearRange
import org.mockito.Mockito._
import org.scalatest.Matchers
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF
import play.filters.csrf.CSRF.UnsignedTokenProvider
import services.BikListService
import support.TestAuthUser
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils.{FormMappings, TaxDateUtils}

import scala.concurrent.Future
import scala.concurrent.duration._

class HelpAndContactControllerTest extends UnitSpec with FakePBIKApplication with Matchers
                                              with TestAuthUser with FormMappings{

  implicit val ac = createDummyUser("testid")
  val timeoutValue = 10 seconds
  def YEAR_RANGE:TaxYearRange = TaxDateUtils.getTaxYearRange()

  val helpForm = Form(
    tuple(
      "contact-name" -> text,
      "contact-email" -> text,
      "contact-comments" -> text
    )
  )


  class MockHelpAndContactController extends HelpAndContactController with TierConnector {
    override lazy val pbikAppConfig = mock[AppConfig]
    override def bikListService = mock[BikListService]
    override val tierConnector = mock[HmrcTierConnector]
    override val httpPost = mock[WSHttp]
    override val contactFrontendPartialBaseUrl = pbikAppConfig.contactFrontendService
    override val contactFormServiceIdentifier = pbikAppConfig.contactFormServiceIdentifier

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

    when(httpPost.POSTForm[HttpResponse](anyString, any)(any, any))
      .thenReturn(Future.successful(HttpResponse(OK, None, Map(), Some("form submitted"))))

    when(httpPost.GET[HttpResponse](anyString)(any, any))
      .thenReturn(Future.successful(HttpResponse(OK, None, Map(), Some("form submitted"))))

  }

  "When using help/ contact hmrc, the HelpAndContactController " should {
    "get 500 response when there is an empty form" in {
      running(fakeApplication) {
        val mockHelpController = new MockHelpAndContactController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val request = mockrequest
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        val result = await(mockHelpController.submitContactHmrcForm().apply(request)/*(ac, mockrequest, hc)*/)
        status(result) shouldBe 500
        bodyOf(result) should include("")
      }
    }
  }

  "When using help/ contact hmrc, the HelpAndContactController " should {
    "be able to submit the contact form successfully " in {
      running(fakeApplication) {
        val mockHelpController = new MockHelpAndContactController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val request = mockrequest
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        val helpFormFilled = helpForm.fill("John", "john@gmail.com", "test comment")
        val mockRequestForm = mockrequest.withFormUrlEncodedBody(helpForm.data.toSeq: _*)
        val result = await(mockHelpController.submitContactHmrcForm().apply(mockRequestForm)/*(ac, mockrequest, hc)*/)
        status(result) shouldBe 303

        val nextUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }
        println("Next URL " + nextUrl)
        val newResult = route(FakeRequest(GET, nextUrl)).get

        contentAsString(newResult) should include("")
      }
    }
  }

}
