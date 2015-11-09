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

import config.{AppConfig}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.BikListUtils.MandatoryRadioButton
import utils._
import connectors.{TierConnector, HmrcTierConnector}
import models._
import org.scalatest.concurrent.Futures
import play.api.i18n.Messages
import play.api.libs.json.{Json, JsValue}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import org.mockito.Matchers.{eq => mockEq}
import org.mockito.Mockito._
import org.scalatest.Matchers
import play.api.libs.json
import play.api.mvc.{Action, Result, AnyContent, Request}
import play.api.test.Helpers._
import play.filters.csrf.CSRF
import play.filters.csrf.CSRF.UnsignedTokenProvider
import services.{BikListService, EiLListService}
import support.TestAuthUser
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.{HttpResponse}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec
import utils.TaxDateUtils
import utils.Exceptions.{InvalidBikTypeURIException, InvalidYearURIException}

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt}

class ExclusionListControllerTest extends UnitSpec with FakePBIKApplication with Matchers
                                                    with TestAuthUser with ControllersReferenceData {

  override lazy val pbikAppConfig = mock[AppConfig]
  val dateRange = TaxDateUtils.getTaxYearRange()
  implicit val user = createDummyUser("testid")
  lazy val ListOfPeople: List[EiLPerson] = List(EiLPerson("AA111111","John", Some("Stones") ,"Smith",Some("123"),Some("01/01/1980"),Some("male"), Some(10),0),
    EiLPerson("AB111111","Adam", None ,"Smith",None, Some("01/01/1980"),Some("male"), None, 0),
    EiLPerson("AC111111", "Humpty", Some("Alexander"),"Dumpty", Some("123"), Some("01/01/1980"),Some("male"), Some(10), 0),
    EiLPerson("AD111111", "Peter", Some("James"),"Johnson",None, None, None, None, 0),
    EiLPerson("AE111111", "Alice", Some("In") ,"Wonderland", Some("123"),Some("03/02/1978"), Some("female"), Some(10), 0),
    EiLPerson("AF111111", "Humpty", Some("Alexander"),"Dumpty", Some("123"), Some("01/01/1980"), Some("male"), Some(10), 0))


  class FakeResponse extends HttpResponse {
    override def status = 200
    override val json = Json.parse("""[
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

  class StubEiLListService extends EiLListService {
    override lazy val pbikAppConfig = mock[AppConfig]
    override val tierConnector = mock[HmrcTierConnector]
    override def currentYearEiL(iabdType: String, year: Int)(implicit ac: AuthContext, hc: HeaderCarrier, request: Request[_]): Future[List[EiLPerson]] = {
      Future.successful(ListOfPeople)
    }
  }

  class StubBikListService extends BikListService {
    override lazy val pbikAppConfig = mock[AppConfig]

    lazy val CYCache = List.range(3, 32).map(n => new Bik("" + n, 10))/*(n => new Bik("" + (n + 1), 10))*/
    override val tierConnector = mock[HmrcTierConnector]

    when(tierConnector.genericGetCall[List[Bik]]( anyString, anyString,
      anyString, mockEq(YEAR_RANGE.cy))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, anyString,
      anyString, mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, anyString,
      anyString, mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))


    when(tierConnector.genericGetCall[List[Bik]]( anyString, mockEq(""),
      anyString, mockEq(YEAR_RANGE.cy))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cy))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, anyString,
      anyString, mockEq(2020))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 5) }))

    when(tierConnector.genericPostCall(anyString, mockEq(updateBenefitTypesPath),
      anyString, anyInt, any)(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]])).thenReturn(Future.successful(new FakeResponse()))

    when(tierConnector.genericGetCall[List[Bik]](anyString,  mockEq(getRegisteredPath),
      anyString, anyInt)(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) >= 15) }))

  }

  class MockExclusionListController extends ExclusionListController with TierConnector with Futures {

    import org.scalatest.time.{Millis, Seconds, Span}
    override lazy val pbikAppConfig = mock[AppConfig]
    implicit val defaultPatience =
      PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

    override def logSplunkEvent(dataEvent:DataEvent)(implicit hc:HeaderCarrier, ac: AuthContext):Future[AuditResult] = {
      Future.successful(AuditResult.Success)
    }

    override val tierConnector: HmrcTierConnector = mock[HmrcTierConnector]

    when(tierConnector.genericPostCall[EiLPerson](anyString,  mockEq("31/exclusion/update"),
      anyString, anyInt, any)(any[HeaderCarrier],any[Request[_]],
        any[json.Format[EiLPerson]])).thenReturn(Future.successful(new FakeResponse()))

    when(tierConnector.genericPostCall[EiLPerson](anyString,  mockEq("31/exclusion/remove"),
      anyString, anyInt, any)(any[HeaderCarrier],any[Request[_]],
        any[json.Format[EiLPerson]])).thenReturn(Future.successful(new FakeResponse()))

    when(tierConnector.genericPostCall(anyString, mockEq(updateBenefitTypesPath),
      anyString, anyInt, any)(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]])).thenReturn(Future.successful(new FakeResponse()))

    override def eiLListService: EiLListService = new StubEiLListService
    override def bikListService: BikListService = new StubBikListService
    override lazy val exclusionsAllowed = true

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

  class MockExclusionsDisallowedController extends MockExclusionListController {
    override lazy val exclusionsAllowed = false
  }

  class StubNoRegisteredBikListService extends BikListService {
    override lazy val pbikAppConfig = mock[AppConfig]

    lazy val CYCache = List.tabulate(21)(n => new Bik("" + (n + 1), 10))
    override val tierConnector = mock[HmrcTierConnector]

    when(tierConnector.genericGetCall[List[Bik]]( anyString, anyString,
      anyString, anyInt)(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) > 50) }))
  }
  class MockNoRegisteredBiksExclusionListController extends MockExclusionListController {
    override def bikListService: BikListService = new StubNoRegisteredBikListService
  }

  "When instantiating the ExclusionListController the services " should {
    " should not be null " in {
      running(fakeApplication) {
        val mockExclusionListController = ExclusionListController
        assert(mockExclusionListController.tierConnector != null)
        assert(mockExclusionListController.pbikAppConfig != null)
        assert(mockExclusionListController.eiLListService != null)
        assert(mockExclusionListController.bikListService != null)
      }
    }
  }


  "When testing exclusions the exclusion functionality " should {
    " should be enabled " in {
      running(fakeApplication) {
        val mockExclusionListController = new MockExclusionListController
        assert(mockExclusionListController.exclusionsAllowed)
      }
    }
  }

  "When testing exclusions the EILService " should {
    " should be defined " in {
      running(fakeApplication) {
        val mockExclusionListController = new MockExclusionListController
        assert(mockExclusionListController.eiLListService != null)
      }
    }
  }

  "When testing exclusions the BIKService " should {
    " should be defined " in {
      running(fakeApplication) {
        val mockExclusionListController = new MockExclusionListController
        assert(mockExclusionListController.bikListService != null)
      }
    }
  }

  "When mapping the CY string, the date returned by the controller " should {
    " be the first year in the CY pair (e.g CY in range 15/16-16/17 would be 15 ) " in {
      running(fakeApplication) {
        val mockExclusionListController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        val result = await(mockExclusionListController.mapYearStringToInt("cy"))
        result shouldBe dateRange.cyminus1 // TODO confusing as it relates to the first year of the CY range which is
                                           // TODO cont cyminus1 to cy ( i.e 2015/2016 )
      }
    }
  }

  "When mapping the CY+1 string, the date returned by the controller " should {
    " be the first year in the CYP1 pair (e.g CYP1 in range 15/16-16/17 would be 16 )  " in {
      running(fakeApplication) {
        val mockExclusionListController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        val result = await(mockExclusionListController.mapYearStringToInt("cyp1"))
        result shouldBe dateRange.cy
      }
    }
  }

  "When mapping an unknown string, the controller " should {
    " throw an InvalidYearURIException " in {
      running(fakeApplication) {
        val mockExclusionListController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        intercept[InvalidYearURIException] {
          await(mockExclusionListController.mapYearStringToInt("ceeewhyploosWon"))
        }
      }
    }
  }

  "When checking the Bik's IABD value is valid for CY the ExclusionListController " should {
    " return the start year of the CY pair, when the IABD value is valid " in {
      running(fakeApplication) {
        val mockExclusionListController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val request = mockrequest
        val testac = createDummyUser("testid")
        assert(testac.principal.accounts.epaye.get.empRef.toString == "taxOfficeNumber/taxOfficeReference")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        val result = await(mockExclusionListController.validateRequest("cy", "car"))
        result shouldBe dateRange.cyminus1
      }
    }
  }

  "When checking the Bik's IABD value is invalid for CY the ExclusionListController " should {
    " throw a InvalidBikTypeURIException " in {
      running(fakeApplication) {
        val mockExclusionListController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val request = mockrequest
        val testac = createDummyUser("testid")
        assert(testac.principal.accounts.epaye.get.empRef.toString == "taxOfficeNumber/taxOfficeReference")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        intercept[InvalidBikTypeURIException] {
          await(mockExclusionListController.validateRequest("cy", "1"))
        }
      }
    }
  }


  "When loading the performPageLoad, an unauthorised user " should {
    "see the users already excluded " in {
      running(fakeApplication) {
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 10 seconds
        val r = await(mockExclusionController.performPageLoad("cy","car").apply(mockrequest))(timeout)
          status(r) shouldBe 200
          bodyOf(r) should include(Messages("ExclusionOverview.title"))
          bodyOf(r) should include(Messages("AF111111"))
          bodyOf(r) should include("Humpty Dumpty")

      }
    }
  }

  "When loading the performPageLoad without nacigating from the overview page, an unauthorised user " should {
    "see the users already excluded " in {
      running(fakeApplication) {
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 10 seconds
        val r = await(mockExclusionController.performPageLoad("cy","car").apply(mockrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(Messages("Service.excludeanotheremployee"))
        bodyOf(r) should include("Humpty")
        bodyOf(r) should include("Alexander")
        bodyOf(r) should include("Dumpty")
        bodyOf(r) should include("123")
        bodyOf(r) should include("01/01/1980")


      }
    }
  }

  "When loading the performPageLoad when exclusions are disallowed the controller " should {
    "show the restriction page " in {
      running(fakeApplication) {
        val mockExclusionController = new MockExclusionsDisallowedController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 10 seconds
        val r = await(mockExclusionController.performPageLoad("cy","car").apply(mockrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(Messages("ServiceMessage.10002"))
      }
    }
  }

  "When loading the withOrWithoutNinoOnPageLoad the controller " should {
    "show the page in order to make a decision " in {
      running(fakeApplication) {
        val title = Messages("ExclusionNinoDecision.title").substring(0, 10)
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 10 seconds
        val r = await(mockExclusionController.withOrWithoutNinoOnPageLoad("cy","car").apply(mockrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include(Messages("ExclusionNinoDecision.question").substring(0, 10))
        bodyOf(r) should include(Messages("Service.yes"))
        bodyOf(r) should include(Messages("Service.no"))
      }
    }
  }

  "When loading the withOrWithoutNinoOnPageLoad when exclusions feature is disabled the controller " should {
    "display the error page " in {
      running(fakeApplication) {
        val title = Messages("ServiceMessage.10002")
        val mockExclusionController = new MockExclusionsDisallowedController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 10 seconds
        val r = await(mockExclusionController.withOrWithoutNinoOnPageLoad("cy","car").apply(mockrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
      }
    }
  }

  "When loading the withOrWithoutNinoDecision page with the form omitted, an authorised user " should {
    "see the page in order to confirm their decision " in {
      running(fakeApplication) {
        val title = Messages("ExclusionNinoDecision.title").substring(0, 10)
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds
        val result = await(mockExclusionController.withOrWithoutNinoDecision("cy","car").apply(mockrequest))(timeout)
        /*status(result) shouldBe 200
        bodyOf(result) should include(Messages("ErrorPage.invalidForm"))*/
        status(result) shouldBe 303
        val nextUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }
        nextUrl should include("/exclude-employee-search")
      }
    }
  }

  "When loading the withOrWithoutNinoDecision page when exclusions are disabled the controller " should {
    "show an error page " in {
      running(fakeApplication) {
        val title = Messages("ServiceMessage.10002")
        val mockExclusionController = new MockExclusionsDisallowedController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.withOrWithoutNinoDecision("cy","car").apply(mockrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
      }
    }
  }

  "When loading the withOrWithoutNinoDecision page with a nino form, an authorised user " should {
    "see the page in order to confirm their decision " in {
      running(fakeApplication) {
        val title = Messages("ExclusionSearch.title")
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds

        implicit val formrequest = mockrequest.withFormUrlEncodedBody(
          "confirmation" -> FORM_TYPE_NINO
        )

        val r = await(mockExclusionController.withOrWithoutNinoDecision("cy","car").apply(formrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include("National Insurance number")
      }
    }
  }

  "When loading the withOrWithoutNinoDecision page with a non-nino form, an authorised user " should {
    "see the page in order to confirm their decision " in {
      running(fakeApplication) {
        val title = Messages("ExclusionSearch.title")
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds

        implicit val formrequest = mockrequest.withFormUrlEncodedBody(
          "confirmation" -> FORM_TYPE_NONINO
        )

        val r = await(mockExclusionController.withOrWithoutNinoDecision("cy","car").apply(formrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include("dob")
      }
    }
  }

  "When loading the searchResults page for an unpopulated NINO search, an authorised user " should {
    "see the NINO specific fields " in {
      running(fakeApplication) {
        val title = Messages("ExclusionSearch.title")
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 10 seconds
        val r = await(mockExclusionController.searchResults("cy","car", ExclusionListController.FORM_TYPE_NINO).apply(mockrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include("First name")
        bodyOf(r) should include("Last name")
        bodyOf(r) should include("National Insurance number")
      }
    }
  }

  "When loading the searchResults page for an unpopulated non-NINO search, an authorised user " should {
    "see the NON-NINO specific fields " in {
      running(fakeApplication) {
        val title = Messages("ExclusionSearch.title")
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.searchResults("cy","car", ExclusionListController.FORM_TYPE_NONINO).apply(mockrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include("First name")
        bodyOf(r) should include("Last name")
        bodyOf(r) should include("Date of birth")
        bodyOf(r) should include("Gender")
      }
    }
  }

  "When loading the searchResults page for a NINO search, an authorised user " should {
    "see the NON-NINO specific fields " in {
      running(fakeApplication) {
        val ninoSearchPerson =  EiLPerson("AB111111","Adam", None ,"Smith",None, None,None, None, 0)
        val f = exclusionSearchFormWithNino.fill(ninoSearchPerson)
        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)

        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.searchResults("cy","car", ExclusionListController.FORM_TYPE_NINO).apply(formrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include("Search results")
        bodyOf(r) should include("Adam")
        bodyOf(r) should include("AB111111")
      }
    }
  }

  "When loading the searchResults page for a non-NINO search, an authorised user " should {
    "see the NON-NINO specific fields " in {
      running(fakeApplication) {
        val ninoSearchPerson =  EiLPerson("AB111111","Adam", None ,"Smith",None, Some("01/01/1980"),Some("male"), None, 0)
        val f = exclusionSearchFormWithoutNino.fill(ninoSearchPerson)
        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)

        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.searchResults("cy","car", ExclusionListController.FORM_TYPE_NONINO).apply(formrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include("Search results")
        bodyOf(r) should include("Adam")
        bodyOf(r) should include("01/01/1980")
        bodyOf(r) should include("male")
      }
    }
  }

//  "When loading the searchResults page for a NINO search, an authorised user " should {
//    "see the NON-NINO specific fields " in {
//      running(fakeApplication) {
//        val ninoSearchPerson =  EiLPerson("AB111111","Adam", None ,"Smith",None, None,None, None, 0)
//        val f = exclusionSearchFormWithNino.fill(ninoSearchPerson)
//        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
//
//        val title = Messages("ExclusionRadioButtonSelectionConfirmation.title")
//        val mockExclusionController = new MockExclusionListController
//        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
//        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds
//        val r = await(mockExclusionController.searchResults("cy","15", ExclusionListController.FORM_TYPE_NINO).apply(formrequest))(timeout)
//        status(r) shouldBe 200
//        bodyOf(r) should include(title)
//        bodyOf(r) should include("Adam")
//        bodyOf(r) should include("AB111111")
//      }
//    }
//  }


  "When loading the searchResults page when exclusions are disabled, the controller " should {
    "show an error page " in {
      running(fakeApplication) {
        val title = Messages("ServiceMessage.10002")
        val mockExclusionController = new MockExclusionsDisallowedController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.searchResults("cy","car", ExclusionListController.FORM_TYPE_NONINO).apply(mockrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
      }
    }
  }

  "When updating exclusions, an authorised user " should {
    "see the page in order to review their result " in {
      running(fakeApplication) {

        val TEST_YEAR_CODE = "cy"
        val TEST_IABD_VALUE = "31"
        val FROM_OVERVIEW = "false"
        implicit val request = mockrequest
        val title = Messages("whatNext.exclude.heading")
        val excludedText = Messages("whatNext.exclude.p1")
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.processExclusionForm(individualsForm.fill(EiLPersonList(ListOfPeople)),TEST_YEAR_CODE, TEST_IABD_VALUE,YEAR_RANGE))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include(excludedText)
      }
    }
  }

  "When removing an excluded individual, with an error free form, an authorised user " should {
    "see the removal confirmation screen " in {
      running(fakeApplication) {
        implicit val request = mockrequest
        val TEST_IABD = "car"
        val TEST_YEAR_CODE = "cy"
        val title = Messages("ExclusionRemovalConfirmation.title")
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.processRemoval(
          individualsForm.fill(EiLPersonList(ListOfPeople)),TEST_YEAR_CODE,TEST_IABD,YEAR_RANGE))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include(Messages("ExclusionRemovalConfirmation.question").substring(0,20))
        bodyOf(r) should include("Humpty")
        bodyOf(r) should include("AB111111")
      }
    }
  }

  "When confirming the removal of an excluded individual, an authorised user " should {
    "see the removal confirmation screen " in {
      running(fakeApplication) {
        val TEST_IABD = "31"
        val TEST_YEAR_CODE = "cyp1"
        implicit val request = mockrequest
        val title = Messages("whatNext.rescind.heading")
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.processRemovalCommit(
          individualsForm.fill(EiLPersonList(ListOfPeople)), TEST_IABD,YEAR_RANGE))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
      }
    }
  }

    "When validating a year the controller " should {
      "should return the current tax year if the validation passes for cy  " in {
        running(fakeApplication) {
          implicit val request = mockrequest
          val mockExclusionController = new MockExclusionListController
          def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
          implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
          implicit val timeout: scala.concurrent.duration.Duration = 10 seconds
          val r = await(mockExclusionController.validateRequest("cy", "car"))(timeout)
          r shouldBe utils.TaxDateUtils.getCurrentTaxYear()
        }
      }

      "it should throw a if the Bik is not registered valid " in {
        running(fakeApplication) {
          implicit val request = mockrequest
          val mockExclusionController = new MockExclusionListController
          def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
          implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
          intercept[InvalidBikTypeURIException] {
            await(mockExclusionController.validateRequest("cy", "1"))
          }
        }
      }

    }

    "When initialising the ExclusionListController the exclusion variable " should {
      " be true " in {
        running(fakeApplication) {
          val cfg = new {
            val test = "ExclusionListConfigurationTest"
          } with ExclusionListConfiguration {
            assert(exclusionsAllowed)
          }
        }
      }
    }

    "When remove exclusions are committed the controller " should {
      " show the what next page " in {
        running(fakeApplication) {
          val TEST_YEAR_CODE = "cyp1"
          val TEST_IABD = "car"
          val f = individualsForm.fill(new EiLPersonList(ListOfPeople))
          implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
          val title = Messages("whatNext.rescind.heading")
          val mockExclusionController = new MockExclusionListController
          def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
          implicit val timeout: scala.concurrent.duration.Duration = 5 seconds
          val r = await(mockExclusionController.removeExclusionsCommit(TEST_IABD)(formrequest))(timeout)
          status(r) shouldBe 200
          bodyOf(r) should include(title)
        }
      }
    }

    "When remove Exclusions Commit when exclusions are disabled the controller " should {
      " should show an error page " in {
        running(fakeApplication) {
          val TEST_IABD = "car"
          val f = individualsForm.fill(new EiLPersonList(ListOfPeople))
          implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
          val title = Messages("ServiceMessage.10002")
          val mockExclusionController = new MockExclusionsDisallowedController
          def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
          implicit val timeout: scala.concurrent.duration.Duration = 5 seconds
          val r = await(mockExclusionController.removeExclusionsCommit(TEST_IABD).apply(formrequest))(timeout)
          status(r) shouldBe 200
          bodyOf(r) should include(title)
        }
      }
    }


    "When remove is called the controller " should {
      " show the confirmation page " in {
        running(fakeApplication) {
          val TEST_YEAR_CODE = "cyp1"
          val TEST_IABD = "car"
          val f = individualsForm.fill(new EiLPersonList(ListOfPeople))
          implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
          val title = Messages("ExclusionRemovalConfirmation.title")
          val mockExclusionController = new MockExclusionListController
          def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
          implicit val timeout: scala.concurrent.duration.Duration = 5 seconds
          val r = await(mockExclusionController.remove(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
          status(r) shouldBe 200
          bodyOf(r) should include(title)
        }
      }
    }

    "When remove is called when exclusion mode is disabled the controller " should {
      " should show an error page " in {
        running(fakeApplication) {
          val TEST_YEAR_CODE = "cyp1"
          val TEST_IABD = "car"
          val f = individualsForm.fill(new EiLPersonList(ListOfPeople))
          implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
          val title = Messages("ServiceMessage.10002")
          val mockExclusionController = new MockExclusionsDisallowedController
          def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
          implicit val timeout: scala.concurrent.duration.Duration = 5 seconds
          val r = await(mockExclusionController.remove(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
          status(r) shouldBe 200
          bodyOf(r) should include(title)
        }
      }
    }

  "When updateExclusions is called the controller " should {
    " redirect to the what next page " in {
      running(fakeApplication) {
        val TEST_YEAR_CODE = "cyp1"
        val TEST_IABD = "car"
        val f = individualsForm.fill(new EiLPersonList(ListOfPeople))
        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
        val title = Messages("whatNext.exclude.heading")
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout: scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.updateExclusions(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
      }
    }
  }

  "When updateExclusions is called but exclusions are disabled the controller " should {
    " redirect back to the overview page " in {
      running(fakeApplication) {
        val TEST_YEAR_CODE = "cyp1"
        val TEST_IABD = "car"
        val f = individualsForm.fill(new EiLPersonList(ListOfPeople))
        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
        val title = Messages("ServiceMessage.10002")
        val mockExclusionController = new MockExclusionsDisallowedController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout: scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.updateExclusions(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
      }
    }
  }

  "When updateMultipleExclusions is called the controller " should {
    " redirect to the what next page " in {
      running(fakeApplication) {
        val TEST_YEAR_CODE = "cyp1"
        val TEST_IABD = "car"
        val f = individualsForm.fill(new EiLPersonList(ListOfPeople))
        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
        val title = Messages("ExclusionSearch.title")
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout: scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.updateMultipleExclusions(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
      }
    }
  }

  "When updateMultipleExclusions is called but exclusions are disabled the controller " should {
    " redirect back to the overview page " in {
      running(fakeApplication) {
        val TEST_YEAR_CODE = "cyp1"
        val TEST_IABD = "car"
        val f = individualsForm.fill(new EiLPersonList(ListOfPeople))
        implicit val formrequest = mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
        val title = Messages("ServiceMessage.10002")
        val mockExclusionController = new MockExclusionsDisallowedController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout: scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.updateMultipleExclusions(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
      }
    }
  }

  "When updating individual exclusions, an authorised user " should {
    "see the page in order to review their result " in {
      running(fakeApplication) {

        val TEST_YEAR_CODE = "cy"
        val TEST_IABD_VALUE = "31"
        val FROM_OVERVIEW = "false"
        implicit val request = mockrequest
        val title = Messages("whatNext.exclude.heading")
        val excludedText = Messages("whatNext.exclude.p1")
        val mockExclusionController = new MockExclusionListController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        implicit val timeout : scala.concurrent.duration.Duration = 5 seconds
        val r = await(mockExclusionController.processIndividualExclusionForm(individualsFormWithRadio.fill("", EiLPersonList(ListOfPeople)),TEST_YEAR_CODE, TEST_IABD_VALUE,YEAR_RANGE))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include(excludedText)
      }
    }
  }
}
