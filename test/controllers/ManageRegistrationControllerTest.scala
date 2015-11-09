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

import config.{PbikAppConfig, AppConfig}
import controllers.registration.ManageRegistrationController
import models._
import connectors.{HmrcTierConnector, TierConnector}
import org.mockito.Mockito._
import org.scalatest.{Matchers}
import org.scalatest.concurrent.ScalaFutures._
import org.mockito.Matchers.{eq => mockEq}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json
import play.api.libs.json.{JsValue}
import play.api.mvc.{Action, AnyContent, Result, Request}
import play.api.test.Helpers._
import play.filters.csrf.CSRF
import play.filters.csrf.CSRF.UnsignedTokenProvider
import play.twirl.api.{Html, HtmlFormat}
import services.{RegistrationService, BikListService}
import support.TestAuthUser
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{SessionKeys, HttpResponse}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec
import utils.BikListUtils.MandatoryRadioButton
import utils.FormMappingsConstants._
import utils._
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt}

class ManageRegistrationControllerTest extends UnitSpec with Matchers with FormMappings with TestAuthUser
   with FakePBIKApplication {


  implicit val ac = createDummyUser("testid")
  lazy val CYCache = List.tabulate(21)(n => new Bik("" + (n + 1), 10))
  lazy val CYRegistrationItems = List.tabulate(21)(n=> new RegistrationItem("" + (n + 1), true, true))
  val timeoutValue = 15 seconds

  def YEAR_RANGE:TaxYearRange = TaxDateUtils.getTaxYearRange()
  class StubBikListService extends BikListService  {
    override lazy val pbikAppConfig = mock[AppConfig]
    override val tierConnector = mock[HmrcTierConnector]
    lazy val CYCache = List.tabulate(21)(n => new Bik("" + (n + 1), 10))

    override def currentYearList(implicit ac: AuthContext, hc: HeaderCarrier, request: Request[_]):
        Future[(Map[String, String], List[Bik])] = {

      Future.successful((Map(HeaderTags.ETAG -> "1"), CYCache.filter { x: Bik => (Integer.parseInt(x.iabdType) <= 10) }))
    }

    override def nextYearList(implicit ac: AuthContext, hc: HeaderCarrier, request: Request[_]):
        Future[(Map[String, String], List[Bik])] = {

      Future.successful((Map(HeaderTags.ETAG -> "1"), CYCache.filter { x: Bik => (Integer.parseInt(x.iabdType) > 10) }))
    }

    when(pbikAppConfig.cyEnabled).thenReturn(true)

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      anyString, mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) <= 10)
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      anyString, mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) <= 10)
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      anyString, mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) <= 10)
    }))


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

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      anyString, mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) <= 10)
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      anyString, mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) <= 10)
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      anyString, mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      (Integer.parseInt(x.iabdType) <= 10)
    }))


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
                                                  List[RegistrationItem], List[Bik], List[Int]) => HtmlFormat.Appendable)
                                               (implicit hc:HeaderCarrier, request: Request[AnyContent], ac: AuthContext):
    Future[Result] = {
      year match {
        case dateRange.cyminus1 => {
          Future.successful(Ok(
            views.html.registration.currentTaxYear(mockFormRegistrationList,dateRange,mockRegistrationItemList,allRegisteredListOption,PbikAppConfig.biksNotSupported)
          ))
        }
        case _ => {
          Future.successful(Ok(
            views.html.registration.nextTaxYear(mockFormRegistrationList,true,dateRange,mockRegistrationItemList,registeredListOption,PbikAppConfig.biksNotSupported)
          ))
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
  }


  "When loading the next tax years on-remove data, the RegistrationController " should {
      "state the status is ok " in {
        running(fakeApplication) {
          val mockRegistrationController = new MockRegistrationController
          def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
          implicit val request = mockrequest
          implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
          implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
          val result = await(mockRegistrationController.loadNextTaxYearOnRemoveData(ac, mockrequest, hc))(timeout)
          status(result) shouldBe 200
        }
      }
    }

  "THe Registration Controller " should {
    "display the correct Biks on the removal screen " in {
      running(fakeApplication) {
        val mockRegistrationController = new MockRegistrationController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val request = mockrequest
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        val title = Messages("RemoveBenefits.Heading").substring(0,44)
        val testac = createDummyUser("testid")
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        val r = await(mockRegistrationController.loadNextTaxYearOnRemoveData(testac, mockrequest, hc))(timeout)

        bodyOf(r) should include(title)
        bodyOf(r) should include(Messages("BenefitInKind.label.15"))
        bodyOf(r) should include(Messages("BenefitInKind.label.16"))
        bodyOf(r) should include(Messages("BenefitInKind.label.17"))
        bodyOf(r) should include(Messages("BenefitInKind.label.18"))
        bodyOf(r) should include(Messages("BenefitInKind.label.19"))
        bodyOf(r) should include(Messages("BenefitInKind.label.20"))

      }
    }
  }


  "When instantiating the RegistrationController " in {
    running(fakeApplication) {
      val registrationController = ManageRegistrationController
      assert(registrationController.pbikAppConfig != null)
      assert(registrationController.tierConnector != null)
      assert(registrationController.bikListService != null)
      assert(registrationController.registrationService != null)
    }
  }

  "THe Registration Controller " should {
    "update the union of registered and new biks  and redirect to the what next page " in {
      running(fakeApplication) {
        val mockRegistrationController = new MockRegistrationController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val request = mockrequest
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        val title = Messages("whatNext.subHeading")
        val additions = CYCache.filter { x: Bik => (Integer.parseInt(x.iabdType) > 15) }
        val testac = createDummyUser("testid")
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        val r = await(mockRegistrationController.updateBiksFutureAction(2020, additions, true)(mockrequest, testac))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)

      }
    }
  }


  "When loading the currentTaxYearOnPageLoad, an authorised user " should {
    "be directed to cy page with list of biks " in {
      running(fakeApplication) {
        implicit val request = mockrequest
        val title = Messages("AddBenefits.Heading")
        val mockRegistrationController = new MockRegistrationController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        val r = await(mockRegistrationController.currentTaxYearOnPageLoad.apply(mockrequest))(timeout)

        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include(Messages("BenefitInKind.label.1"))
        bodyOf(r) should include(Messages("BenefitInKind.label.3"))

      }
    }

  }

  "When loading the nextTaxYearAddOnPageLoad, an authorised user " should {
    "be directed to cy + 1 page with list of biks" in {
      running(fakeApplication) {
        implicit val request = mockrequest
        val title = Messages("AddBenefits.Heading")
        val mockRegistrationController = new MockRegistrationController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        val r = await(mockRegistrationController.nextTaxYearAddOnPageLoad.apply(mockrequest))(timeout)

        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include(Messages("BenefitInKind.label.1"))
        bodyOf(r) should include(Messages("BenefitInKind.label.3"))
        bodyOf(r) should include(Messages("AddBenefits.ChooseBenefitsLabel.1", "" + YEAR_RANGE.cy, "" + YEAR_RANGE.cyplus1))
      }
    }
  }


  "When loading the nextTaxYearRemoveOnPageLoad, an authorised user " should {
    "be directed to the cy + 1 remove page with biks to remove " in {
      running(fakeApplication) {
        implicit val request = mockrequest
        val title = Messages("RemoveBenefits.Heading").substring(0, 10)
        val mockRegistrationController = new MockRegistrationController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        val r = await(mockRegistrationController.nextTaxYearRemoveOnPageLoad.apply(mockrequest))(timeout)

        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include(Messages("BenefitInKind.label.15"))
        bodyOf(r) should include(Messages("BenefitInKind.label.16"))
        bodyOf(r) should not include(Messages("ManagingRegistration.add.hint"))
        bodyOf(r) should not include(Messages("ManagingRegistration.cant.hint"))
      }
    }
  }

  "When loading the addOrRemovePageLoad, an authorised user " should {
    "be directed to options to add or remove page " in {
      running(fakeApplication) {
        implicit val request = mockrequest
        val title = Messages("RemoveBenefits.Heading").substring(0, 10)
        val mockRegistrationController = new MockRegistrationController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        val r = await(mockRegistrationController.nextTaxYearRemoveOnPageLoad.apply(mockrequest))(timeout)

        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include(Messages("BenefitInKind.label.15"))
        bodyOf(r) should include(Messages("BenefitInKind.label.16"))
      }
    }
  }

  "When loading the addOrRemoveDecision, an authorised user " should {
    "be directed to the login page " in {
      running(fakeApplication) {
        implicit val request = mockrequest
        val title = Messages("RemoveBenefits.Heading").substring(0, 10)
        val mockRegistrationController = new MockRegistrationController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        val r = await(mockRegistrationController.nextTaxYearRemoveOnPageLoad.apply(mockrequest))(timeout)

        status(r) shouldBe 200
        bodyOf(r) should include(title)
        bodyOf(r) should include(Messages("BenefitInKind.label.15"))
        bodyOf(r) should include(Messages("BenefitInKind.label.16"))

        //  redirectLocation(result) shouldBe Some("/payrollbik/sign-in?continue=/payrollbik/overview")
      }
    }
  }


  "When loading the confirmRemoveNextTaxYear, an authorised user " should {
    "be directed cy + 1 confirmation page to remove bik " in {
      running(fakeApplication) {
        implicit val request = mockrequest
        val title = Messages("RemoveBenefits.Heading").substring(0, 10)
        val mockRegistrationController = new MockRegistrationController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        val r = await(mockRegistrationController.confirmRemoveNextTaxYear.apply(mockrequest))(timeout)
        status(r) shouldBe 200
        bodyOf(r) should include(title)

        //  redirectLocation(result) shouldBe Some("/payrollbik/sign-in?continue=/payrollbik/overview")
      }
    }
  }

  "When loading the updateRegisteredBenefitTypes, an authorised user " should {
    "persist their changes and be redirected to the what next page " in {
      running(fakeApplication) {
        implicit val request = mockrequest
        val title = Messages("whatNext.subHeading")
        val mockRegistrationController = new MockRegistrationController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        val r = await(mockRegistrationController.updateRegisteredBenefitTypes.apply(mockrequest))(timeout)

        status(r) shouldBe 200
        bodyOf(r) should include(title)

      }
    }
  }

  "When loading the addNextYearRegisteredBenefitTypes, an unauthorised user " should {
    "be directed to the login page " in {
      running(fakeApplication) {
        implicit val request = mockrequest
        val title = Messages("whatNext.add.heading")
        val mockRegistrationController = new MockRegistrationController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        val r = await(mockRegistrationController.addNextYearRegisteredBenefitTypes.apply(noSessionIdRequest))(timeout)

        status(r) shouldBe UNAUTHORIZED
        bodyOf(r) should include("Request was not authenticated user should be redirected")

      }
    }
  }
//
  "When loading the removeNextYearRegisteredBenefitTypes, an unauthorised user " should {
    "be directed to the login page " in {
      running(fakeApplication) {
        implicit val request = mockrequest
        val title = Messages("whatNext.add.heading")
        val mockRegistrationController = new MockRegistrationController
        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
        val r = await(mockRegistrationController.removeNextYearRegisteredBenefitTypes.apply(noSessionIdRequest))(timeout)

        status(r) shouldBe UNAUTHORIZED
        bodyOf(r) should include("Request was not authenticated user should be redirected")

      }
    }
  }

//  "When Adding benefits to next tax year the page " should {
//    "contain a list of Biks we dont currently support " in {
//      running(fakeApplication) {
//        val mockRegistrationController = new MockRegistrationController
//        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
//        implicit val request = mockrequest
//        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
//        val r = await(mockRegistrationController.nextTaxYearAddOnPageLoad.apply(request))(timeout)
//        status(r) shouldBe 200
//        for ( x <- PbikAppConfig.biksNotSupported ) {
//          bodyOf(r) should include( Messages("BenefitInKind.label." + x) )
//        }
//      }
//    }
//  }



//  "When navigating with a CYP1 option the controller " should {
//    "redirect to the nextTaxYear page" in {
//      running(fakeApplication) {
//        val mockRegistrationController = new MockRegistrationController
//        def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
//        implicit val request = mockrequest
//        implicit val timeout : scala.concurrent.duration.Duration = timeoutValue
//        val formdata = navigationRadioButton.fill(BinaryRadioButton(Some("cyp1")))
//        route(FakeRequest())
//        val r = await(mockRegistrationController.manageRegistrationDecision.apply(request.withFormUrlEncodedBody("selectionValue" -> FormMappingsConstants.CYP1_ADD)))(timeout)
//        status(r) shouldBe 200
//        redirectLocation(r) shouldBe Some("/payrollbik/registration/next/add")
//      }
//    }
//  }

}
