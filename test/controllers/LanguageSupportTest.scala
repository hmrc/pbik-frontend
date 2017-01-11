/*
 * Copyright 2017 HM Revenue & Customs
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
import connectors.{HmrcTierConnector, TierConnector}
import controllers.registration.ManageRegistrationController
import models._
import org.mockito.Matchers.{eq => mockEq}
import org.mockito.Mockito._
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures._
import play.api.data.Form
import play.api.i18n.{Lang, Messages}
import play.api.libs.json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF
import play.filters.csrf.CSRF.UnsignedTokenProvider
import play.twirl.api.HtmlFormat
import services.{BikListService, RegistrationService}
import support.TestAuthUser
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{SessionKeys, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec
import utils._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class LanguageSupportTest extends UnitSpec with Matchers with FormMappings with TestAuthUser
   with FakePBIKApplication {


  implicit val ac = createDummyUser("testid")
  lazy val CYCache = List.tabulate(21)(n => new Bik("" + (n + 1), 10))
  lazy val CYRegistrationItems = List.tabulate(21)(n=> new RegistrationItem("" + (n + 1), true, true))
  val timeoutValue = 15 seconds

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

    override def registeredBenefitsList(year: Int, orgIdentifier: String)(path: String)
                                       (implicit ac: AuthContext, hc: HeaderCarrier, request: Request[_]) :  Future[List[Bik]] = {
      CYCache
    }

  }

  class FakeResponse extends HttpResponse {
    override def status = 200
  }

  class StubbedRegistrationService extends RegistrationService with TierConnector {

    override lazy val pbikAppConfig: AppConfig = mock[AppConfig]
    override def bikListService: BikListService = new StubBikListService
    override val tierConnector = mock[HmrcTierConnector]
    val dateRange = TaxDateUtils.getTaxYearRange()

    val registeredListOption = Future.successful(List.empty[Bik])
    val allRegisteredListOption = Future.successful(CYCache)
    val mockRegistrationItemList = List.empty[RegistrationItem]
    val mockFormRegistrationList: Form[RegistrationList] = objSelectedForm.fill(RegistrationList(None, CYRegistrationItems))


    override def generateViewForBikRegistrationSelection(year: Int, cachingSuffix: String,
                                                generateViewBasedOnFormItems: (Form[RegistrationList],
                                                  List[RegistrationItem], List[Bik], List[Int], List[Int], Option[Int]) => HtmlFormat.Appendable)
                                               (implicit hc:HeaderCarrier, request: Request[AnyContent], ac: AuthContext):
    Future[Result] = {
      year match {
        case dateRange.cyminus1 => {
          Future.successful(Ok(
            views.html.registration.currentTaxYear(
              mockFormRegistrationList,dateRange,mockRegistrationItemList,allRegisteredListOption,PbikAppConfig.biksNotSupported, biksAvailableCount=Some(17))
          ))
        }
        case _ => {
          Future.successful(Ok(
            views.html.registration.nextTaxYear(
              mockFormRegistrationList,true,dateRange,mockRegistrationItemList,registeredListOption,PbikAppConfig.biksNotSupported, biksAvailableCount=Some(17))
          ))
        }
      }
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

  class MockRegistrationController extends ManageRegistrationController with TierConnector
                                      with ControllersReferenceData {

    import org.scalatest.time.{Millis, Seconds, Span}

    implicit val defaultPatience =
      PatienceConfig(timeout = Span(7, Seconds), interval = Span(600, Millis))

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

    override def logSplunkEvent(dataEvent: DataEvent)(implicit hc: HeaderCarrier, ac: AuthContext): Future[AuditResult] = {
      Future.successful(AuditResult.Success)
    }

    override lazy val pbikAppConfig: AppConfig = mock[AppConfig]
    override def bikListService: BikListService = new StubBikListService
    override def registrationService = new StubbedRegistrationService

    implicit val mr = mockrequest

    override val tierConnector = mock[HmrcTierConnector]

    val dateRange = TaxDateUtils.getTaxYearRange()

    when(pbikAppConfig.cyEnabled).thenReturn(true)

    when(pbikAppConfig.reportAProblemPartialUrl).thenReturn("")

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(""),
      anyString, mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) <= 10)
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) <= 10)
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) <= 10)
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) <= 10)
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      anyString, mockEq(2020))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) <= 5)
    }))

    when(tierConnector.genericPostCall(anyString, mockEq(updateBenefitTypesPath),
      anyString, anyInt, any)(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]])).thenReturn(Future.successful(new FakeResponse()))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(getRegisteredPath),
      anyString, anyInt)(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) >= 15)
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) <= 10)
    }))
  }


  /*"The Homepage Controller " should {
    "show the welsh language homepage when the language is welsh " in {
      running(fakeApplication) {
        val mockRegistrationController = new MockRegistrationController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val request = mockrequest
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))
        val additions = CYCache.filter { x: Bik => (Integer.parseInt(x.iabdType) > 15) }
        val testac = createDummyUser("testid")
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        implicit val lang:Lang = new Lang("cy")
        val r = await(mockRegistrationController.updateBiksFutureAction(2020, additions, true)(mockrequest, testac/*, lang */))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include("Cyfeirnod TWE y Cyflogwr")
      }
    }
  }*/

  "The Homepage Controller " should {
    "set the request language and redirect to the homepage" in {
      running(fakeApplication) {
        val mockController = new MockHomePageController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val request = mockWelshrequest
        implicit val ac: AuthContext = createDummyUser("VALID_ID")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))
        val additions = CYCache.filter { x: Bik => (Integer.parseInt(x.iabdType) > 15) }
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        //implicit val lang:Lang = new Lang("cy")
        val r = await( mockController.setLanguage.apply(request))(timeout)
        status(r) shouldBe 303
      }
    }
  }

  "HomePageController" should {

    "display the navigation page " in {
      running(fakeApplication) {
        val homePageController = new MockHomePageController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val request = FakeRequest().withSession(
          SessionKeys.sessionId -> sessionId,
          SessionKeys.token -> "RANDOMTOKEN",
          SessionKeys.userId -> userId).withCookies(Cookie("PLAY_LANG", "cy"))
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        implicit val lang : Lang = new Lang("cy")
        val r = await(homePageController.onPageLoad.apply(request))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include("Cyfeirnod TWE y cyflogwr")
      }
    }

  }


}
