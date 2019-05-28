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

import config.{AppConfig, LocalFormPartialRetriever, PbikAppConfig, PbikContext}
import connectors.HmrcTierConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import javax.inject.Inject
import models._
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.http.HttpEntity.Strict
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration, Environment}
import services.BikListService
import support.TestAuthUser
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import utils._

import scala.concurrent.Future
import scala.concurrent.duration._

class HomePageControllerSpec extends PlaySpec with FakePBIKApplication
  with TestAuthUser with FormMappings {

  val timeoutValue: FiniteDuration = 10 seconds

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .overrides(bind[AppConfig].toInstance(mock(classOf[PbikAppConfig])))
    .overrides(bind[HmrcTierConnector].toInstance(mock(classOf[HmrcTierConnector])))
    .overrides(bind[BikListService].to(classOf[StubBikListService]))
    .build()

  implicit val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]
  def YEAR_RANGE: TaxYearRange = taxDateUtils.getTaxYearRange()



  class StubBikListService @Inject()(pbikAppConfig: PbikAppConfig,
                                     tierConnector: HmrcTierConnector,
                                     controllersReferenceData: ControllersReferenceData,
                                     uRIInformation: URIInformation) extends BikListService(pbikAppConfig,
                                                                                tierConnector,
                                                                                controllersReferenceData,
                                                                                uRIInformation) {

    lazy val CYCache: List[Bik] = List.range(3, 32).map(n => Bik("" + n, 10))
    /*(n => new Bik("" + (n + 1), 10))*/
    override lazy val pbikHeaders: Map[String, String] = Map(HeaderTags.ETAG -> "0", HeaderTags.X_TXID -> "1")

    override def currentYearList(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]):
    Future[(Map[String, String], List[Bik])] = {
      Future.successful((Map(HeaderTags.ETAG -> "1"), CYCache.filter { x: Bik => Integer.parseInt(x.iabdType) == 31 }))
    }

    override def nextYearList(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]):
    Future[(Map[String, String], List[Bik])] = {
      Future.successful((Map(HeaderTags.ETAG -> "1"), CYCache.filter { x: Bik => Integer.parseInt(x.iabdType) == 31 }))
    }

    override def registeredBenefitsList(year: Int, empRef: EmpRef)(path: String)
                              (implicit hc: HeaderCarrier, request: Request[_]): Future[List[Bik]] = {
      Future.successful(CYCache)
    }

  }


  class MockHomePageController @Inject()(bikListService: BikListService,
                                         pbikAppConfig: PbikAppConfig,
                                         authAction: AuthAction,
                                         noSessionCheckAction: NoSessionCheckAction,
                                         runModeConfiguration: Configuration,
                                         environment: Environment,
                                         taxDateUtils: TaxDateUtils,
                                         controllersReferenceData: ControllersReferenceData,
                                         splunkLogger: SplunkLogger,
                                         context: PbikContext,
                                         uRIInformation: URIInformation,
                                         externalURLs: ExternalUrls,
                                         localFormPartialRetriever: LocalFormPartialRetriever) extends HomePageController(
    bikListService,
    authAction,
    noSessionCheckAction,
    runModeConfiguration,
    environment,
    controllersReferenceData,
    splunkLogger)(
    taxDateUtils,
    pbikAppConfig,
    context,
    uRIInformation,
    externalURLs,
    localFormPartialRetriever
  ) {

    def logSplunkEvent(dataEvent: DataEvent)(implicit hc: HeaderCarrier): Future[AuditResult] = {
      Future.successful(AuditResult.Success)
    }
  }

  class MockHomePageControllerCYEnabled @Inject()(bikListService: BikListService,
    pbikAppConfig: PbikAppConfig,
    authAction: AuthAction,
    noSessionCheckAction: NoSessionCheckAction,
    runModeConfiguration: Configuration,
    environment: Environment,
    taxDateUtils: TaxDateUtils,
    controllersReferenceData: ControllersReferenceData,
    splunkLogger: SplunkLogger,
    context: PbikContext,
    uRIInformation: URIInformation,
    externalURLs: ExternalUrls,
    localFormPartialRetriever: LocalFormPartialRetriever) extends MockHomePageController(bikListService,
      pbikAppConfig,
      authAction,
      noSessionCheckAction,
      runModeConfiguration,
      environment,
      taxDateUtils,
      controllersReferenceData,
      splunkLogger,
      context,
      uRIInformation,
      externalURLs,
      localFormPartialRetriever
  ) {
    when(pbikAppConfig.cyEnabled).thenReturn(true)
    when(pbikAppConfig.reportAProblemPartialUrl).thenReturn("")
  }

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
      val homePageController = app.injector.instanceOf[MockHomePageController]
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
        SessionKeys.sessionId -> "hackmeister",
        SessionKeys.token -> "RANDOMTOKEN",
        SessionKeys.userId -> userId)
      val result = homePageController.onPageLoad.apply(request)
      status(result) must be(UNAUTHORIZED) //401
    }

    "logout and redirect to feed back page" in {
      val homePageController = app.injector.instanceOf[MockHomePageController]
      val result = homePageController.signout.apply(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get must include("/feedback/PBIK")

    }
  }

  "When a valid user loads the CY warning but CY is disabled the HomePageController" should {
    "show the CY disabled error page" in {
      val homePageController = app.injector.instanceOf[MockHomePageController]
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      //        implicit val ac: AuthContext = createDummyUser("VALID_ID")
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
      val result = await(homePageController.loadCautionPageForCY.apply(request))
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.10003.1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.10003.2"))
    }
  }

  "When a valid user loads the CY warning page and CY mode is enabled the HomePageController" should {
    "show the page" in {
      val homePageController = app.injector.instanceOf[MockHomePageControllerCYEnabled]
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      //      implicit val ac: AuthContext = createDummyUser("VALID_ID")
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
      val result = await(homePageController.loadCautionPageForCY.apply(request))
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("AddBenefits.CY.Caution.Title"))
    }
  }

  "HomePageController" should {
    "display the navigation page" in {
      val homePageController = app.injector.instanceOf[MockHomePageController]
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
        SessionKeys.sessionId -> sessionId,
        SessionKeys.token -> "RANDOMTOKEN",
        SessionKeys.userId -> userId)
      implicit val timeout: akka.util.Timeout = timeoutValue
      val result = await(homePageController.onPageLoad(request))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("Overview.heading"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("Overview.next.heading", "" + YEAR_RANGE.cy, "" + YEAR_RANGE.cyplus1))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("Overview.table.heading.1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("Overview.current.heading", "" + YEAR_RANGE.cyminus1, "" + YEAR_RANGE.cy))
      result.body.asInstanceOf[Strict].data.utf8String must include("Help improve digital services by joining the HMRC user panel <span class=\"visuallyhidden\">(opens in new window)</span></a>")
      result.body.asInstanceOf[Strict].data.utf8String must include("No thanks")
    }
  }

}
