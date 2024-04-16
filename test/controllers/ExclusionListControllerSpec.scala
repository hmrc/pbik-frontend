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

import connectors.PbikConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models._
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.data.Form
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{EiLListService, SessionService}
import support._
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.HeaderCarrier
import utils.Exceptions.InvalidBikTypeException
import utils._

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class ExclusionListControllerSpec extends PlaySpec with FakePBIKApplication {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[AuthAction].to(classOf[TestAuthActionOrganisation]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .overrides(bind[EiLListService].to(classOf[StubEiLListService]))
    .overrides(bind[PbikConnector].toInstance(mock(classOf[PbikConnector])))
    .overrides(bind[SessionService].toInstance(mock(classOf[SessionService])))
    .build()

  private val messagesApi: MessagesApi                                               = app.injector.instanceOf[MessagesApi]
  private val controllersReferenceData: ControllersReferenceData                     = app.injector.instanceOf[ControllersReferenceData]
  private val mockExclusionListController: MockExclusionListController               =
    app.injector.instanceOf[MockExclusionListController]
  private val mockExclusionsDisallowedController: MockExclusionsDisallowedController =
    app.injector.instanceOf[MockExclusionsDisallowedController]
  private val date: LocalDate                                                        = LocalDate.now()
  private val dateRange: TaxYearRange                                                =
    if (date.getMonthValue < 4 || (date.getMonthValue == 4 && date.getDayOfMonth < 6)) {
      models.TaxYearRange(date.getYear - 1, date.getYear, date.getYear + 1)
    } else {
      models.TaxYearRange(date.getYear, date.getYear + 1, date.getYear + 2)
    }
  private val (statusValue, year, start, end): (Int, Int, Int, Int)                  = (10, 2020, 3, 32)
  private val (nino, cy, cyp1): (String, String, String)                             = ("AA111111A", "cy", "cyp1")
  private val (iabdType, iabdString): (String, String)                               = ("31", "car")
  private val CYCache: List[Bik]                                                     =
    List.range(start, end).map(n => Bik("" + n, statusValue))

  val responseHeaders: Map[String, String] = None.orNull

  val exclusionResponse: EiLResponse = EiLResponse(
    OK,
    Json
      .parse("""[
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
      .as[List[EiLPerson]]
  )

  private val listOfPeople: List[EiLPerson] = List(
    EiLPerson(
      "AA111111",
      "John",
      Some("Stones"),
      "Smith",
      Some("123"),
      Some("01/01/1980"),
      Some("male"),
      Some(statusValue)
    ),
    EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None)
  )
  private val formData: Form[EiLPersonList] =
    controllersReferenceData.individualsForm.fill(EiLPersonList(listOfPeople))
  private val pbikSession: PbikSession      = PbikSession(
    sessionId = UUID.randomUUID().toString,
    registrations = None,
    bikRemoved = None,
    listOfMatches = Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None))),
    eiLPerson = Some(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None)),
    currentExclusions = Some(List(EiLPerson("AA111111B", "Jane", None, "Doe", Some("456"), None, None, None))),
    cyRegisteredBiks = None,
    nyRegisteredBiks = None
  )

  implicit val lang: Lang                                                         = Lang("en-GB")
  implicit val request: FakeRequest[AnyContentAsEmpty.type]                       = mockRequest
  implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(
      EmpRef("taxOfficeNumber", "taxOfficeReference"),
      UserName(Name(None, None)),
      request,
      None
    )

  when(mockExclusionListController.tierConnector.getAllAvailableBiks(any[Int])(any[HeaderCarrier]))
    .thenReturn(Future.successful(CYCache))

  when(
    mockExclusionListController.tierConnector.getRegisteredBiks(
      any[EmpRef],
      any[Int]
    )(any[HeaderCarrier], any[AuthenticatedRequest[_]])
  ).thenReturn(
    Future.successful(
      BikResponse(
        responseHeaders,
        CYCache.filter { x: Bik =>
          Integer.parseInt(x.iabdType) <= 10
        }
      )
    )
  )

  when(
    mockExclusionListController.tierConnector
      .getRegisteredBiks(any[EmpRef], argEq(year))(
        any[HeaderCarrier],
        any[AuthenticatedRequest[_]]
      )
  ).thenReturn(
    Future.successful(
      BikResponse(
        responseHeaders,
        CYCache.filter { x: Bik =>
          Integer.parseInt(x.iabdType) <= 5
        }
      )
    )
  )

  when(
    mockExclusionListController.tierConnector
      .getRegisteredBiks(any[EmpRef], any[Int])(any[HeaderCarrier], any[AuthenticatedRequest[_]])
  ).thenReturn(
    Future.successful(
      BikResponse(
        responseHeaders,
        CYCache.filter { x: Bik =>
          Integer.parseInt(x.iabdType) >= 15
        }
      )
    )
  )

  when(
    mockExclusionListController.tierConnector
      .excludeEiLPersonFromBik(argEq(iabdString), any[EmpRef], any[Int], any[EiLPerson])(
        any[HeaderCarrier],
        any[Request[_]]
      )
  ).thenReturn(Future.successful(exclusionResponse))

  when(mockExclusionListController.sessionService.storeEiLPerson(any())(any[HeaderCarrier])).thenReturn(
    Future.successful(PbikSession(pbikSession.sessionId))
  )

  "ExclusionListController" when {
    "testing exclusions the exclusion functionality" must {
      "be enabled" in {
        assert(mockExclusionListController.exclusionsAllowed)
      }
    }

    "checking the Bik's IABD value is valid for CY" must {
      "return the start year of the CY pair, when the IABD value is valid" in {
        val result = await(mockExclusionListController.validateRequest(cy, iabdString))

        result mustBe dateRange.cyminus1
      }
    }

    "checking the Bik's IABD value is invalid for CY" must {
      "throw a InvalidBikTypeException" in {
        intercept[InvalidBikTypeException] {
          await(mockExclusionListController.validateRequest(cy, "unknown"))
        }
      }
    }

    "loading the performPageLoad" should {
      "direct an unauthorised user to an error page" in {
        val result = mockExclusionListController.performPageLoad(cy, iabdString)(mockRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "show the restriction page when exclusions are disallowed" in {
        val result = mockExclusionsDisallowedController.performPageLoad(cy, iabdString)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(messagesApi("ServiceMessage.10002"))
      }
    }

    "loading the submitExcludedEmployees page when exclusions are disabled" must {
      "show an error page" in {
        val result = mockExclusionsDisallowedController.submitExcludedEmployees(cy, iabdString)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(messagesApi("ServiceMessage.10002"))
      }
    }

    def submitExcludedEmployeesTest(selectionValue: String, page: String, url: String, year: String): Unit = {
      val formData                                                      =
        controllersReferenceData.binaryRadioButton.fill(MandatoryRadioButton(selectionValue))
      implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
      val result                                                        = mockExclusionListController.submitExcludedEmployees(year, iabdString)(formRequest)

      s"loading the submitExcludedEmployees with valid form selecting $selectionValue" must {
        s"proceed to $page" in {
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(url)
        }
      }
    }
    val submitExcludedEmployeesInputArgs = Seq(
      (
        "yes",
        "Exclude an employee form page",
        s"/payrollbik/cyp1/$iabdString/employee-national-insurance-number",
        cyp1
      ),
      ("no", "Payrolling summary form page for CY", "/payrollbik/cy/registered-benefits-expenses", cy),
      ("no", "Payrolling summary form page for CY1", "/payrollbik/cy1/registered-benefits-expenses", cyp1)
    )
    submitExcludedEmployeesInputArgs.foreach(args => (submitExcludedEmployeesTest _).tupled(args))

    "loading the withOrWithoutNinoOnPageLoad" must {
      "show the page in order to make a decision" in {
        val (beginIndex, endIndex) = (0, 10)
        val title                  = messagesApi("ExclusionNinoDecision.title").substring(beginIndex, endIndex)
        val result                 = mockExclusionListController.withOrWithoutNinoOnPageLoad(cy, iabdString)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(title)
        contentAsString(result) must include(messagesApi("Service.yes"))
        contentAsString(result) must include(messagesApi("Service.no"))
      }

      "display the error page when exclusions feature is disabled" in {
        val title  = messagesApi("ServiceMessage.10002")
        val result = mockExclusionsDisallowedController.withOrWithoutNinoOnPageLoad(cy, iabdString)(mockRequest)

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }
    }

    "loading the withOrWithoutNinoDecision page with the form omitted, an authorised user" must {
      "see the page in order to confirm their decision" in {
        when(mockExclusionListController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(pbikSession)))
        val title  = messagesApi("ExclusionNinoDecision.title")
        val result = mockExclusionListController.withOrWithoutNinoDecision(cyp1, iabdString)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(title)
      }
    }

    "loading the withOrWithoutNinoDecision page when exclusions are disabled" must {
      "show an error page" in {
        val title  = messagesApi("ServiceMessage.10002")
        val result = mockExclusionsDisallowedController.withOrWithoutNinoDecision(cyp1, iabdString)(mockRequest)

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }
    }

    def withOrWithoutNinoDecisionTest(selectionValue: String, page: String, url: String): Unit = {
      val formData                                                      =
        controllersReferenceData.binaryRadioButton.fill(MandatoryRadioButton(selectionValue))
      implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
      val result                                                        =
        mockExclusionListController.withOrWithoutNinoDecision(cyp1, iabdString)(formRequest)

      s"loading the withOrWithoutNinoDecision with valid form selecting $selectionValue" must {
        s"proceed to $page" in {
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(url)
        }
      }
    }
    val withOrWithoutNinoDecisionArgs = Seq(
      ("nino", "nino form page", s"/payrollbik/cyp1/$iabdString/nino/search-for-employee"),
      ("no-nino", "no-nino form page", s"/payrollbik/cyp1/$iabdString/no-nino/search-for-employee")
    )
    withOrWithoutNinoDecisionArgs.foreach(args => (withOrWithoutNinoDecisionTest _).tupled(args))

    "loading showExclusionSearchForm, an authorised user" must {
      def searchPersonTest(formType: String, titleKey: String, hintKey: String): Unit =
        s"see the $formType form in order to search for a person" in {
          val title  = messagesApi(titleKey)
          val hint   = messagesApi(hintKey)
          val result = mockExclusionListController.showExclusionSearchForm(cyp1, iabdString, formType)(mockRequest)

          status(result) mustBe OK
          contentAsString(result) must include(title)
          contentAsString(result) must include(hint)
        }
      val searchPersonTestArgs                                                        = Seq(
        ("nino", "ExclusionSearch.form.title", "Service.field.ninohint"),
        ("no-nino", "ExclusionSearch.form.title", "/payrollbik/cyp1/car/no-nino/search-for-employee")
      )
      searchPersonTestArgs.foreach(args => (searchPersonTest _).tupled(args))

      "see an error page if neither nino or no-nino are chosen" in {
        val formType = "nothing"
        val title    = messagesApi("ErrorPage.invalidForm")
        val result   = mockExclusionListController.showExclusionSearchForm(cyp1, iabdString, formType)(mockRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include(title)
      }
    }

    "loading the searchResults page" must {
      "display the expected search results page for an authorised user's NINO search" in {
        when(mockExclusionListController.sessionService.storeListOfMatches(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(PbikSession(pbikSession.sessionId)))
        when(mockExclusionListController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = Some(List(EiLPerson("", "", None, "", None, None, None, None))),
                  eiLPerson = None,
                  currentExclusions = None
                )
              )
            )
          )

        val ninoSearchPerson                                              = EiLPerson("AB111111", "Adam", None, "Smith", None, None, None, None)
        val formData                                                      =
          controllersReferenceData.exclusionSearchFormWithNino(request = mockRequest).fill(ninoSearchPerson)
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val result                                                        =
          mockExclusionListController.searchResults(cyp1, iabdString, ControllersReferenceDataCodes.FORM_TYPE_NINO)(
            formRequest
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$cyp1/$iabdString/nino/exclude-employee-results")

      }

      "display the expected search results page for an authorised user's non-NINO search" in {
        val noNinoSearchPerson                                            =
          EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None)
        val formData                                                      =
          controllersReferenceData.exclusionSearchFormWithoutNino(request = mockRequest).fill(noNinoSearchPerson)
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val result                                                        =
          mockExclusionListController.searchResults(cyp1, iabdString, ControllersReferenceDataCodes.FORM_TYPE_NONINO)(
            formRequest
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$cyp1/$iabdString/no-nino/exclude-employee-results")
      }

      "show an error page when exclusions are disabled" in {
        val title  = messagesApi("ServiceMessage.10002")
        val result =
          mockExclusionsDisallowedController.searchResults(
            cyp1,
            iabdString,
            ControllersReferenceDataCodes.FORM_TYPE_NONINO
          )(mockRequest)

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }
    }

    "handling a valid search result" must {
      "show an error page if the list of matches contains only already excluded individuals" in {
        when(mockExclusionListController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  eiLPerson = None,
                  currentExclusions =
                    Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None)))
                )
              )
            )
          )
        val result = mockExclusionListController.showResults(cyp1, iabdString, "nino")(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(messagesApi("ServiceMessage.63085.h1"))
      }

      "show an error page if the list of matches is empty" in {
        when(mockExclusionListController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = None,
                  eiLPerson = None,
                  currentExclusions =
                    Some(List(EiLPerson("AA111111A", "John", None, "Smith", Some("123"), None, None, None)))
                )
              )
            )
          )
        val result =
          Future.successful(
            mockExclusionListController
              .searchResultsHandleValidResult(List.empty[EiLPerson], cyp1, "nino", iabdString, List.empty[EiLPerson])
          )

        status(result) mustBe NOT_FOUND
        contentAsString(result) must include(messagesApi("ServiceMessage.65127.h1"))
      }
    }

    "loading the searchResults page" must {
      "show the search results if they are present in the cache" in {
        when(mockExclusionListController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(pbikSession)))
        val result = mockExclusionListController.showResults(cyp1, iabdString, "nino")(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include("AA111111")
        contentAsString(result) must include("John")
        contentAsString(result) must include("Smith")
      }

      "show an error page if no results are present in the cache" in {
        when(mockExclusionListController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(Some(pbikSession.copy(pbikSession.sessionId, None, None, None, None, None, None, None)))
          )
        val result = mockExclusionListController.showResults(cyp1, iabdString, "nino")(mockRequest)

        status(result) mustBe NOT_FOUND
        contentAsString(result) must include("We’re sorry a technical error has occurred")
      }
    }

    "remove is called" must {
      "show the confirmation page" in {
        when(mockExclusionListController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(pbikSession)))
        val nino   = "AA111111B"
        val result = mockExclusionListController.remove(cyp1, iabdString, nino)(mockRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$cyp1/$iabdString/check-employee-details")
      }

      "show an error page when exclusion mode is disabled" in {
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val title                                                         = messagesApi("ServiceMessage.10002")
        val result                                                        =
          mockExclusionsDisallowedController.remove(cyp1, iabdString, nino)(formRequest)

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }
    }

    "loading removal screens with valid data in the cache" must {
      when(mockExclusionListController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(pbikSession.copy(listOfMatches = None, currentExclusions = None))))

      "show the removal check your answers screen" in {
        val result = mockExclusionListController.showRemovalConfirmation(cyp1, iabdString)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(
          "By confirming, John Smith will have Car and car fuel taxed through payroll from 6 April"
        )
        contentAsString(result) must include("John Smith")
      }

      "show the removal what next screen" in {
        val result = mockExclusionListController.showRemovalWhatsNext(iabdString)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(
          "John Smith will have Car and car fuel taxed through payroll from 6 April"
        )
      }
    }

    "remove exclusions are committed" must {
      implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)

      "show the what next page" in {
        when(
          mockExclusionListController.tierConnector
            .removeEiLPersonExclusionFromBik(argEq(iabdString), any[EmpRef], any[Int], any[EiLPerson])(
              any[HeaderCarrier],
              any[Request[_]]
            )
        ).thenReturn(Future.successful(OK))

        val result = mockExclusionListController.removeExclusionsCommit(iabdString)(formRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$iabdString/employee-registration-complete")
      }

      "show an error page when exclusions are disabled" in {
        val title  = messagesApi("ServiceMessage.10002")
        val result = mockExclusionsDisallowedController.removeExclusionsCommit(iabdString)(formRequest)

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }
    }

    "updateExclusions is called" must {
      "redirect to the what next page" in {
        when(mockExclusionListController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(Some(pbikSession.copy(nyRegisteredBiks = Some(List(Bik(iabdType, statusValue))))))
          )
        val result = mockExclusionListController.updateExclusions(cyp1, iabdString)(mockRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$cyp1/$iabdString/exclusion-complete")
      }

      "return a 500 when there is no session data present" in {
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        when(mockExclusionListController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                pbikSession.copy(
                  listOfMatches = None,
                  eiLPerson = None,
                  currentExclusions = Some(List.empty[EiLPerson]),
                  nyRegisteredBiks = Some(List(Bik(iabdType, statusValue)))
                )
              )
            )
          )
        val result                                                        = mockExclusionListController.updateExclusions(cyp1, iabdString)(formRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "redirect back to the overview page when exclusions are disabled" in {
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val title                                                         = messagesApi("ServiceMessage.10002")
        val result                                                        = mockExclusionsDisallowedController.updateExclusions(cyp1, iabdString)(formRequest)

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }
    }

    "updateMultipleExclusions is called" must {
      "redirect to the what next page" in {
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody("individualNino" -> "AA111111A")

        when(mockExclusionListController.sessionService.fetchPbikSession()(any[HeaderCarrier])).thenReturn(
          Future.successful(
            Some(
              pbikSession.copy(
                currentExclusions = Some(List.empty[EiLPerson]),
                nyRegisteredBiks = Some(List(Bik(iabdType, statusValue)))
              )
            )
          )
        )

        val result =
          mockExclusionListController.updateMultipleExclusions(
            cyp1,
            iabdString,
            ControllersReferenceDataCodes.FORM_TYPE_NINO
          )(formRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/$cyp1/$iabdString/exclusion-complete")
      }

      "redirect back to the overview page when exclusions are disabled" in {
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val title                                                         = messagesApi("ServiceMessage.10002")
        val result                                                        =
          mockExclusionsDisallowedController.updateMultipleExclusions(
            cy,
            iabdType,
            ControllersReferenceDataCodes.FORM_TYPE_NINO
          )(formRequest)
        status(result) mustBe FORBIDDEN
        contentAsString(result) must include(title)
      }

      "return 400 for an invalid input on first name" in {
        val testEilPerson                                                 = List(
          EiLPerson("AA111111", "1", None, "Smith", None, None, None, None)
        )
        val formData                                                      = controllersReferenceData.individualsForm.fill(EiLPersonList(testEilPerson))
        implicit val formRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          mockRequest.withFormUrlEncodedBody(formData.data.toSeq: _*)
        val result                                                        =
          mockExclusionListController.updateMultipleExclusions(cy, iabdString, "nino")(formRequest)

        status(result) mustBe BAD_REQUEST
      }
    }

    "the what next page is loaded" must {
      "display the what next confirmation screen" in {
        when(mockExclusionListController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(pbikSession.copy(eiLPerson = None, currentExclusions = None))))
        val result = mockExclusionListController.showExclusionConfirmation(cyp1, iabdString)(mockRequest)

        status(result) mustBe OK
      }
    }

    "searchResultsHandleFormErrors is called" must {
      val eiLPersonForNino   = EiLPerson("AA111111", "1", None, "Smith", None, None, None, None)
      val eiLPersonForNoNino = EiLPerson("AB111111", "1", None, "Smith", None, Some("01/01/1980"), Some("male"), None)

      def test(formType: String, formData: Form[EiLPerson]): Unit =
        s"return OK for an invalid input on first name for formType $formType" in {
          val form   = formData
          val result = mockExclusionListController.searchResultsHandleFormErrors(cy, formType, iabdString, form)

          status(result) mustBe OK
        }
      val inputArgs                                               = Seq(
        ("nino", controllersReferenceData.exclusionSearchFormWithNino(mockRequest).fill(eiLPersonForNino)),
        ("no-nino", controllersReferenceData.exclusionSearchFormWithoutNino(mockRequest).fill(eiLPersonForNoNino))
      )
      inputArgs.foreach(args => (test _).tupled(args))
    }

    "extractExcludedIndividual is called" must {
      "return the correct result" in {
        val eilPerson = EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None)

        def result(eilPersonList: EiLPersonList, nino: String = "AB111111"): Option[EiLPerson] =
          mockExclusionListController.extractExcludedIndividual(nino, eilPersonList)

        result(EiLPersonList(List.empty)) mustBe None
        result(EiLPersonList(List(eilPerson))) mustBe Some(eilPerson)
        result(EiLPersonList(listOfPeople)) mustBe Some(
          EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None)
        )
        result(EiLPersonList(List(eilPerson, eilPerson.copy(nino = "AC111111"))), " ") mustBe Some(eilPerson)
      }
    }

    "commitExclusion is called" must {
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        AuthenticatedRequest(
          EmpRef("taxOfficeNumber", "taxOfficeReference"),
          UserName(Name(None, None)),
          request,
          None
        )
      implicit val hc: HeaderCarrier                                      = HeaderCarrier()

      val eilPerson = EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None)
      "return INTERNAL_SERVER_ERROR (500) when receiving a BAD_REQUEST (400) from the connector at an exclusion" in {
        when(
          mockExclusionListController.tierConnector
            .excludeEiLPersonFromBik(argEq(iabdString), any[EmpRef], any[Int], argEq(eilPerson))(
              any[HeaderCarrier],
              any[Request[_]]
            )
        ).thenReturn(Future.successful(EiLResponse(BAD_REQUEST, List.empty)))

        val resultForCyp1 = mockExclusionListController.commitExclusion(cyp1, iabdString, dateRange, Some(eilPerson))
        val resultForCy   = mockExclusionListController.commitExclusion(cy, iabdString, dateRange, Some(eilPerson))

        status(resultForCyp1) mustBe INTERNAL_SERVER_ERROR
        status(resultForCy) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
