/*
 * Copyright 2015 HM Revenue & Customs
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

import models.{HeaderTags, TaxYearRange, BinaryRadioButton, Bik}
import config.AppConfig
import connectors.{HmrcTierConnector, TierConnector}
import org.mockito.Mockito._
import org.scalatest.Matchers
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.{FakeApplication, FakeRequest}
import play.api.test.Helpers._
import play.filters.csrf.CSRF
import play.filters.csrf.CSRF.UnsignedTokenProvider
import services.BikListService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.test.UnitSpec
import utils.BikListUtils.MandatoryRadioButton
import utils.{TaxDateUtils, FormMappings}
import utils.FormMappingsConstants._
import support.TestAuthUser
import scala.concurrent.duration._

import scala.concurrent.Future

class HomePageControllerTest extends UnitSpec with FakePBIKApplication with Matchers
                                              with TestAuthUser with FormMappings{

  implicit val user = createDummyUser("testid")
  val timeoutValue = 10 seconds
  def YEAR_RANGE:TaxYearRange = TaxDateUtils.getTaxYearRange()

  class StubBikListService extends BikListService {

    override lazy val pbikAppConfig = mock[AppConfig]
    override val tierConnector = mock[HmrcTierConnector]
    lazy val CYCache = List.range(3, 32).map(n => new Bik("" + n, 10))/*(n => new Bik("" + (n + 1), 10))*/
    pbikHeaders = Map(HeaderTags.ETAG -> "0", HeaderTags.X_TXID -> "1")

    override def currentYearList(implicit ac: AuthContext, hc: HeaderCarrier, request: Request[_]):
        Future[(Map[String, String], List[Bik])] = {

      Future.successful((Map(HeaderTags.ETAG -> "1"),CYCache.filter { x: Bik => (Integer.parseInt(x.iabdType) == 31) }))
    }

    override def nextYearList(implicit ac: AuthContext, hc: HeaderCarrier, request: Request[_]):
        Future[(Map[String, String], List[Bik])] = {
      
      Future.successful((Map(HeaderTags.ETAG -> "1"), CYCache.filter { x: Bik => (Integer.parseInt(x.iabdType) == 31) }))
    }

  }

  class MockHomePageController extends HomePageController with TierConnector {
    override lazy val pbikAppConfig = mock[AppConfig]
    override val tierConnector = mock[HmrcTierConnector]
    override def bikListService = new StubBikListService

    override def logSplunkEvent(dataEvent: DataEvent)(implicit hc: HeaderCarrier, ac: AuthContext): Future[AuditResult] = {
      Future.successful(AuditResult.Success)
    }

    override def AuthorisedForPbik(body: AuthContext => Request[AnyContent] => Future[Result]): Action[AnyContent] = {
      val user = createDummyUser("testid")
      Action.async { implicit request =>
        if (request.session.get("sessionId").getOrElse("").startsWith("session")) {
          body(user)(request)
        } else {
          Future(Unauthorized("Request was not authenticated user should be redirected"))
        }
      }
    }

  }

  class MockHomePageControllerCYEnabled extends MockHomePageController {
    when(pbikAppConfig.cyEnabled).thenReturn(true)
  }

  class SetUp extends FakeApplication {
    implicit val hc = HeaderCarrier()
    implicit val user = createDummyUser("testId")

  }


  "When instantiating the HomePageController " in {
    running(fakeApplication) {
      val homePageController = HomePageController
      assert(homePageController.pbikAppConfig != null)
      assert(homePageController.tierConnector != null)
      assert(homePageController.bikListService != null)
    }
  }

  "HomePageController" should {
    "show Unauthorised if the session is not authenticated" in new SetUp {
      running(fakeApplication) {
        val homePageController = new MockHomePageController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val request = FakeRequest().withSession(
          SessionKeys.sessionId -> "hackmeister",
          SessionKeys.token -> "RANDOMTOKEN",
          SessionKeys.userId -> userId)
        val r = homePageController.onPageLoad.apply(request)
        status(r) shouldBe 401
      }
    }
  }

  "When a valid user loads the CY warning but CY is disabled the HomePageController" should {
    "show the CY disabled error page" in new SetUp {
      running(fakeApplication) {
        val homePageController = new MockHomePageController
        implicit val request = mockrequest
        implicit val ac: AuthContext = createDummyUser("VALID_ID")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))
        val r = await(homePageController.loadCautionPageForCY.apply(request))
        status(r) shouldBe 200
        bodyOf(r) should include(Messages("ServiceMessage.10003"))
      }
    }
  }

  "When a valid user loads the CY warning page and CY mode is enabled the HomePageController" should {
    "show the page" in new SetUp {
      running(fakeApplication) {
        val homePageController = new MockHomePageControllerCYEnabled
        implicit val request = mockrequest
        implicit val ac: AuthContext = createDummyUser("VALID_ID")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))
        val r = await(homePageController.loadCautionPageForCY.apply(request))
        status(r) shouldBe 200
        bodyOf(r) should include(Messages("AddBenefits.CY.Caution.Title"))
      }
    }
  }

  /*"When a valid user submits an open decisions without a valid form the HomePageController" should {
    "show the page" in new SetUp {
      running(fakeApplication) {
        val homePageController = new MockHomePageController
        implicit val request = mockrequest
        implicit val ac: AuthContext = createDummyUser("VALID_ID")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))
        val r = await(homePageController.overviewDecision.apply(request))
        /*status(r) shouldBe 200
        bodyOf(r) should include(Messages("ErrorPage.invalidForm"))*/
        status(r) shouldBe 303
        val nextUrl = redirectLocation(r) match {
          case Some(s: String) => s
          case _ => ""
        }
        nextUrl should include("/overview")
      }
    }
  }*/

//  "When a valid user submits a valid form to view benefits the HomePageController" should {
//    "show the view registration page" in new SetUp {
//      running(fakeApplication) {
//        val f = navigationRadioButton.fill(new MandatoryRadioButton(selectionValue = VIEW_REGISTERED_BIKS))
//        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
//        val homePageController = new MockHomePageController
//        implicit val ac: AuthContext = createDummyUser("VALID_ID")
//        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))
//        val r = await(homePageController.overviewDecision.apply(formrequest))
//        status(r) shouldBe 303
//        redirectLocation(r) shouldBe Some("/payrollbik/view/registration")
//      }
//    }
//  }


//  "When a valid user submits a valid form to manage benefits the HomePageController" should {
//    "show the view registration page" in new SetUp {
//      running(fakeApplication) {
//        val f = navigationRadioButton.fill(new MandatoryRadioButton(selectionValue =MANAGE_REGISTERED_BIKS))
//        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
//        val homePageController = new MockHomePageController
//        implicit val ac: AuthContext = createDummyUser("VALID_ID")
//        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))
//        val r = await(homePageController.overviewDecision.apply(formrequest))
//        status(r) shouldBe 303
//        redirectLocation(r) shouldBe Some("/payrollbik/manage/selection")
//      }
//    }
//  }

//  "When a valid user submits a valid form to view exclusions the HomePageController" should {
//    "show the view registration page" in new SetUp {
//      running(fakeApplication) {
//        val f = navigationRadioButton.fill(new MandatoryRadioButton(selectionValue = VIEW_EXCLUSIONS))
//        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
//        val homePageController = new MockHomePageController
//        implicit val ac: AuthContext = createDummyUser("VALID_ID")
//        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))
//        val r = await(homePageController.overviewDecision.apply(formrequest))
//        status(r) shouldBe 303
//        redirectLocation(r) shouldBe Some("/payrollbik/exclusion/view")
//      }
//    }
//  }
//
//  "When a valid user submits a valid form to manage exclusions the HomePageController" should {
//    "show the view registration page" in new SetUp {
//      running(fakeApplication) {
//        val f = navigationRadioButton.fill(new MandatoryRadioButton(selectionValue = MANAGE_EXCLUSIONS))
//        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
//        val homePageController = new MockHomePageController
//        implicit val ac: AuthContext = createDummyUser("VALID_ID")
//        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))
//        val r = await(homePageController.overviewDecision.apply(formrequest))
//        status(r) shouldBe 303
//        redirectLocation(r) shouldBe Some("/payrollbik/exclusion/manage")
//      }
//    }
//  }

  /*"When a valid user submits an invalid form to manage exclusions the HomePageController" should {
    "show the same page with validation errors" in new SetUp {
      running(fakeApplication) {
        val f = navigationRadioButton.fill(new MandatoryRadioButton(selectionValue = "a"))
        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
        val homePageController = new MockHomePageController
        implicit val ac: AuthContext = createDummyUser("VALID_ID")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))
        val r = await(homePageController.overviewDecision.apply(formrequest))
        /*status(r) shouldBe 200
        bodyOf(r) should include(Messages("ErrorPage.invalidForm"))*/
        status(r) shouldBe 303
        val nextUrl = redirectLocation(r) match {
          case Some(s: String) => s
          case _ => ""
        }
        nextUrl should include("/overview")
      }
    }
  }*/

  /*"When a radiobutton is unmarshalled but navigation is not an expected value the HomePageController" should {
    "show the same page with validation errors" in new SetUp {
      running(fakeApplication) {
        val f = navigationRadioButton.fill(new MandatoryRadioButton(selectionValue = "GO_TO_LAND_OF_MU"))
        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
        val homePageController = new MockHomePageController
        implicit val ac: AuthContext = createDummyUser("VALID_ID")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))
        val r = await(homePageController.overviewDecision.apply(formrequest))
        status(r) shouldBe 200
        bodyOf(r) should include(Messages("ErrorPage.invalidForm"))
      }
    }
  }

*/
  "HomePageController" should {

      "display the navigation page " in new SetUp {
        running(fakeApplication) {
          val homePageController = new MockHomePageController
          def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
          implicit val request = FakeRequest().withSession(
            SessionKeys.sessionId -> sessionId,
            SessionKeys.token -> "RANDOMTOKEN",
            SessionKeys.userId -> userId)
          implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
          val r = await(homePageController.onPageLoad.apply(request))(timeout)
          bodyOf(r) should include(Messages("Overview.heading"))
          bodyOf(r) should include(Messages("Overview.next.heading", ""+YEAR_RANGE.cy, ""+YEAR_RANGE.cyplus1))
          bodyOf(r) should include(Messages("Overview.table.heading.1"))
          bodyOf(r) should include(Messages("Overview.current.heading", ""+YEAR_RANGE.cyminus1, ""+YEAR_RANGE.cy))
          status(r) shouldBe 200
      }
    }

  }

}
