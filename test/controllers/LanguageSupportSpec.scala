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
import controllers.registration.ManageRegistrationController
import javax.inject.Inject
import models._
import org.mockito.Matchers.{eq => mockEq}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures._
import org.scalatestplus.play.PlaySpec
import play.api.{Application, Configuration, Environment}
import play.api.data.Form
import play.api.http.HttpEntity.Strict
import play.api.i18n.Lang
import play.api.i18n.Messages.Implicits._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.{BikListService, RegistrationService}
import support.TestAuthUser
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionKeys}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.{TestAuthAction, TestNoSessionCheckAction, _}

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class LanguageSupportSpec @Inject()(taxDateUtils: TaxDateUtils) extends PlaySpec with FormMappings with TestAuthUser
  with FakePBIKApplication {

  override val fakeApplication: Application = GuiceApplicationBuilder()
    .overrides(bind[AppConfig].toInstance(mock[PbikAppConfig]))
    .overrides(bind[HmrcTierConnector].toInstance(mock[HmrcTierConnector]))
    .overrides(bind[BikListService].to(classOf[StubBikListService]))
    .overrides(bind[RegistrationService].to(classOf[StubbedRegistrationService]))
    .overrides(bind[AuthAction].to(classOf[TestAuthAction]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .build()


//  implicit val ac: AuthContext = createDummyUser("testid")
  lazy val CYCache: List[Bik] = List.tabulate(21)(n => Bik("" + (n + 1), 10))
  lazy val CYRegistrationItems: List[RegistrationItem] = List.tabulate(21)(n => RegistrationItem("" + (n + 1), active = true, enabled = true))
  val timeoutValue: FiniteDuration = 15 seconds

  def YEAR_RANGE: TaxYearRange = taxDateUtils.getTaxYearRange()

  class StubBikListService @Inject()(pbikAppConfig: AppConfig,
                                     tierConnector: HmrcTierConnector,
                                     runModeConfiguration: Configuration,
                                     controllersReferenceData: ControllersReferenceData,
                                     environment: Environment,
                                     uriInformation: URIInformation) extends BikListService (
  pbikAppConfig,
  tierConnector,
  runModeConfiguration,
  controllersReferenceData,
  environment,
  uriInformation
  ) {

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
      Future(CYCache)(scala.concurrent.ExecutionContext.Implicits.global)
    }

  }

  class FakeResponse extends HttpResponse {
    override def status = 200
  }

  class StubbedRegistrationService @Inject()(pbikAppConfig: PbikAppConfig,
                                             tierConnector: HmrcTierConnector,
                                             bikListService: BikListService,
                                             runModeConfiguration: Configuration,
                                             environment: Environment,
                                             taxDateUtils: TaxDateUtils,
                                             context: PbikContext,
                                             controllersReferenceData: ControllersReferenceData,
                                             uRIInformation: URIInformation,
                                             externalURLs: ExternalUrls,
                                             localFormPartialRetriever: LocalFormPartialRetriever) extends RegistrationService(

    tierConnector,
    bikListService,
    runModeConfiguration,
    environment,
    taxDateUtils,
    controllersReferenceData,
    uRIInformation)(
    pbikAppConfig,
    context,
    externalURLs,
    localFormPartialRetriever
  ) {

    val dateRange: TaxYearRange = taxDateUtils.getTaxYearRange()

    val registeredListOption = List.empty[Bik]
    val allRegisteredListOption: List[Bik] = CYCache
    val mockRegistrationItemList = List.empty[RegistrationItem]
    val mockFormRegistrationList: Form[RegistrationList] = objSelectedForm.fill(RegistrationList(None, CYRegistrationItems))


    override def generateViewForBikRegistrationSelection(year: Int, cachingSuffix: String,
                                                         generateViewBasedOnFormItems: (Form[RegistrationList],
                                                           List[RegistrationItem], List[Bik], List[Int], List[Int], Option[Int]) => HtmlFormat.Appendable)
                                                        (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]):
    Future[Result] = {
      year match {
        case dateRange.cyminus1 => {
          Future.successful(Ok(views.html.registration.currentTaxYear(mockFormRegistrationList, dateRange, mockRegistrationItemList, allRegisteredListOption, nonLegislationBiks = List(0), decommissionedBiks = List(0), biksAvailableCount = Some(17), empRef = request.empRef)))
        }
        case _ => {
          Future.successful(Ok(views.html.registration.nextTaxYear(mockFormRegistrationList, additive = true, dateRange, mockRegistrationItemList, registeredListOption, nonLegislationBiks = List(0), decommissionedBiks = List(0), biksAvailableCount = Some(17), empRef = request.empRef))
          )
        }
      }
    }

  }

  class MockHomePageController @Inject()(bikListService: BikListService,
                                         authenticate: AuthAction,
                                         noSessionCheck: NoSessionCheckAction,
                                         pbikAppConfig: PbikAppConfig,
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
    authenticate,
    noSessionCheck,
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
  )

  class MockRegistrationController @Inject()(pbikAppConfig: PbikAppConfig,
                                             registrationService: RegistrationService,
                                             bikListService: BikListService,
                                             tierConnector: HmrcTierConnector,
                                             authenticate: AuthAction,
                                             noSessionCheck: NoSessionCheckAction,
                                             runModeConfiguration: Configuration,
                                             environment: Environment,
                                             taxDateUtils: TaxDateUtils,
                                             context: PbikContext,
                                             whatNextPageController: WhatNextPageController,
                                             controllersReferenceData: ControllersReferenceData,
                                             splunkLogger: SplunkLogger,
                                             uriInformation: URIInformation,
                                             externalURLs: ExternalUrls,
                                             localFormPartialRetriever: LocalFormPartialRetriever) extends ManageRegistrationController(

    registrationService,
    bikListService,
    tierConnector,
    authenticate,
    noSessionCheck,
    runModeConfiguration,
    environment,
    taxDateUtils,
    whatNextPageController,
    controllersReferenceData,
    splunkLogger)(
    pbikAppConfig,
    context,
    uriInformation,
    externalURLs,
    localFormPartialRetriever
  ) {

    import org.scalatest.time.{Millis, Seconds, Span}

    implicit val defaultPatience: ScalaFutures.PatienceConfig =
      PatienceConfig(timeout = Span(7, Seconds), interval = Span(600, Millis))


    implicit val mr: FakeRequest[AnyContentAsEmpty.type] = mockrequest


    val dateRange: TaxYearRange = taxDateUtils.getTaxYearRange()

    when(pbikAppConfig.cyEnabled).thenReturn(true)

    when(pbikAppConfig.reportAProblemPartialUrl).thenReturn("")

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(""),
      any[EmpRef], mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getBenefitTypesPath),
      EmpRef("", ""), mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getBenefitTypesPath),
      EmpRef("", ""), mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getBenefitTypesPath),
      EmpRef("", ""), mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], mockEq(2020))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 5
    }))

    when(tierConnector.genericPostCall(anyString, mockEq(uriInformation.updateBenefitTypesPath),
      any[EmpRef], anyInt, any)(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]])).thenReturn(Future.successful(new FakeResponse()))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getRegisteredPath),
      any[EmpRef], anyInt)(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) >= 15
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getBenefitTypesPath),
      EmpRef("", ""), mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))
  }

  "The Homepage Controller" should {
    "set the request language and redirect to the homepage" in {
      val mockController = app.injector.instanceOf[MockHomePageController]
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockWelshrequest
//      implicit val ac: AuthContext = createDummyUser("VALID_ID")
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
      val additions = CYCache.filter { x: Bik => Integer.parseInt(x.iabdType) > 15 }
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(mockController.setLanguage.apply(request))(timeout)
      result.header.status must be(SEE_OTHER) // 303
    }
  }

  "HomePageController" should {
    "display the navigation page" in {
      val homePageController = app.injector.instanceOf[MockHomePageController]
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
        SessionKeys.sessionId -> sessionId,
        SessionKeys.token -> "RANDOMTOKEN",
        SessionKeys.userId -> userId).withCookies(Cookie("PLAY_LANG", "cy"))
      implicit val timeout: FiniteDuration = timeoutValue
      implicit val lang: Lang = new Lang("cy")
      val result = await(homePageController.onPageLoad.apply(request))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include("Cyfeirnod TWE y cyflogwr")
    }
  }

}
