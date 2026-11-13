/*
 * Copyright 2026 HM Revenue & Customs
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
import models.auth.AuthenticatedRequest
import models.v1.IabdType.IabdType
import models.v1.{BenefitInKindWithCount, BenefitListResponse, IabdType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{BikListService, SessionService}
import support.TestSplunkLogger
import uk.gov.hmrc.http.SessionKeys
import utils.*
import models.*

import scala.concurrent.Future

class HomePageControllerSpec extends FakePBIKApplication {

  private val mockConnector: PbikConnector       = mock(classOf[PbikConnector])
  private val mockBikListService: BikListService = mock(classOf[BikListService])
  private val mockSessionService: SessionService = mock(classOf[SessionService])

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(bind[PbikConnector].toInstance(mockConnector))
    .overrides(bind[BikListService].to(mockBikListService))
    .overrides(bind[SplunkLogger].to(classOf[TestSplunkLogger]))
    .overrides(bind[AuthAction].to(classOf[TestAuthActionOrganisation]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .build()

  implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = createAuthenticatedRequest(mockRequest)
  private val iabdType: IabdType                                      = IabdType.Accommodation

  private val homePageController: HomePageController = injected[HomePageController]
  private val messages: Messages                     = injected[MessagesApi].preferred(Seq(lang))

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockConnector)
    reset(mockBikListService)

    when(mockBikListService.currentYearList(any(), any()))
      .thenReturn(
        Future.successful(
          BenefitListResponse(
            Some(List(BenefitInKindWithCount(IabdType.CarBenefit, 34))),
            5
          )
        )
      )
    when(mockBikListService.nextYearList(any(), any()))
      .thenReturn(
        Future.successful(
          BenefitListResponse(
            Some(List(BenefitInKindWithCount(IabdType.MedicalInsurance, 35))),
            5
          )
        )
      )

    when(mockBikListService.getAllBenefitsForYear(any())(any()))
      .thenReturn(Future.successful(IabdType.values))
  }

  "HomePageController" when {
    ".notAuthorised" should {
      "return 401 (UNAUTHORIZED) for a notAuthorised method call" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockRequest
        val result                                                = homePageController.notAuthorised()(request)

        status(result) mustBe UNAUTHORIZED
        contentAsString(result) must include(messages("ErrorPage.authorisationError"))
      }
    }

    ".signOut" should {
      "logout and redirect to feedback page" in {
        val result = homePageController.signout(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("sign-out-without-state")
      }
    }

    ".signOutIndividual" should {
      "logout and redirect to sign out page" in {
        val result = homePageController.signOutIndividual(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("individual-signed-out")
      }
    }

    ".signOutNoSurvey" should {
      "logout and redirect to sign out page" in {
        val result = homePageController.signOutNoSurvey(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("signed-you-out")
      }
    }

    ".setLanguage" should {
      "changes from english to welsh" in {
        val result = homePageController.setLanguage()(mockWelshRequest)

        status(result) mustBe SEE_OTHER
      }
    }

    ".onPageLoad" should {
      "show Payrolling summary page" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  None,
                  Some(RegistrationItem(iabdType, active = true, enabled = true)),
                  None,
                  None,
                  None,
                  None,
                  None
                )
              )
            )
          )
        val result = homePageController.onPageLoad.apply(authenticatedRequest)

        status(result) mustBe OK
        contentAsString(result) must include("Payrolling summary")
      }

      "get rid off decomissioned biks for next tax year even when provided" in {
        when(mockBikListService.currentYearList(any(), any()))
          .thenReturn(
            Future.successful(
              BenefitListResponse(
                pbikRegistrationDetailsList = Some(
                  List(
                    BenefitInKindWithCount(IabdType.VouchersAndCreditCards, 1),
                    BenefitInKindWithCount(IabdType.OtherItems, 1),
                    BenefitInKindWithCount(IabdType.Accommodation, 1),
                    BenefitInKindWithCount(IabdType.BeneficialLoan, 1)
                  )
                ),
                currentEmployerOptimisticLock = 1
              )
            )
          )
        when(mockBikListService.nextYearList(any(), any()))
          .thenReturn(
            Future.successful(
              BenefitListResponse(
                pbikRegistrationDetailsList = Some(
                  List(
                    BenefitInKindWithCount(IabdType.CarBenefit, 1),
                    BenefitInKindWithCount(IabdType.MedicalInsurance, 1),
                    BenefitInKindWithCount(IabdType.Accommodation, 1),
                    BenefitInKindWithCount(IabdType.BeneficialLoan, 1)
                  )
                ),
                currentEmployerOptimisticLock = 1
              )
            )
          )
        val result = homePageController.onPageLoad.apply(authenticatedRequest)

        status(result) mustBe OK
        contentAsString(result) mustNot include(messages(s"BenefitInKind.label.${IabdType.CarBenefit.id}"))
        contentAsString(result) mustNot include(messages(s"BenefitInKind.label.${IabdType.MedicalInsurance.id}"))
        contentAsString(result) must include(messages(s"PayrollingSummary.choose.benefit.link.text.organisation"))
      }

      "not allow to register more biks if all already selected for cy+1" in {
        when(mockBikListService.currentYearList(any(), any()))
          .thenReturn(
            Future.successful(
              BenefitListResponse(
                pbikRegistrationDetailsList = Some(
                  List(
                    BenefitInKindWithCount(IabdType.VouchersAndCreditCards, 1),
                    BenefitInKindWithCount(IabdType.OtherItems, 1)
                  )
                ),
                currentEmployerOptimisticLock = 1
              )
            )
          )
        when(mockBikListService.nextYearList(any(), any()))
          .thenReturn(
            Future.successful(
              BenefitListResponse(
                pbikRegistrationDetailsList = Some(
                  List(
                    BenefitInKindWithCount(IabdType.Assets, 1),
                    BenefitInKindWithCount(IabdType.OtherItems, 1),
                    BenefitInKindWithCount(IabdType.Accommodation, 1),
                    BenefitInKindWithCount(IabdType.AssetTransfer, 1),
                    BenefitInKindWithCount(IabdType.Entertaining, 1),
                    BenefitInKindWithCount(IabdType.IncomeTaxPaidButNotDeductedFromDirectorRemuneration, 1),
                    BenefitInKindWithCount(IabdType.BeneficialLoan, 1),
                    BenefitInKindWithCount(IabdType.Mileage, 1),
                    BenefitInKindWithCount(IabdType.NonQualifyingRelocationExpenses, 1),
                    BenefitInKindWithCount(IabdType.Telephone, 1),
                    BenefitInKindWithCount(IabdType.PaymentsOnEmployeeBehalf, 1),
                    BenefitInKindWithCount(IabdType.QualifyingRelocationExpenses, 1),
                    BenefitInKindWithCount(IabdType.EmployerProvidedServices, 1),
                    BenefitInKindWithCount(IabdType.TravelAndSubsistence, 1),
                    BenefitInKindWithCount(IabdType.VouchersAndCreditCards, 1)
                  )
                ),
                currentEmployerOptimisticLock = 1
              )
            )
          )
        val result = homePageController.onPageLoad.apply(authenticatedRequest)

        status(result) mustBe OK
        contentAsString(result) mustNot include(messages(s"PayrollingSummary.choose.benefit.link.text.organisation"))
      }
    }
  }
}
