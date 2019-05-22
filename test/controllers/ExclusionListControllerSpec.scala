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

import akka.util.Timeout
import config.{AppConfig, LocalFormPartialRetriever, PbikAppConfig, PbikContext}
import connectors.{FormPartialProvider, HmrcTierConnector}
import controllers.actions.{AuthAction, NoSessionCheckAction}
import javax.inject.Inject
import models._
import org.joda.time.LocalDate
import org.mockito.Matchers.{eq => mockEq}
import org.mockito.Mockito._
import org.scalatest.concurrent.Futures
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.{Application, Configuration, Environment}
import play.api.http.HttpEntity.Strict
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.{Crypto, json}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{BikListService, EiLListService}
import support.TestAuthUser
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.Exceptions.{InvalidBikTypeURIException, InvalidYearURIException}
import utils.{ControllersReferenceData, URIInformation, _}
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class ExclusionListControllerSpec extends PlaySpec with OneAppPerSuite with FakePBIKApplication
  with TestAuthUser with ControllersReferenceData {

  override lazy val pbikAppConfig: PbikAppConfig = mock[PbikAppConfig]
  val date = new LocalDate()
  val dateRange: TaxYearRange = if (date.getMonthOfYear < 4 || (date.getMonthOfYear == 4 && date.getDayOfMonth < 6)) {
    models.TaxYearRange(date.getYear - 1, date.getYear, date.getYear + 1)
  } else {
    models.TaxYearRange(date.getYear, date.getYear + 1, date.getYear + 2)
  }

  // implicit val user: AuthContext = createDummyUser("testid")
  lazy val ListOfPeople: List[EiLPerson] = List(EiLPerson("AA111111", "John", Some("Stones"), "Smith", Some("123"), Some("01/01/1980"), Some("male"), Some(10), 0),
    EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None, 0),
    EiLPerson("AC111111", "Humpty", Some("Alexander"), "Dumpty", Some("123"), Some("01/01/1980"), Some("male"), Some(10), 0),
    EiLPerson("AD111111", "Peter", Some("James"), "Johnson", None, None, None, None, 0),
    EiLPerson("AE111111", "Alice", Some("In"), "Wonderland", Some("123"), Some("03/02/1978"), Some("female"), Some(10), 0),
    EiLPerson("AF111111", "Humpty", Some("Alexander"), "Dumpty", Some("123"), Some("01/01/1980"), Some("male"), Some(10), 0))

  class FakeResponse extends HttpResponse {
    override def status = 200

    override val json: JsValue = Json.parse(
      """[
                 {
                     "nino": "AB111111",
                     "firstForename": "Adam",
                    "surname": "Smith",
                     "worksPayrollNumber": "ABC123",
                     "dateOfBirth": "01/01/1980",
                     "gender": "male",
                     "status": 0,
                     "perOptLock": 0
                 }
             ]""")
  }

  val tierConnector: HmrcTierConnector = mock[HmrcTierConnector]

  class StubEiLListService extends EiLListService(
    pbikAppConfig,
    tierConnector,
    app.injector.instanceOf[Configuration],
    app.injector.instanceOf[Environment],
    app.injector.instanceOf[URIInformation])
  {
      override def currentYearEiL(iabdType: String, year: Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[List[EiLPerson]] = {
        Future.successful(ListOfPeople)
      }
  }

  class StubEiLListServiceOneExclusion extends StubEiLListService {
    override def currentYearEiL(iabdType: String, year: Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[List[EiLPerson]] = {
      Future.successful(List(ListOfPeople.head))
    }
  }

  val uriInformation: URIInformation
  class StubBikListService extends BikListService(
    pbikAppConfig,
    tierConnector,
    app.injector.instanceOf[Configuration],
    app.injector.instanceOf[ControllersReferenceData],
    app.injector.instanceOf[Environment],
    app.injector.instanceOf[URIInformation]
  ) {

    lazy val CYCache: List[Bik] = List.range(3, 32).map(n => Bik("" + n, 10))
    /*(n => new Bik("" + (n + 1), 10))*/
    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(""),
      any[EmpRef], mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getBenefitTypesPath),
      mockEq(EmpRef.empty), mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getBenefitTypesPath),
      mockEq(EmpRef.empty), mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getBenefitTypesPath),
      mockEq(EmpRef.empty), mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier], any[Request[_]],
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

  }

  override lazy val pbikAppConfig: PbikAppConfig = mock[PbikAppConfig]
   val authenticate: AuthAction = new TestAuthAction
   val noSessionCheck: NoSessionCheckAction = new TestNoSessionCheckAction
  val uriInformation : URIInformation
  val controllersReferenceData : ControllersReferenceData


  class MockExclusionListController extends ExclusionListController(
    pbikAppConfig,
    authenticate,
    noSessionCheck,
    app.injector.instanceOf[EiLListService],
    app.injector.instanceOf[BikListService],
    tierConnector,
    app.injector.instanceOf[Configuration],
    app.injector.instanceOf[Environment],
    app.injector.instanceOf[PbikContext],
    app.injector.instanceOf[TaxDateUtils],
    app.injector.instanceOf[SplunkLogger],
    controllersReferenceData,
    uriInformation,
    app.injector.instanceOf[ExternalUrls],
    app.injector.instanceOf[LocalFormPartialRetriever]
  ) with Futures {

    implicit val defaultPatience: PatienceConfig =
      PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

     def logSplunkEvent(dataEvent: DataEvent)(implicit hc: HeaderCarrier): Future[AuditResult] = {
      Future.successful(AuditResult.Success)
    }

    override val tierConnector: HmrcTierConnector = mock[HmrcTierConnector]

    when(tierConnector.genericPostCall[EiLPerson](anyString, mockEq("31/exclusion/update"),
      any[EmpRef], anyInt, any)(any[HeaderCarrier], any[Request[_]],
      any[json.Format[EiLPerson]])).thenReturn(Future.successful(new FakeResponse()))

    when(tierConnector.genericPostCall[EiLPerson](anyString, mockEq("31/exclusion/remove"),
      any[EmpRef], anyInt, any)(any[HeaderCarrier], any[Request[_]],
      any[json.Format[EiLPerson]])).thenReturn(Future.successful(new FakeResponse()))

    when(tierConnector.genericPostCall(anyString, mockEq(uriInformation.updateBenefitTypesPath),
      any[EmpRef], anyInt, any)(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]])).thenReturn(Future.successful(new FakeResponse()))

    override val eiLListService: EiLListService = new StubEiLListService

    override val bikListService: BikListService = new StubBikListService

    override lazy val exclusionsAllowed = true

  }

  override val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .build()

  class MockExclusionsDisallowedController extends MockExclusionListController {
    override lazy val exclusionsAllowed = false
  }

  class StubNoRegisteredBikListService extends BikListService(
    pbikAppConfig,
    tierConnector,
    app.injector.instanceOf[Configuration],
    app.injector.instanceOf[ControllersReferenceData],
    app.injector.instanceOf[Environment],
    app.injector.instanceOf[URIInformation]) {

    lazy val CYCache: List[Bik] = List.tabulate(21)(n => Bik("" + (n + 1), 10))
    override val tierConnector: HmrcTierConnector = mock[HmrcTierConnector]

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], anyInt)(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) > 50
    }))
  }

  class MockNoRegisteredBiksExclusionListController extends MockExclusionListController {
    override val  bikListService: BikListService = new StubNoRegisteredBikListService
  }

  val exclusionListController : ExclusionListController = mock[ExclusionListController]

  "When instantiating the ExclusionListController the services" must {
    " should not be null" in {
      val mockExclusionListController = exclusionListController
      assert(mockExclusionListController.tierConnector != null)
      assert(mockExclusionListController.pbikAppConfig != null)
      assert(mockExclusionListController.eiLListService != null)
      assert(mockExclusionListController.bikListService != null)
    }
  }

  "When testing exclusions the exclusion functionality" must {
    " should be enabled" in {
      val mockExclusionListController = new MockExclusionListController
      assert(mockExclusionListController.exclusionsAllowed)
    }
  }

  "When testing exclusions the EILService" must {
    " should be defined" in {
      val mockExclusionListController = new MockExclusionListController
      assert(mockExclusionListController.eiLListService != null)
    }
  }

  "When testing exclusions the BIKService" must {
    " should be defined" in {
      val mockExclusionListController = new MockExclusionListController
      assert(mockExclusionListController.bikListService != null)
    }
  }

  "When mapping the CY string, the date returned by the controller" must {
    " be the first year in the CY pair (e.g CY in range 15/16-16/17 would be 15 )" in {
      val mockExclusionListController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val result: Int = await(mockExclusionListController.mapYearStringToInt("cy"))
      result must be(dateRange.cyminus1)
    }
  }

  "When mapping the CY+1 string, the date returned by the controller" must {
    " be the first year in the CYP1 pair (e.g CYP1 in range 15/16-16/17 would be 16 ) " in {
      val mockExclusionListController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val result = await(mockExclusionListController.mapYearStringToInt("cyp1"))
      result must be(dateRange.cy)
    }
  }

  "When mapping an unknown string, the controller" must {
    " throw an InvalidYearURIException" in {
      val mockExclusionListController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      intercept[InvalidYearURIException] {
        await(mockExclusionListController.mapYearStringToInt("ceeewhyploosWon"))
      }
    }
  }

  "When checking the Bik's IABD value is valid for CY the ExclusionListController" must {
    " return the start year of the CY pair, when the IABD value is valid" in {
      val mockExclusionListController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val result = await(mockExclusionListController.validateRequest("cy", "car"))
      result must be(dateRange.cyminus1)
    }
  }

  "When checking the Bik's IABD value is invalid for CY the ExclusionListController" must {
    " throw a InvalidBikTypeURIException" in {
      val mockExclusionListController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      intercept[InvalidBikTypeURIException] {
        await(mockExclusionListController.validateRequest("cy", "1"))
      }
    }
  }

  "When loading the performPageLoad, an unauthorised user" should {
    "see the users already excluded" in {
      val mockExclusionController = new MockExclusionListController
      implicit val timeout: Timeout = 10 seconds
      val result = await(mockExclusionController.performPageLoad("cy", "car").apply(mockrequest))(timeout)
      result.header.status must be(OK)

      //TODO: Work on this
      //result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ExclusionOverview.title"))
      //result.body.asInstanceOf[Strict].data.utf8String must include(Messages("AF111111"))
      //result.body.asInstanceOf[Strict].data.utf8String must include("Humpty Dumpty")
    }
  }

  "When loading the performPageLoad without nacigating from the overview page, an unauthorised user" should {
    "see the users already excluded" in {
      val mockExclusionController = new MockExclusionListController
      implicit val timeout: Timeout = 10 seconds
      val result = await(mockExclusionController.performPageLoad("cy", "car").apply(mockrequest))(timeout)
      result.header.status must be(OK)

      // TODO: Should work on this
      /*       result.body.asInstanceOf[Strict].data.utf8String must include("Humpty Dumpty")
             result.body.asInstanceOf[Strict].data.utf8String must include(Messages("Service.excludeanotheremployee"))
             result.body.asInstanceOf[Strict].data.utf8String must include("Humpty")
             result.body.asInstanceOf[Strict].data.utf8String must include("Alexander")
             result.body.asInstanceOf[Strict].data.utf8String must include("Dumpty")
             result.body.asInstanceOf[Strict].data.utf8String must include("123")
             result.body.asInstanceOf[Strict].data.utf8String must include("01/01/1980")*/
    }
  }

  "When loading the performPageLoad when exclusions are disallowed the controller" must {
    "show the restriction page" in {
      val mockExclusionController = new MockExclusionsDisallowedController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: akka.util.Timeout = 10 seconds
      val result = await(mockExclusionController.performPageLoad("cy", "car").apply(mockrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.10002"))
    }
  }

  "When loading the withOrWithoutNinoOnPageLoad the controller" must {
    "show the page in order to make a decision" in {
      val title = Messages("ExclusionNinoDecision.title").substring(0, 10)
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 10 seconds
      val result = await(mockExclusionController.withOrWithoutNinoOnPageLoad("cy", "car").apply(mockrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ExclusionNinoDecision.question").substring(0, 10))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("Service.yes"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("Service.no"))
    }
  }

  "When loading the withOrWithoutNinoOnPageLoad when exclusions feature is disabled the controller" must {
    "display the error page" in {
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = new MockExclusionsDisallowedController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 10 seconds
      val result = await(mockExclusionController.withOrWithoutNinoOnPageLoad("cy", "car").apply(mockrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When loading the withOrWithoutNinoDecision page with the form omitted, an authorised user" must {
    "see the page in order to confirm their decision" in {
      val title = Messages("ExclusionNinoDecision.title").substring(0, 10)
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      val result = await(mockExclusionController.withOrWithoutNinoDecision("cy", "car").apply(mockrequest))(5 seconds)
      result.header.status must be(303)
      val nextUrl = redirectLocation(Future(result)(scala.concurrent.ExecutionContext.Implicits.global)) match {
        case Some(s: String) => s
        case _ => ""
      }
      nextUrl must include("/exclude-employee-search")
    }
  }

  "When loading the withOrWithoutNinoDecision page when exclusions are disabled the controller" must {
    "show an error page" in {
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = new MockExclusionsDisallowedController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.withOrWithoutNinoDecision("cy", "car").apply(mockrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When loading the withOrWithoutNinoDecision page with a nino form, an authorised user" must {
    "see the page in order to confirm their decision" in {
      val title = Messages("ExclusionSearch.form.title")
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(
        "confirmation" -> FORM_TYPE_NINO
      )

      val result = await(mockExclusionController.withOrWithoutNinoDecision("cy", "car").apply(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include("National Insurance number")
    }
  }

  "When loading the withOrWithoutNinoDecision page with a non-nino form, an authorised user" must {
    "see the page in order to confirm their decision" in {
      val title = Messages("ExclusionSearch.form.title")
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(
        "confirmation" -> FORM_TYPE_NONINO
      )
      val result = await(mockExclusionController.withOrWithoutNinoDecision("cy", "car").apply(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include("dob")
    }
  }

  "When loading the searchResults page for an unpopulated NINO search, an authorised user" must {
    "see the NINO specific fields" in {
      val title = Messages("ExclusionSearch.form.title")
      val mockExclusionController = new MockExclusionListController

      def csrfToken = "csrfToken" -> Crypto.generateToken

      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 10 seconds
      val result = await(mockExclusionController.searchResults("cy", "car", controllersReferenceData.FORM_TYPE_NINO).apply(mockrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include("First name")
      result.body.asInstanceOf[Strict].data.utf8String must include("Last name")
      result.body.asInstanceOf[Strict].data.utf8String must include("National Insurance number")
    }
  }

  "When loading the searchResults page for an unpopulated non-NINO search, an authorised user" must {
    "see the NON-NINO specific fields" in {
      val title = Messages("ExclusionSearch.form.title")
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.searchResults("cy", "car", controllersReferenceData.FORM_TYPE_NONINO).apply(mockrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include("First name")
      result.body.asInstanceOf[Strict].data.utf8String must include("Last name")
      result.body.asInstanceOf[Strict].data.utf8String must include("Date of birth")
      result.body.asInstanceOf[Strict].data.utf8String must include("Gender")
    }
  }

  "When loading the searchResults page for a NINO search, an authorised user" must {
    "see the NON-NINO specific fields" in {
      val ninoSearchPerson = EiLPerson("AB111111", "Adam", None, "Smith", None, None, None, None, 0)
      val f = exclusionSearchFormWithNino.fill(ninoSearchPerson)
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.searchResults("cy", "car", controllersReferenceData.FORM_TYPE_NINO).apply(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include("Search for an employee")
      result.body.asInstanceOf[Strict].data.utf8String must include("Adam")
      result.body.asInstanceOf[Strict].data.utf8String must include("AB111111")
    }
  }

  "When loading the searchResults page for a non-NINO search, an authorised user" must {
    "see the NON-NINO specific fields" in {
      val ninoSearchPerson = EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None, 0)
      val f = exclusionSearchFormWithoutNino.fill(ninoSearchPerson)
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val mockExclusionController: MockExclusionListController = new MockExclusionListController {override val eiLListService = new StubEiLListServiceOneExclusion
      }
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.searchResults("cy", "car", controllersReferenceData.FORM_TYPE_NONINO).apply(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include("Search results")
      result.body.asInstanceOf[Strict].data.utf8String must include("Adam")
      result.body.asInstanceOf[Strict].data.utf8String must include("01/01/1980")
      result.body.asInstanceOf[Strict].data.utf8String must include("male")
    }
  }

  "When loading the searchResults page when exclusions are disabled, the controller" must {
    "show an error page" in {
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = new MockExclusionsDisallowedController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.searchResults("cy", "car", controllersReferenceData.FORM_TYPE_NONINO).apply(mockrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When updating exclusions, an authorised user" must {
    "see the page in order to review their result" in {
      val TEST_YEAR_CODE = "cy"
      val TEST_IABD_VALUE = "31"
      val FROM_OVERVIEW = "false"
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val title = Messages("whatNext.exclude.heading")
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.processExclusionForm(individualsForm.fill(EiLPersonList(ListOfPeople)), TEST_YEAR_CODE, TEST_IABD_VALUE, YEAR_RANGE))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When removing an excluded individual, with an error free form, an authorised user" must {
    "see the removal confirmation screen" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val TEST_IABD = "car"
      val TEST_YEAR_CODE = "cy"
      val title = Messages("ExclusionRemovalConfirmation.title")
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.processRemoval(individualsForm.fill(EiLPersonList(ListOfPeople)), TEST_YEAR_CODE, TEST_IABD, YEAR_RANGE))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ExclusionRemovalConfirmation.intro").substring(0, 31))
      result.body.asInstanceOf[Strict].data.utf8String must include("Humpty")
      result.body.asInstanceOf[Strict].data.utf8String must include("AB111111")
    }
  }

  "When confirming the removal of an excluded individual, an authorised user" must {
    "see the removal confirmation screen" in {
      val TEST_IABD = "31"
      val TEST_YEAR_CODE = "cyp1"
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val title = Messages("whatNext.rescind.heading")
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.processRemovalCommit(individualsForm
        .fill(EiLPersonList(ListOfPeople)), TEST_IABD, YEAR_RANGE)
      (hc, authenticatedRequest , context ))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  val taxDateUtils: TaxDateUtils

  "When validating a year the controller" must {
    "should return the current tax year if the validation passes for cy" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      implicit val timeout: Timeout = 10 seconds
      val result = await(mockExclusionController.validateRequest("cy", "car"))(timeout)
      result must be(taxDateUtils.getCurrentTaxYear())
    }

    "it should throw an InvalidBikTypeURIException if the Bik is not registered valid" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      intercept[InvalidBikTypeURIException] {
        await(mockExclusionController.validateRequest("cy", "1"))
      }
    }

  }

  "When remove exclusions are committed the controller" must {
    " show the what next page" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val f = individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("whatNext.rescind.heading")
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.removeExclusionsCommit(TEST_IABD)(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When remove Exclusions Commit when exclusions are disabled the controller" must {
    " should show an error page" in {
      val TEST_IABD = "car"
      val f = individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = new MockExclusionsDisallowedController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.removeExclusionsCommit(TEST_IABD).apply(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When remove is called the controller" must {
    " show the confirmation page" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val f = individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("ExclusionRemovalConfirmation.title")
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.remove(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When remove is called when exclusion mode is disabled the controller" must {
    " should show an error page" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val f = individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = new MockExclusionsDisallowedController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.remove(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When updateExclusions is called the controller" must {
    " redirect to the what next page" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val f = individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("whatNext.exclude.heading")
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.updateExclusions(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When updateExclusions is called but exclusions are disabled the controller" must {
    " redirect back to the overview page" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val f = individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = new MockExclusionsDisallowedController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.updateExclusions(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When updateMultipleExclusions is called the controller" must {
    " redirect to the what next page" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val f = individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("ExclusionSearch.title")
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.updateMultipleExclusions(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When updateMultipleExclusions is called but exclusions are disabled the controller" must {
    " redirect back to the overview page" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val f = individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = new MockExclusionsDisallowedController
      //UnsignedTokenProvider.generateToken
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.updateMultipleExclusions(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When updating individual exclusions, an authorised user" must {
    "see the page in order to review their result" in {
      val TEST_YEAR_CODE = "cy"
      val TEST_IABD_VALUE = "31"
      val FROM_OVERVIEW = "false"
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val title = Messages("whatNext.exclude.heading")
      val excludedText = Messages("whatNext.exclude.p1", "Exclusion complete")
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.processIndividualExclusionForm(individualsFormWithRadio.fill("", EiLPersonList(ListOfPeople)), TEST_YEAR_CODE, TEST_IABD_VALUE, YEAR_RANGE))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include(excludedText)
    }
  }

  // Tests below to check input validation
  "When updating exclusions," must {
    "an invalid input on first name" in {
      val TEST_EIL_PERSON: List[EiLPerson] = List(EiLPerson("AA111111", " ", Some("Stones"), "Smith", Some("123"), Some("01/01/1980"), Some("male"), Some(10), 0))
      val TEST_YEAR_CODE = "cy"
      val TEST_IABD_VALUE = "car"
      val f = individualsForm.fill(EiLPersonList(TEST_EIL_PERSON))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("ExclusionSearch.form.title")
      val excludedText = Messages("whatNext.exclude.p1")
      val mockExclusionController = new MockExclusionListController
      //UnsignedTokenProvider.generateToken
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.searchResults(TEST_YEAR_CODE, TEST_IABD_VALUE, "nino")(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include("Search for an employee to exclude")
    }
  }

}
