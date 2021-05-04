/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.HmrcTierConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.HttpEntity.Strict
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.i18n.Messages.Implicits._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.libs.crypto.CSRFTokenSigner
import play.api.libs.json
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{EiLListService, SessionService}
import support._
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import utils.Exceptions.{InvalidBikTypeURIException, InvalidYearURIException}
import utils.{ControllersReferenceData, URIInformation, _}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class ExclusionListControllerSpec extends PlaySpec with FakePBIKApplication with TestAuthUser {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .overrides(bind[AuthAction].to(classOf[TestAuthAction]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .overrides(bind[EiLListService].to(classOf[StubEiLListService]))
    .overrides(bind[HmrcTierConnector].toInstance(mock(classOf[HmrcTierConnector])))
    .overrides(bind[SessionService].toInstance(mock(classOf[SessionService])))
    .build()

  implicit val lang = Lang("en-GB")

  val controllersReferenceData: ControllersReferenceData = app.injector.instanceOf[ControllersReferenceData]
  val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]
  val date = new LocalDate()
  val dateRange: TaxYearRange = if (date.getMonthOfYear < 4 || (date.getMonthOfYear == 4 && date.getDayOfMonth < 6)) {
    models.TaxYearRange(date.getYear - 1, date.getYear, date.getYear + 1)
  } else {
    models.TaxYearRange(date.getYear, date.getYear + 1, date.getYear + 2)
  }

  lazy val ListOfPeople: List[EiLPerson] = List(
    EiLPerson("AA111111", "John", Some("Stones"), "Smith", Some("123"), Some("01/01/1980"), Some("male"), Some(10), 0),
    EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None, 0),
    EiLPerson(
      "AC111111",
      "Humpty",
      Some("Alexander"),
      "Dumpty",
      Some("123"),
      Some("01/01/1980"),
      Some("male"),
      Some(10),
      0),
    EiLPerson("AD111111", "Peter", Some("James"), "Johnson", None, None, None, None, 0),
    EiLPerson(
      "AE111111",
      "Alice",
      Some("In"),
      "Wonderland",
      Some("123"),
      Some("03/02/1978"),
      Some("female"),
      Some(10),
      0),
    EiLPerson(
      "AF111111",
      "Humpty",
      Some("Alexander"),
      "Dumpty",
      Some("123"),
      Some("01/01/1980"),
      Some("male"),
      Some(10),
      0)
  )

  val mockExclusionListController: MockExclusionListController = {
    val melc: MockExclusionListController = app.injector.instanceOf[MockExclusionListController]

    lazy val CYCache: List[Bik] = List.range(3, 32).map(n => Bik("" + n, 10))

    when(
      melc.tierConnector.genericGetCall[List[Bik]](
        any[String],
        any[String],
        any[EmpRef],
        argEq(controllersReferenceData.YEAR_RANGE.cy))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      melc.tierConnector.genericGetCall[List[Bik]](
        any[String],
        any[String],
        any[EmpRef],
        argEq(controllersReferenceData.YEAR_RANGE.cyminus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      melc.tierConnector.genericGetCall[List[Bik]](
        any[String],
        any[String],
        any[EmpRef],
        argEq(controllersReferenceData.YEAR_RANGE.cyplus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      melc.tierConnector
        .genericGetCall[List[Bik]](any[String], argEq(""), any[EmpRef], argEq(controllersReferenceData.YEAR_RANGE.cy))(
          any[HeaderCarrier],
          any[Request[_]],
          any[json.Format[List[Bik]]],
          any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      melc.tierConnector.genericGetCall[List[Bik]](
        any[String],
        argEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
        argEq(EmpRef.empty),
        argEq(controllersReferenceData.YEAR_RANGE.cy)
      )(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]], any[Manifest[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) <= 10
      }))

    when(
      melc.tierConnector.genericGetCall[List[Bik]](
        any[String],
        argEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
        argEq(EmpRef.empty),
        argEq(controllersReferenceData.YEAR_RANGE.cyminus1)
      )(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]], any[Manifest[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) <= 10
      }))

    when(
      melc.tierConnector.genericGetCall[List[Bik]](
        any[String],
        argEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
        argEq(EmpRef.empty),
        argEq(controllersReferenceData.YEAR_RANGE.cyplus1)
      )(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]], any[Manifest[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) <= 10
      }))

    when(
      melc.tierConnector.genericGetCall[List[Bik]](any[String], any[String], any[EmpRef], argEq(2020))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 5
    }))

    when(
      melc.tierConnector.genericPostCall(
        any[String],
        argEq(app.injector.instanceOf[URIInformation].updateBenefitTypesPath),
        any[EmpRef],
        any[Int],
        any)(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]]))
      .thenReturn(Future.successful(new FakeResponse()))

    when(
      melc.tierConnector.genericGetCall[List[Bik]](
        any[String],
        argEq(app.injector.instanceOf[URIInformation].getRegisteredPath),
        any[EmpRef],
        any[Int])(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]], any[Manifest[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) >= 15
      }))

    when(melc.cachingService.cacheEiLPerson(any())(any[HeaderCarrier])).thenReturn(
      Future.successful(None)
    )

    melc
  }

  "When testing exclusions the exclusion functionality" must {
    "should be enabled" in {
      assert(mockExclusionListController.exclusionsAllowed)
    }
  }

  "When mapping the CY string, the date returned by the controller" must {
    "be the first year in the CY pair (e.g CY in range 15/16-16/17 would be 15 )" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val result: Int = await(mockExclusionListController.mapYearStringToInt("cy"))
      result must be(dateRange.cyminus1)
    }
  }

  "When mapping the CY+1 string, the date returned by the controller" must {
    "be the first year in the CYP1 pair (e.g CYP1 in range 15/16-16/17 would be 16 ) " in {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val result = await(mockExclusionListController.mapYearStringToInt("cyp1"))
      result must be(dateRange.cy)
    }
  }

  "When mapping an unknown string, the controller" must {
    "throw an InvalidYearURIException" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      intercept[InvalidYearURIException] {
        await(mockExclusionListController.mapYearStringToInt("ceeewhyploosWon"))
      }
    }
  }

  "When checking the Bik's IABD value is valid for CY the ExclusionListController" must {
    "return the start year of the CY pair, when the IABD value is valid" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val result = await(mockExclusionListController.validateRequest("cy", "car"))
      result must be(dateRange.cyminus1)
    }
  }

  "When checking the Bik's IABD value is invalid for CY the ExclusionListController" must {
    "throw a InvalidBikTypeURIException" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      intercept[InvalidBikTypeURIException] {
        await(mockExclusionListController.validateRequest("cy", "1"))
      }
    }
  }

  "When loading the performPageLoad, an unauthorised user" should {
    "be taken to an error page" in {
      implicit val timeout: Timeout = 10 seconds
      val result = await(mockExclusionListController.performPageLoad("cy", "car").apply(mockrequest))(timeout)
      result.header.status must be(INTERNAL_SERVER_ERROR)
    }
  }

  "When loading the performPageLoad without navigating from the overview page, an unauthorised user" should {
    "be taken to an error page" in {
      implicit val timeout: Timeout = 10 seconds
      val result = await(mockExclusionListController.performPageLoad("cy", "car").apply(mockrequest))(timeout)
      result.header.status must be(INTERNAL_SERVER_ERROR)
    }
  }

  "When loading the performPageLoad when exclusions are disallowed the controller" must {
    "show the restriction page" in {
      val mockExclusionController = app.injector.instanceOf[MockExclusionsDisallowedController]

      implicit val timeout: akka.util.Timeout = 10 seconds
      val result = await(mockExclusionController.performPageLoad("cy", "car").apply(mockrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.10002"))
    }
  }

  "When loading the withOrWithoutNinoOnPageLoad the controller" must {
    "show the page in order to make a decision" in {
      val title = Messages("ExclusionNinoDecision.title").substring(0, 10)
      implicit val timeout: Timeout = 10 seconds
      val result =
        await(mockExclusionListController.withOrWithoutNinoOnPageLoad("cy", "car").apply(mockrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include(
        Messages("ExclusionNinoDecision.question").substring(0, 10))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("Service.yes"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("Service.no"))
    }
  }

  "When loading the withOrWithoutNinoOnPageLoad when exclusions feature is disabled the controller" must {
    "display the error page" in {
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = app.injector.instanceOf[MockExclusionsDisallowedController]
      implicit val timeout: Timeout = 10 seconds
      val result = await(mockExclusionController.withOrWithoutNinoOnPageLoad("cy", "car").apply(mockrequest))(timeout)
      result.header.status must be(FORBIDDEN)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When loading the withOrWithoutNinoDecision page with the form omitted, an authorised user" must {
    "see the page in order to confirm their decision" in {
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(PbikSession(
          None,
          None,
          Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None))),
          Some(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None)),
          Some(List(EiLPerson("AA111111B", "Jane", None, "Doe", Some("456"), None, None, None))),
          None,
          None
        ))))
      val title = Messages("ExclusionNinoDecision.title")
      val message = Messages("ExclusionNinoDecision.question")
      val result = mockExclusionListController
        .withOrWithoutNinoDecision("cyp1", "car")
        .apply(mockrequest)

      status(result) must be(OK)
      contentAsString(result) must include(title)
      contentAsString(result) must include(message)
    }
  }

  "When loading the withOrWithoutNinoDecision page when exclusions are disabled the controller" must {
    "show an error page" in {
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = app.injector.instanceOf[MockExclusionsDisallowedController]
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.withOrWithoutNinoDecision("cy1", "car").apply(mockrequest))(timeout)
      result.header.status must be(FORBIDDEN)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When loading the withOrWithoutNinoDecision with valid form selecting nino" must {
    "proceed to nino form page" in {
      lazy val formData =
        controllersReferenceData.binaryRadioButton.fill(MandatoryRadioButton("nino"))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
      val result = mockExclusionListController
        .withOrWithoutNinoDecision("cyp1", "car")
        .apply(formrequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be("/payrollbik/cyp1/car/nino/exclude-employee-form")
    }
  }

  "When loading the withOrWithoutNinoDecision with valid form selecting no-nino" must {
    "proceed to no-nino form page" in {
      lazy val formData =
        controllersReferenceData.binaryRadioButton.fill(MandatoryRadioButton("no-nino"))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
      val result = mockExclusionListController
        .withOrWithoutNinoDecision("cyp1", "car")
        .apply(formrequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be("/payrollbik/cyp1/car/no-nino/exclude-employee-form")
    }
  }

  "When loading showExclusionSearchForm, an authorised user" must {
    "see the nino form in order to search for a person" in {
      val formType = "nino"
      val title = Messages("ExclusionSearch.form.title")
      val ninoHint = Messages("Service.field.ninohint")
      val result = mockExclusionListController.showExclusionSearchForm("cyp1", "car", formType).apply(mockrequest)

      status(result) must be(OK)
      contentAsString(result) must include(title)
      contentAsString(result) must include(ninoHint)
    }

    "see the no-nino form in order to search for a person" in {
      val formType = "no-nino"
      val title = Messages("ExclusionSearch.form.title")
      val dobHint = Messages("Service.field.dobhint")
      val result = mockExclusionListController.showExclusionSearchForm("cyp1", "car", formType).apply(mockrequest)

      status(result) must be(OK)
      contentAsString(result) must include(title)
      contentAsString(result) must include(dobHint)
    }

    "see an error page if neither nino or no-nino are chosen" in {
      val formType = "nothing"
      val title = Messages("ErrorPage.invalidForm")
      val result = mockExclusionListController.showExclusionSearchForm("cyp1", "car", formType).apply(mockrequest)

      status(result) must be(INTERNAL_SERVER_ERROR)
      contentAsString(result) must include(title)
    }

  }

  "When loading the searchResults page for an unpopulated NINO search, an authorised user" must {
    "see the NINO specific fields" in {
      val title = Messages("ExclusionSearch.form.title")
      implicit val timeout: Timeout = 10 seconds
      val result = await(
        mockExclusionListController
          .searchResults("cy", "car", ControllersReferenceDataCodes.FORM_TYPE_NINO)
          .apply(mockrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include("First name")
      result.body.asInstanceOf[Strict].data.utf8String must include("Last name")
      result.body.asInstanceOf[Strict].data.utf8String must include("National Insurance number")
    }
  }

  "When loading the searchResults page for an unpopulated non-NINO search, an authorised user" must {
    "see the NO-NINO specific fields" in {
      val title = Messages("ExclusionSearch.form.title")
      implicit val timeout: Timeout = 5 seconds
      val result = await(
        mockExclusionListController
          .searchResults("cy", "car", ControllersReferenceDataCodes.FORM_TYPE_NONINO)
          .apply(mockrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include("First name")
      result.body.asInstanceOf[Strict].data.utf8String must include("Last name")
      result.body.asInstanceOf[Strict].data.utf8String must include("Date of birth")
      result.body.asInstanceOf[Strict].data.utf8String must include("Gender")
    }
  }

  "When loading the searchResults page for a NINO search, an authorised user" must {
    "see the expected search results page" in {
      when(mockExclusionListController.cachingService.cacheListOfMatches(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                None,
                None,
                Some(List(EiLPerson("", "", None, "", None, None, None, None))),
                None,
                None,
                None,
                None))))
      lazy val ninoSearchPerson = EiLPerson("AB111111", "Adam", None, "Smith", None, None, None, None, 0)
      lazy val formData =
        controllersReferenceData.exclusionSearchFormWithNino(request = mockrequest).fill(ninoSearchPerson)
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
      val result = mockExclusionListController
        .searchResults("cyp1", "car", ControllersReferenceDataCodes.FORM_TYPE_NINO)
        .apply(formrequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be("/payrollbik/cyp1/car/nino/exclude-employee-results")

    }
  }

  "When loading the searchResults page for a non-NINO search, an authorised user" must {
    "see the expected search results page" in {

      val noNinoSearchPerson =
        EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None, 0)
      val formData =
        controllersReferenceData.exclusionSearchFormWithoutNino(request = mockrequest).fill(noNinoSearchPerson)
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
      val result = mockExclusionListController
        .searchResults("cyp1", "car", ControllersReferenceDataCodes.FORM_TYPE_NONINO)
        .apply(formrequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be("/payrollbik/cyp1/car/no-nino/exclude-employee-results")
    }
  }

  "When loading the searchResults page when exclusions are disabled, the controller" must {
    "show an error page" in {
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = app.injector.instanceOf[MockExclusionsDisallowedController]
      implicit val timeout: Timeout = 5 seconds
      val result = await(
        mockExclusionController
          .searchResults("cy", "car", ControllersReferenceDataCodes.FORM_TYPE_NONINO)
          .apply(mockrequest))(timeout)
      result.header.status must be(FORBIDDEN)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When handling a valid search result, the controller" must {
    "show an error page if the list of matches contains only already excluded individuals" in {
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(PbikSession(
          None,
          None,
          Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None))),
          None,
          Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None))),
          None,
          None
        ))))
      val result = mockExclusionListController.showResults("cyp1", "car", "nino").apply(mockrequest)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("ServiceMessage.63085.h1"))
    }
  }

  "When handling a valid search result, the controller" must {
    "show an error page if the list of matches is empty" in {
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                None,
                None,
                None,
                None,
                Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None))),
                None,
                None
              ))))
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      val result = Future.successful(
        mockExclusionListController
          .searchResultsHandleValidResult(List.empty[EiLPerson], "car", "nino", "31", List.empty[EiLPerson]))
      status(result) must be(NOT_FOUND)
      contentAsString(result) must include(Messages("ServiceMessage.65127.h1"))
    }
  }

  "When loading the searchResults page, the controller" must {
    "show the search results if they are present in the cache" in {
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(PbikSession(
          None,
          None,
          Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None))),
          Some(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None)),
          Some(List(EiLPerson("AA111111B", "Jane", None, "Doe", Some("456"), None, None, None))),
          None,
          None
        ))))
      val result = mockExclusionListController.showResults("cyp1", "car", "nino").apply(mockrequest)
      status(result) must be(OK)
      contentAsString(result) must include("AA111111")
      contentAsString(result) must include("John")
      contentAsString(result) must include("Smith")
    }
  }

  "When loading the searchResults page directly, the controller" must {
    "show an error page if no results are present in the cache" in {
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                None,
                None,
                None,
                None,
                None,
                None,
                None
              ))))
      val result = mockExclusionListController.showResults("cyp1", "car", "nino").apply(mockrequest)
      status(result) must be(NOT_FOUND)
      contentAsString(result) must include("We’re sorry a technical error has occurred")
    }
  }

  "When updating exclusions, an authorised user" must {
    "see the page in order to review their result" in {
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(PbikSession(
          None,
          None,
          Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None))),
          Some(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None)),
          Some(List(EiLPerson("AA111111B", "Jane", None, "Doe", Some("456"), None, None, None))),
          None,
          None
        ))))
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val result = mockExclusionListController.updateExclusions(TEST_YEAR_CODE, TEST_IABD).apply(mockrequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be(s"/payrollbik/$TEST_YEAR_CODE/$TEST_IABD/exclude-confirmation")
    }
  }

  "When removing an excluded individual, with an error free form, an authorised user" must {
    "see the removal confirmation screen" in {
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(PbikSession(
          None,
          None,
          Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None))),
          Some(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None)),
          Some(List(EiLPerson("AA111111B", "Jane", None, "Doe", Some("456"), None, None, None))),
          None,
          None
        ))))
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      val TEST_IABD = "car"
      val TEST_YEAR_CODE = "cyp1"
      val TEST_NINO = "AA111111B"
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val result = mockExclusionListController.remove(TEST_YEAR_CODE, TEST_IABD, TEST_NINO)(mockrequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be(s"/payrollbik/$TEST_YEAR_CODE/$TEST_IABD/exclude-employee-remove")
    }
  }

  "When confirming the removal of an excluded individual, an authorised user" must {
    "see the removal confirmation screen" in {
      val TEST_IABD = "car"
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val result = mockExclusionListController.removeExclusionsCommit(TEST_IABD)(mockrequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be("/payrollbik/car/remove-commit")
    }
  }

  "When loading the removal check your answers screen with valid data in the cache, the controller" must {
    "show the removal check your answers screen" in {
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                None,
                None,
                None,
                Some(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None)),
                None,
                None,
                None
              ))))
      val result = mockExclusionListController.showRemovalConfirmation("cyp1", "car").apply(mockrequest)
      status(result) must be(OK)
      contentAsString(result) must include(
        "The following employee will have Car and car fuel taxed through your payroll")
      contentAsString(result) must include("John")
      contentAsString(result) must include("Smith")
    }
  }

  "When loading the removal what next screen with valid data in the cache, the controller" must {
    "show the removal what next screen" in {
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                None,
                None,
                None,
                Some(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None)),
                None,
                None,
                None
              ))))
      val result = mockExclusionListController.showRemovalWhatsNext("car").apply(mockrequest)
      status(result) must be(OK)
      contentAsString(result) must include(
        "You’ve told us that John Smith will be having Car and car fuel taxed through your payroll from 6 April")
    }
  }

  "When validating a year the controller" must {
    "should return the current tax year if the validation passes for cy" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      implicit val timeout: Timeout = 10 seconds
      val result = await(mockExclusionListController.validateRequest("cy", "car"))(timeout)
      result must be(taxDateUtils.getCurrentTaxYear())
    }

    "it should throw an InvalidBikTypeURIException if the Bik is not registered valid" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      intercept[InvalidBikTypeURIException] {
        await(mockExclusionListController.validateRequest("cy", "1"))
      }
    }

  }

  "When remove exclusions are committed the controller" must {
    " show the what next page" in {
      val TEST_IABD = "car"
      val f = controllersReferenceData.individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)

      val result = mockExclusionListController.removeExclusionsCommit(TEST_IABD)(formrequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be(s"/payrollbik/$TEST_IABD/remove-commit")
    }
  }

  "When remove Exclusions Commit when exclusions are disabled the controller" must {
    " should show an error page" in {
      val TEST_IABD = "car"
      val f = controllersReferenceData.individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = app.injector.instanceOf[MockExclusionsDisallowedController]
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.removeExclusionsCommit(TEST_IABD).apply(formrequest))(timeout)
      result.header.status must be(FORBIDDEN)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When remove is called the controller" must {
    " show the confirmation page" in {
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(PbikSession(
          None,
          None,
          Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None))),
          Some(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None)),
          Some(List(EiLPerson("AA111111A", "Jane", None, "Doe", Some("456"), None, None, None))),
          None,
          None
        ))))
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val TEST_NINO = "AA111111A"
      val f = controllersReferenceData.individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val result = mockExclusionListController.remove(TEST_YEAR_CODE, TEST_IABD, TEST_NINO)(formrequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be(s"/payrollbik/$TEST_YEAR_CODE/$TEST_IABD/exclude-employee-remove")
    }
  }

  "When remove is called when exclusion mode is disabled the controller" must {
    " should show an error page" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val TEST_NINO = "AA111111A"
      val f = controllersReferenceData.individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = app.injector.instanceOf[MockExclusionsDisallowedController]
      implicit val timeout: Timeout = 5 seconds
      val result = await(mockExclusionController.remove(TEST_YEAR_CODE, TEST_IABD, TEST_NINO)(formrequest))(timeout)
      result.header.status must be(FORBIDDEN)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When updateExclusions is called the controller" must {
    " redirect to the what next page" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val f = controllersReferenceData.individualsForm.fill(EiLPersonList(ListOfPeople))
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(PbikSession(
          None,
          None,
          Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None))),
          None,
          Some(List.empty[EiLPerson]),
          None,
          Some(List(Bik("31", 30)))
        ))))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val result = mockExclusionListController.updateExclusions(TEST_YEAR_CODE, TEST_IABD)(formrequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be(s"/payrollbik/$TEST_YEAR_CODE/$TEST_IABD/exclude-confirmation")
    }
  }

  "When updateExclusions is called but theres no session data present" must {
    "return a 500" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val f = controllersReferenceData.individualsForm.fill(EiLPersonList(ListOfPeople))
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                None,
                None,
                None,
                None,
                Some(List.empty[EiLPerson]),
                None,
                Some(List(Bik("31", 30)))
              ))))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val result = mockExclusionListController.updateExclusions(TEST_YEAR_CODE, TEST_IABD)(formrequest)

      status(result) must be(INTERNAL_SERVER_ERROR)
    }
  }

  "When updateExclusions is called but exclusions are disabled the controller" must {
    " redirect back to the overview page" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val f = controllersReferenceData.individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = app.injector.instanceOf[MockExclusionsDisallowedController]
      implicit val timeout: Timeout = 5 seconds
      val result =
        await(mockExclusionController.updateExclusions(TEST_YEAR_CODE, TEST_IABD)(formrequest))(timeout)
      result.header.status must be(FORBIDDEN)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When updateMultipleExclusions is called the controller" must {
    " redirect to the what next page" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD = "car"
      val f = controllersReferenceData.individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody("individualNino" -> "AA111111A")
      val title = Messages("ExclusionSearch.title")
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(PbikSession(
          None,
          None,
          Some(List(
            EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None),
            EiLPerson("BB222222B", "John", None, "Smith", Some("456"), None, None, None)
          )),
          None,
          Some(List.empty[EiLPerson]),
          None,
          Some(List(Bik("31", 30)))
        ))))
      implicit val timeout: Timeout = 5 seconds

      val result =
        await(
          mockExclusionListController
            .updateMultipleExclusions(TEST_YEAR_CODE, TEST_IABD, ControllersReferenceDataCodes.FORM_TYPE_NINO)(
              formrequest))(timeout)
      result.header.status must be(SEE_OTHER)
      result.header.headers("Location") mustBe "/payrollbik/cyp1/car/exclude-confirmation"
    }
  }

  "When the what next page is loaded, the controller" must {
    "display the the what next confirmation screen" in {
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                None,
                None,
                Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None))),
                None,
                None,
                None,
                None
              ))))
      val result = mockExclusionListController.showExclusionConfirmation("cyp1", "car").apply(mockrequest)
      status(result) must be(OK)
      contentAsString(result) must include(
        "You’ve told us that John Smith won’t be having Car and car fuel taxed through your payroll from 6 April")

    }
  }

  "When updateMultipleExclusions is called but exclusions are disabled the controller" must {
    " redirect back to the overview page" in {
      val TEST_YEAR_CODE = "cy"
      val TEST_IABD = "car"
      val f = controllersReferenceData.individualsForm.fill(EiLPersonList(ListOfPeople))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("ServiceMessage.10002")
      val mockExclusionController = app.injector.instanceOf[MockExclusionsDisallowedController]
      implicit val timeout: Timeout = 5 seconds
      val result =
        await(
          mockExclusionController
            .updateMultipleExclusions(TEST_YEAR_CODE, TEST_IABD, ControllersReferenceDataCodes.FORM_TYPE_NINO)(
              formrequest))(timeout)
      result.header.status must be(FORBIDDEN)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When updating individual exclusions, an authorised user" must {
    "see the page what's next page in order to review their result" in {
      val TEST_YEAR_CODE = "cyp1"
      val TEST_IABD_VALUE = "car"
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      when(mockExclusionListController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(PbikSession(
          None,
          None,
          Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None))),
          None,
          Some(List.empty[EiLPerson]),
          None,
          Some(List(Bik("31", 30)))
        ))))
      val result = mockExclusionListController.updateExclusions(TEST_YEAR_CODE, TEST_IABD_VALUE).apply(mockrequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be(s"/payrollbik/$TEST_YEAR_CODE/$TEST_IABD_VALUE/exclude-confirmation")
    }
  }

  // Tests below to check input validation
  "When updating exclusions," must {
    "an invalid input on first name" in {
      val TEST_EIL_PERSON: List[EiLPerson] = List(
        EiLPerson("AA111111", " ", Some("Stones"), "Smith", Some("123"), Some("01/01/1980"), Some("male"), Some(10), 0))
      val TEST_YEAR_CODE = "cy"
      val TEST_IABD_VALUE = "car"
      val f = controllersReferenceData.individualsForm.fill(EiLPersonList(TEST_EIL_PERSON))
      implicit val formrequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockrequest.withFormUrlEncodedBody(f.data.toSeq: _*)
      val title = Messages("ExclusionSearch.form.title")
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      implicit val timeout: Timeout = 5 seconds
      val result =
        await(mockExclusionListController.searchResults(TEST_YEAR_CODE, TEST_IABD_VALUE, "nino")(formrequest))(timeout)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include("Search for an employee to exclude")
    }
  }
}
