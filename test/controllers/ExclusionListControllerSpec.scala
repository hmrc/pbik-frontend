/*
 * Copyright 2024 HM Revenue & Customs
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

import base.FakePBIKApplication
import connectors.PbikConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models._
import models.auth.AuthenticatedRequest
import models.form.{DateOfBirth, MandatoryRadioButton, NinoForm, NoNinoForm}
import models.v1.IabdType.IabdType
import models.v1._
import models.v1.exclusion.{Gender, PbikExclusionPerson, PbikExclusions, SelectedExclusionToRemove}
import models.v1.trace.{TracePersonListResponse, TracePersonResponse}
import org.mockito.ArgumentMatchers.{any, anyInt, eq => argEq}
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{BikListService, ExclusionService, SessionService}
import support._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Exceptions.InvalidBikTypeException
import utils._

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class ExclusionListControllerSpec extends FakePBIKApplication {

  private val mockConnector      = mock(classOf[PbikConnector])
  private val mockSessionService = mock(classOf[SessionService])
  private val mockBikListService = mock(classOf[BikListService])

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[AuthAction].to(classOf[TestAuthActionOrganisation]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .overrides(bind[ExclusionService].to(classOf[StubExclusionService]))
    .overrides(bind[PbikConnector].toInstance(mockConnector))
    .overrides(bind[SessionService].toInstance(mockSessionService))
    .overrides(bind[BikListService].toInstance(mockBikListService))
    .build()

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val messages: Messages                                                     = injected[MessagesApi].preferred(Seq(lang))
  private val controllersReferenceData: ControllersReferenceData                     = injected[ControllersReferenceData]
  private val mockExclusionListController: MockExclusionListController               =
    injected[MockExclusionListController]
  private val mockExclusionsDisallowedController: MockExclusionsDisallowedController =
    injected[MockExclusionsDisallowedController]
  private val date: LocalDate                                                        = LocalDate.now()
  private val dateRange: TaxYearRange                                                =
    if (date.getMonthValue < 4 || (date.getMonthValue == 4 && date.getDayOfMonth < 6)) {
      models.TaxYearRange(date.getYear - 1, date.getYear, date.getYear + 1)
    } else {
      models.TaxYearRange(date.getYear, date.getYear + 1, date.getYear + 2)
    }
  private val year: Int                                                              = 2020
  private val (nino, cy, cyp1): (String, String, String)                             = ("AA111111A", "cy", "cyp1")
  private val iabdType: IabdType                                                     = IabdType.CarBenefit
  private val cyBenefitTypes: BenefitTypes                                           = BenefitTypes(IabdType.values)
  private val cyBiks                                                                 =
    cyBenefitTypes.pbikTypes.map(x => BenefitInKindWithCount(x, 3))

  private val pbikSession: PbikSession = PbikSession(
    sessionId = UUID.randomUUID().toString,
    registrations = None,
    bikRemoved = None,
    listOfMatches = Some(
      TracePersonListResponse(
        8,
        List(
          TracePersonResponse("AB123456C", "John", None, "Doe", None, 22),
          TracePersonResponse("AA111111", "John", None, "Smith", None, 33)
        )
      )
    ),
    eiLPerson = Some(
      SelectedExclusionToRemove(1, PbikExclusionPerson("AB123456C", "John", Some("A"), "Doe", Some("12345"), 22))
    ),
    currentExclusions = Some(
      PbikExclusions(
        17,
        Some(
          List(
            PbikExclusionPerson("AB123456C", "John", Some("A"), "Doe", Some("12345"), 22)
          )
        )
      )
    ),
    cyRegisteredBiks = None,
    nyRegisteredBiks = None
  )

  implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = createAuthenticatedRequest(
    mockRequest
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockConnector)
    reset(mockSessionService)
    reset(mockBikListService)

    when(mockConnector.getAllAvailableBiks(anyInt())(any()))
      .thenReturn(Future.successful(Right(cyBenefitTypes)))

    when(
      mockConnector.getRegisteredBiks(
        any(),
        anyInt()
      )(any())
    ).thenReturn(
      Future.successful(
        BenefitListResponse(Some(cyBiks.toList), 0)
      )
    )

    when(
      mockConnector
        .getRegisteredBiks(any(), argEq(year))(any())
    ).thenReturn(
      Future.successful(
        BenefitListResponse(Some(cyBiks.toList), 0)
      )
    )

    when(
      mockConnector
        .getRegisteredBiks(any(), anyInt())(any())
    ).thenReturn(
      Future.successful(
        BenefitListResponse(Some(cyBiks.toList), 0)
      )
    )

    when(
      mockConnector
        .excludeEiLPersonFromBik(any(), anyInt(), any())(any())
    ).thenReturn(Future.successful(Right(OK)))

    when(mockSessionService.storeEiLPerson(any())(any())).thenReturn(
      Future.successful(PbikSession(pbikSession.sessionId))
    )

    when(mockSessionService.storeCYRegisteredBiks(any())(any())).thenReturn(
      Future.successful(PbikSession(pbikSession.sessionId))
    )

    when(mockSessionService.storeNYRegisteredBiks(any())(any())).thenReturn(
      Future.successful(PbikSession(pbikSession.sessionId))
    )

    when(mockBikListService.getRegisteredBenefitsForYear(anyInt())(any(), any()))
      .thenReturn(
        Future.successful(
          BenefitListResponse(
            Some(List(BenefitInKindWithCount(iabdType, 3))),
            0
          )
        )
      )

    when(mockSessionService.storeCurrentExclusions(any())(any()))
      .thenReturn(Future.successful(PbikSession(pbikSession.sessionId)))

  }

  "ExclusionListController" when {
    "testing exclusions the exclusion functionality" must {
      "be enabled" in
        assert(mockExclusionListController.exclusionsAllowed)
    }

    "checking the Bik's IABD value is invalid for CY" must {
      "return the start year of the CY pair, when the IABD value is valid" in {
        val result = await(mockExclusionListController.validateRequest(cy, iabdType))

        result mustBe dateRange.cyminus1
      }

      "throw exception when valid year and not valid IABD" in {
        val result = intercept[InvalidBikTypeException] {
          await(mockExclusionListController.validateRequest(cy, IabdType.MedicalInsurance))
        }

        result.message mustBe "Invalid Bik Type"
      }
    }

    "loading the performPageLoad" should {

      "show the restriction page when exclusions are disallowed" in {
        val result = mockExclusionsDisallowedController.performPageLoad(cy, iabdType)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(messages("ServiceMessage.10002"))
      }

      "show the exclusion overview page when exclusions are allowed" in {
        val result = mockExclusionListController.performPageLoad(cy, iabdType)(mockRequest)

        status(result) mustBe OK
      }
    }

    "loading the submitExcludedEmployees page when exclusions are disabled" must {
      "show an error page" in {
        val result = mockExclusionsDisallowedController.submitExcludedEmployees(cy, iabdType)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(messages("ServiceMessage.10002"))
      }
    }

    def submitExcludedEmployeesTest(selectionValue: String, page: String, url: String, year: String): Unit = {
      val formData                                             =
        controllersReferenceData.binaryRadioButton.fill(MandatoryRadioButton(selectionValue))
      val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)

      s"loading the submitExcludedEmployees with valid form selecting $selectionValue" must {
        s"proceed to $page when value=$selectionValue and year=$year" in {
          val result = mockExclusionListController.submitExcludedEmployees(year, iabdType)(formRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(url)
        }
      }
    }
    val submitExcludedEmployeesInputArgs                                                                   = Seq(
      (
        ControllersReferenceDataCodes.YES,
        "Exclude an employee form page",
        s"/payrollbik/$cyp1/${iabdType.id}/employee-national-insurance-number",
        cyp1
      ),
      (
        ControllersReferenceDataCodes.NO,
        s"Payrolling summary form page for $cy",
        s"/payrollbik/$cy/registered-benefits-expenses",
        cy
      ),
      (
        ControllersReferenceDataCodes.NO,
        s"Payrolling summary form page for $cyp1",
        s"/payrollbik/cy1/registered-benefits-expenses",
        cyp1
      )
    )
    submitExcludedEmployeesInputArgs.foreach(args => (submitExcludedEmployeesTest _).tupled(args))

    "loading the withOrWithoutNinoOnPageLoad" must {
      "show the page in order to make a decision" in {
        val (beginIndex, endIndex) = (0, 10)
        val title                  = messages("ExclusionNinoDecision.title").substring(beginIndex, endIndex)
        val result                 = mockExclusionListController.withOrWithoutNinoOnPageLoad(cy, iabdType)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(title)
        contentAsString(result) must include(messages("Service.yes"))
        contentAsString(result) must include(messages("Service.no"))
      }

      "display the error page when exclusions feature is disabled" in {
        val title  = messages("ServiceMessage.10002")
        val result = mockExclusionsDisallowedController.withOrWithoutNinoOnPageLoad(cy, iabdType)(mockRequest)

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }
    }

    "loading the withOrWithoutNinoDecision page with the form omitted, an authorised user" must {
      "see the page in order to confirm their decision" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(Future.successful(Some(pbikSession)))
        val title  = messages("ExclusionNinoDecision.title")
        val result = mockExclusionListController.withOrWithoutNinoDecision(cyp1, iabdType)(mockRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(title)
      }
    }

    "loading the withOrWithoutNinoDecision page when exclusions are disabled" must {
      "show an error page" in {
        val title  = messages("ServiceMessage.10002")
        val result = mockExclusionsDisallowedController.withOrWithoutNinoDecision(cyp1, iabdType)(mockRequest)

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }
    }

    def withOrWithoutNinoDecisionTest(selectionValue: String, page: String, url: String): Unit = {
      val formData                                             =
        controllersReferenceData.binaryRadioButton.fill(MandatoryRadioButton(selectionValue))
      val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)

      s"loading the withOrWithoutNinoDecision with valid form selecting $selectionValue" must {
        s"proceed to $page" in {
          val result = mockExclusionListController.withOrWithoutNinoDecision(cyp1, iabdType)(formRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(url)
        }
      }
    }
    val withOrWithoutNinoDecisionArgs                                                          = Seq(
      ("nino", "nino form page", s"/payrollbik/cyp1/${iabdType.id}/nino/search-for-employee"),
      ("no-nino", "no-nino form page", s"/payrollbik/cyp1/${iabdType.id}/no-nino/search-for-employee")
    )
    withOrWithoutNinoDecisionArgs.foreach(args => (withOrWithoutNinoDecisionTest _).tupled(args))

    "loading showExclusionSearchForm, an authorised user" must {
      def searchPersonTest(formType: String, titleKey: String, hintKey: String): Unit =
        s"see the $formType form in order to search for a person" in {
          val title  = messages(titleKey)
          val hint   = messages(hintKey)
          val result = mockExclusionListController.showExclusionSearchForm(cyp1, iabdType, formType)(mockRequest)

          status(result) mustBe OK
          contentAsString(result) must include(title)
          contentAsString(result) must include(hint)
        }
      val searchPersonTestArgs                                                        = Seq(
        ("nino", "ExclusionSearch.form.title", "Service.field.ninohint"),
        ("no-nino", "ExclusionSearch.form.title", s"/payrollbik/cyp1/${iabdType.id}/no-nino/search-for-employee")
      )
      searchPersonTestArgs.foreach(args => (searchPersonTest _).tupled(args))

      "see an error page if neither nino or no-nino are chosen" in {
        val formType = "nothing"
        val title    = messages("ErrorPage.invalidForm")
        val result   = mockExclusionListController.showExclusionSearchForm(cyp1, iabdType, formType)(mockRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include(title)
      }
    }

    "loading the searchResults page" must {
      "display the expected search results page for an authorised user's NINO search" in {
        when(mockConnector.findPersonByNino(any(), anyInt(), any())(any()))
          .thenReturn(
            Future.successful(
              Right(
                TracePersonListResponse(
                  1,
                  List(TracePersonResponse("AB111111", "Adam", None, "Smith", Some("123"), 22))
                )
              )
            )
          )
        when(mockSessionService.storeListOfMatches(any())(any()))
          .thenReturn(Future.successful(PbikSession(pbikSession.sessionId)))
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = None,
                  eiLPerson = None,
                  currentExclusions = None
                )
              )
            )
          )

        val ninoSearchPerson                                              = NinoForm("Adam", "Smith", "AB111111")
        val formData                                                      =
          controllersReferenceData.exclusionSearchFormWithNino(request = mockRequest).fill(ninoSearchPerson)
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val result                                                        =
          mockExclusionListController.searchResults(cyp1, iabdType, ControllersReferenceDataCodes.FORM_TYPE_NINO)(
            formRequest
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$cyp1/${iabdType.id}/nino/exclude-employee-results")
      }

      "display the expected nino search form when NINO form with errors" in {
        when(mockConnector.findPersonByNino(any(), anyInt(), any())(any()))
          .thenReturn(
            Future.successful(
              Right(
                TracePersonListResponse(
                  1,
                  List(TracePersonResponse("AB111111", "Adam", None, "Smith", Some("123"), 22))
                )
              )
            )
          )
        when(mockSessionService.storeListOfMatches(any())(any()))
          .thenReturn(Future.successful(PbikSession(pbikSession.sessionId)))
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = None,
                  eiLPerson = None,
                  currentExclusions = None
                )
              )
            )
          )

        val formData                                                      =
          controllersReferenceData.exclusionSearchFormWithNino(request = mockRequest)
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val result                                                        =
          mockExclusionListController.searchResults(cyp1, iabdType, ControllersReferenceDataCodes.FORM_TYPE_NINO)(
            formRequest
          )

        status(result) mustBe OK
        contentAsString(result) must include(messages("Service.errorSummary.heading"))
      }

      "display the expected nino search form when non-NINO form with errors" in {
        when(mockConnector.findPersonByPersonalDetails(any(), anyInt(), any())(any()))
          .thenReturn(
            Future.successful(
              Right(
                TracePersonListResponse(
                  1,
                  List(TracePersonResponse("AB111111", "Adam", None, "Smith", Some("123"), 22))
                )
              )
            )
          )
        when(mockSessionService.storeListOfMatches(any())(any()))
          .thenReturn(Future.successful(PbikSession(pbikSession.sessionId)))
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = None,
                  eiLPerson = None,
                  currentExclusions = None
                )
              )
            )
          )

        val formData                                                      =
          controllersReferenceData.exclusionSearchFormWithoutNino(request = mockRequest)
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val result                                                        =
          mockExclusionListController.searchResults(cyp1, iabdType, ControllersReferenceDataCodes.FORM_TYPE_NONINO)(
            formRequest
          )

        status(result) mustBe OK
        contentAsString(result) must include(messages("Service.errorSummary.heading"))
      }

      "display the expected error page view when NINO search call returns NPSError" in {
        when(mockConnector.findPersonByNino(any(), anyInt(), any())(any()))
          .thenReturn(
            Future.successful(
              Left(NPSErrors(Seq(NPSError("test reson", "code.test.123"))))
            )
          )
        when(mockSessionService.storeListOfMatches(any())(any()))
          .thenReturn(Future.successful(PbikSession(pbikSession.sessionId)))
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = None,
                  eiLPerson = None,
                  currentExclusions = None
                )
              )
            )
          )

        val ninoSearchPerson                                              = NinoForm("Adam", "Smith", "AB111111")
        val formData                                                      =
          controllersReferenceData.exclusionSearchFormWithNino(request = mockRequest).fill(ninoSearchPerson)
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val result                                                        =
          mockExclusionListController.searchResults(cyp1, iabdType, ControllersReferenceDataCodes.FORM_TYPE_NINO)(
            formRequest
          )

        status(result) mustBe UNPROCESSABLE_ENTITY
        contentAsString(result) must include(messages("ServiceMessage.code.test.123"))
      }

      "display the expected error page view when non-NINO search call returns NPSError" in {
        when(mockConnector.findPersonByPersonalDetails(any(), anyInt(), any())(any()))
          .thenReturn(
            Future.successful(
              Left(NPSErrors(Seq(NPSError("test reson", "code.test.123"))))
            )
          )
        when(mockSessionService.storeListOfMatches(any())(any()))
          .thenReturn(Future.successful(PbikSession(pbikSession.sessionId)))
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = None,
                  eiLPerson = None,
                  currentExclusions = None
                )
              )
            )
          )

        val noNinoForm                                                    = NoNinoForm("Adam", "Smith", DateOfBirth("01", "01", "1980"), Gender.Female)
        val formData                                                      =
          controllersReferenceData.exclusionSearchFormWithoutNino(request = mockRequest).fill(noNinoForm)
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val result                                                        =
          mockExclusionListController.searchResults(cyp1, iabdType, ControllersReferenceDataCodes.FORM_TYPE_NONINO)(
            formRequest
          )

        status(result) mustBe UNPROCESSABLE_ENTITY
        contentAsString(result) must include(messages("ServiceMessage.code.test.123"))
      }

      "display the expected search results page for an authorised user's non-NINO search - Male" in {
        when(mockConnector.findPersonByPersonalDetails(any(), anyInt(), any())(any()))
          .thenReturn(
            Future.successful(
              Right(
                TracePersonListResponse(
                  1,
                  List(TracePersonResponse("AB111111", "Adam", None, "Smith", Some("123"), 22))
                )
              )
            )
          )
        when(mockSessionService.storeListOfMatches(any())(any()))
          .thenReturn(Future.successful(PbikSession(pbikSession.sessionId)))
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = None,
                  eiLPerson = None,
                  currentExclusions = None
                )
              )
            )
          )
        val noNinoSearchPerson                                            =
          NoNinoForm("Adam", "Smith", DateOfBirth("01", "11", "1980"), Gender.Male)
        val formData                                                      =
          controllersReferenceData.exclusionSearchFormWithoutNino(request = mockRequest).fill(noNinoSearchPerson)
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val result                                                        =
          mockExclusionListController.searchResults(cyp1, iabdType, ControllersReferenceDataCodes.FORM_TYPE_NONINO)(
            formRequest
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$cyp1/${iabdType.id}/no-nino/exclude-employee-results")
      }

      "display the expected search results page for an authorised user's non-NINO search - Female" in {
        when(mockConnector.findPersonByPersonalDetails(any(), anyInt(), any())(any()))
          .thenReturn(
            Future.successful(
              Right(
                TracePersonListResponse(
                  1,
                  List(TracePersonResponse("AB111111", "Adam", None, "Smith", Some("123"), 22))
                )
              )
            )
          )
        when(mockSessionService.storeListOfMatches(any())(any()))
          .thenReturn(Future.successful(PbikSession(pbikSession.sessionId)))
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = None,
                  eiLPerson = None,
                  currentExclusions = None
                )
              )
            )
          )
        val noNinoSearchPerson                                            =
          NoNinoForm("Adam", "Smith", DateOfBirth("01", "11", "1980"), Gender.Female)
        val formData                                                      =
          controllersReferenceData.exclusionSearchFormWithoutNino(request = mockRequest).fill(noNinoSearchPerson)
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val result                                                        =
          mockExclusionListController.searchResults(cyp1, iabdType, ControllersReferenceDataCodes.FORM_TYPE_NONINO)(
            formRequest
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$cyp1/${iabdType.id}/no-nino/exclude-employee-results")
      }

      "display the expected search results page for an authorised user's non-NINO search - Unknown" in {
        when(mockConnector.findPersonByPersonalDetails(any(), anyInt(), any())(any()))
          .thenReturn(
            Future.successful(
              Right(
                TracePersonListResponse(
                  1,
                  List(TracePersonResponse("AB111111", "Adam", None, "Smith", Some("123"), 22))
                )
              )
            )
          )
        when(mockSessionService.storeListOfMatches(any())(any()))
          .thenReturn(Future.successful(PbikSession(pbikSession.sessionId)))
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = None,
                  eiLPerson = None,
                  currentExclusions = None
                )
              )
            )
          )
        val noNinoSearchPerson                                            =
          NoNinoForm("Adam", "Smith", DateOfBirth("01", "11", "1980"), Gender.Unknown)
        val formData                                                      =
          controllersReferenceData.exclusionSearchFormWithoutNino(request = mockRequest).fill(noNinoSearchPerson)
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val result                                                        =
          mockExclusionListController.searchResults(cyp1, iabdType, ControllersReferenceDataCodes.FORM_TYPE_NONINO)(
            formRequest
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$cyp1/${iabdType.id}/no-nino/exclude-employee-results")
      }

      "show an error page when exclusions are disabled" in {
        val title  = messages("ServiceMessage.10002")
        val result =
          mockExclusionsDisallowedController.searchResults(
            cyp1,
            iabdType,
            ControllersReferenceDataCodes.FORM_TYPE_NONINO
          )(mockRequest)

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }
    }

    "handling a valid search result" must {
      "show an error page if the list of matches contains only already excluded individuals" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  eiLPerson = None,
                  currentExclusions = Some(
                    PbikExclusions(
                      0,
                      Some(List(PbikExclusionPerson("AB111111", "Adam", None, "Smith", Some("123"), 22)))
                    )
                  ),
                  listOfMatches = Some(
                    TracePersonListResponse(
                      1,
                      List(TracePersonResponse("AB111111", "Adam", None, "Smith", Some("123"), 22))
                    )
                  )
                )
              )
            )
          )
        val result = mockExclusionListController.showResults(cyp1, iabdType, "nino")(mockRequest)

        status(result) mustBe NOT_FOUND
        contentAsString(result) must include(messages("ServiceMessage.63085.h1"))
      }

      "show an error page if the list of matches is empty" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = None,
                  eiLPerson = None,
                  currentExclusions = Some(
                    PbikExclusions(
                      0,
                      Some(List(PbikExclusionPerson("AB111111", "Adam", None, "Smith", Some("123"), 22)))
                    )
                  )
                )
              )
            )
          )
        val result =
          Future.successful(
            mockExclusionListController
              .searchResultsHandleValidResult(List.empty, cyp1, "nino", iabdType, List.empty)
          )

        status(result) mustBe NOT_FOUND
        contentAsString(result) must include(messages("ServiceMessage.65127.h1"))
      }
    }

    "loading the searchResults page" must {
      "show the search results if they are present in the cache" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(Future.successful(Some(pbikSession)))
        val result = mockExclusionListController.showResults(cyp1, iabdType, "nino")(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include("AA111111")
        contentAsString(result) must include("John")
        contentAsString(result) must include("Smith")
      }

      "show an error page if no results are present in the cache" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(Some(pbikSession.copy(pbikSession.sessionId, None, None, None, None, None, None, None)))
          )
        val result = mockExclusionListController.showResults(cyp1, iabdType, "nino")(mockRequest)

        status(result) mustBe NOT_FOUND
        contentAsString(result) must include("We’re sorry a technical error has occurred")
      }
    }

    "remove is called" must {
      "show the confirmation page" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(Future.successful(Some(pbikSession)))
        val nino   = pbikSession.currentExclusions.get.exclusions.head.nationalInsuranceNumber
        val result = mockExclusionListController.remove(cyp1, iabdType, nino)(mockRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$cyp1/${iabdType.id}/check-employee-details")
      }

      "show an error page when exclusion mode is disabled" in {
        val title  = messages("ServiceMessage.10002")
        val result = mockExclusionsDisallowedController.remove(cyp1, iabdType, nino)(mockRequest)

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }
    }

    "loading removal screens with valid data in the cache" must {

      "show the removal check your answers screen" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(Future.successful(Some(pbikSession.copy(listOfMatches = None, currentExclusions = None))))

        val result = mockExclusionListController.showRemovalConfirmation(cyp1, iabdType)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(
          s"By confirming, ${pbikSession.eiLPerson.get.personToExclude.firstForename} ${pbikSession.eiLPerson.get.personToExclude.surname} will have Car and car fuel taxed through payroll from 6 April"
        )
        contentAsString(result) must include(
          s"${pbikSession.eiLPerson.get.personToExclude.firstForename} ${pbikSession.eiLPerson.get.personToExclude.surname}"
        )
      }

      "show the removal what next screen" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(Future.successful(Some(pbikSession.copy(listOfMatches = None, currentExclusions = None))))

        val result = mockExclusionListController.showRemovalWhatsNext(iabdType)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(
          s"${pbikSession.eiLPerson.get.personToExclude.firstForename} ${pbikSession.eiLPerson.get.personToExclude.surname} will have Car and car fuel taxed through payroll from 6 April"
        )
      }
    }

    "remove exclusions are committed" must {
      "show the what next page" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(Future.successful(Some(pbikSession.copy(listOfMatches = None, currentExclusions = None))))

        when(
          mockConnector
            .removeEiLPersonExclusionFromBik(argEq(iabdType), any(), anyInt(), any())(any())
        ).thenReturn(Future.successful(Right(OK)))

        val result = mockExclusionListController.removeExclusionsCommit(iabdType)(mockRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/${iabdType.id}/employee-registration-complete")
      }

      "show an error page when exclusions are disabled" in {
        val title  = messages("ServiceMessage.10002")
        val result = mockExclusionsDisallowedController.removeExclusionsCommit(iabdType)(mockRequest)

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }

      "return BAD_REQUEST when receiving a IM_A_TEAPOT  from the connector at an exclusion" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(Future.successful(Some(pbikSession.copy(listOfMatches = None, currentExclusions = None))))
        when(
          mockConnector
            .removeEiLPersonExclusionFromBik(any(), any(), anyInt(), any())(any())
        ).thenReturn(Future.successful(Right(IM_A_TEAPOT)))

        val resultForCyp1 = mockExclusionListController.removeExclusionsCommit(iabdType)(mockRequest)
        val resultForCy   = mockExclusionListController.removeExclusionsCommit(iabdType)(mockRequest)

        status(resultForCyp1) mustBe BAD_REQUEST
        status(resultForCy) mustBe BAD_REQUEST
      }

      "return INTERNAL_SERVER_ERROR when receiving a NPSError  from the connector at an exclusion" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(Future.successful(Some(pbikSession.copy(listOfMatches = None, currentExclusions = None))))
        when(
          mockConnector
            .removeEiLPersonExclusionFromBik(any(), any(), anyInt(), any())(any())
        ).thenReturn(Future.successful(Left(NPSErrors(Seq(NPSError("test reson", "code.test.123"))))))

        val result = mockExclusionListController.removeExclusionsCommit(iabdType)(mockRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include(messages("ServiceMessage.code.test.123"))
      }
    }

    "updateExclusions is called" must {
      "redirect to the what next page" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(nyRegisteredBiks =
                  Some(
                    BenefitListResponse(
                      Some(List(BenefitInKindWithCount(iabdType, 3))),
                      0
                    )
                  )
                )
              )
            )
          )
        val result = mockExclusionListController.updateExclusions(cyp1, iabdType)(mockRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$cyp1/${iabdType.id}/exclusion-complete")
      }

      "return a 500 when there is no session data present" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = None,
                  eiLPerson = None,
                  currentExclusions = None,
                  nyRegisteredBiks = Some(
                    BenefitListResponse(
                      Some(List(BenefitInKindWithCount(iabdType, 3))),
                      0
                    )
                  )
                )
              )
            )
          )
        val result = mockExclusionListController.updateExclusions(cyp1, iabdType)(mockRequest)

        status(result) mustBe NOT_FOUND
      }

      "redirect back to the overview page when exclusions are disabled" in {
        val title  = messages("ServiceMessage.10002")
        val result = mockExclusionsDisallowedController.updateExclusions(cyp1, iabdType)(mockRequest)

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }
    }

    "updateMultipleExclusions is called" must {
      "redirect to the what next page" in {
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(
            "individualNino" -> pbikSession.listOfMatches.get.pbikExclusionList.head.nationalInsuranceNumber
          )

        when(mockSessionService.fetchPbikSession()(any())).thenReturn(
          Future.successful(
            Some(
              pbikSession.copy(
                currentExclusions = None,
                nyRegisteredBiks = Some(
                  BenefitListResponse(
                    Some(List(BenefitInKindWithCount(iabdType, 3))),
                    0
                  )
                )
              )
            )
          )
        )

        val result =
          mockExclusionListController.updateMultipleExclusions(
            cyp1,
            iabdType,
            ControllersReferenceDataCodes.FORM_TYPE_NINO
          )(formRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$cyp1/${iabdType.id}/exclusion-complete")
      }

      "bad request when form with errors" in {
        implicit val formRequest: FakeRequest[AnyContentAsEmpty.type] = mockRequest

        when(mockSessionService.fetchPbikSession()(any())).thenReturn(
          Future.successful(
            Some(
              pbikSession.copy(
                currentExclusions = None,
                nyRegisteredBiks = Some(
                  BenefitListResponse(
                    Some(List(BenefitInKindWithCount(iabdType, 3))),
                    0
                  )
                )
              )
            )
          )
        )

        val result =
          mockExclusionListController.updateMultipleExclusions(
            cyp1,
            iabdType,
            ControllersReferenceDataCodes.FORM_TYPE_NINO
          )(formRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(messages("error.exclusion.multi.selection"))
      }

      "redirect back to the overview page when exclusions are disabled" in {
        val title  = messages("ServiceMessage.10002")
        val result =
          mockExclusionsDisallowedController.updateMultipleExclusions(
            cy,
            iabdType,
            ControllersReferenceDataCodes.FORM_TYPE_NINO
          )(mockRequest)
        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }
    }

    "the what next page is loaded" must {
      "display the what next confirmation screen" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(Future.successful(Some(pbikSession.copy(eiLPerson = None, currentExclusions = None))))
        val result = mockExclusionListController.showExclusionConfirmation(cyp1, iabdType)(mockRequest)

        status(result) mustBe OK
      }
    }

    "commitExclusion is called" must {

      "return INTERNAL_SERVER_ERROR when receiving a BAD_REQUEST  from the connector at an exclusion" in {
        val eilPerson = TracePersonResponse("AB111111", "Adam", None, "Smith", Some("123"), 24)

        when(
          mockConnector
            .excludeEiLPersonFromBik(any(), anyInt(), any())(any())
        ).thenReturn(Future.successful(Right(BAD_REQUEST)))

        val resultForCyp1 = mockExclusionListController.commitExclusion(cyp1, iabdType, 15, Some(eilPerson))
        val resultForCy   = mockExclusionListController.commitExclusion(cy, iabdType, 16, Some(eilPerson))

        status(resultForCyp1) mustBe INTERNAL_SERVER_ERROR
        status(resultForCy) mustBe INTERNAL_SERVER_ERROR
      }

      "return INTERNAL_SERVER_ERROR when receiving a NPSError  from the connector at an exclusion" in {
        val eilPerson = TracePersonResponse("AB111111", "Adam", None, "Smith", Some("123"), 24)

        when(
          mockConnector
            .excludeEiLPersonFromBik(any(), anyInt(), any())(any())
        ).thenReturn(Future.successful(Left(NPSErrors(Seq(NPSError("test reson", "code.test.123"))))))

        val result = mockExclusionListController.commitExclusion(cyp1, iabdType, 15, Some(eilPerson))

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include(messages("ServiceMessage.code.test.123"))
      }

    }
  }
}
