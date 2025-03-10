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
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models.auth.AuthenticatedRequest
import models.form.SelectYear
import models.v1.{BenefitInKindWithCount, BenefitListResponse, IabdType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.Helpers._
import services.BikListService
import uk.gov.hmrc.http.HeaderCarrier
import utils.Exceptions.InvalidYearURIException
import utils._

import scala.concurrent.Future

class StartPageControllerAgentSpec extends FakePBIKApplication {

  private val bikListService: BikListService = mock(classOf[BikListService])

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[AuthAction].to(classOf[TestAuthActionAgent]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .overrides(bind[BikListService].to(bikListService))
    .build()

  private val messages: Messages                                 = fakeApplication.injector.instanceOf[MessagesApi].preferred(Seq(lang))
  private val cyMessages: Messages                               = fakeApplication.injector.instanceOf[MessagesApi].preferred(Seq(cyLang))
  private val startPageController: StartPageController           = fakeApplication.injector.instanceOf[StartPageController]
  private val formMappings: FormMappings                         = fakeApplication.injector.instanceOf[FormMappings]
  private val controllersReferenceData: ControllersReferenceData =
    fakeApplication.injector.instanceOf[ControllersReferenceData]
  private val bikResponseWithBenefits                            = BenefitListResponse(
    Some(List(BenefitInKindWithCount(IabdType.CarBenefit, 1))),
    13
  )
  private val bikResponseEmpty                                   = BenefitListResponse(None, 0)
  implicit val hc: HeaderCarrier                                 = HeaderCarrier()

  val agentRequest: AuthenticatedRequest[AnyContentAsEmpty.type]      =
    createAuthenticatedRequest(mockRequest, client = agentClient)
  val agentRequestWelsh: AuthenticatedRequest[AnyContentAsEmpty.type] =
    createAuthenticatedRequest(mockWelshRequest, client = agentClient)

  val agentRequestWithCYForm: AuthenticatedRequest[AnyContentAsFormUrlEncoded]      = agentRequest.copy(
    request = mockRequest
      .withFormUrlEncodedBody(
        formMappings.selectYearForm.fill(SelectYear(FormMappingsConstants.CY)).data.toSeq: _*
      )
  )
  val agentRequestWithCY1Form: AuthenticatedRequest[AnyContentAsFormUrlEncoded]     = agentRequest.copy(
    request = mockRequest
      .withFormUrlEncodedBody(
        formMappings.selectYearForm.fill(SelectYear(FormMappingsConstants.CYP1)).data.toSeq: _*
      )
  )
  val agentRequestWithInvalidForm: AuthenticatedRequest[AnyContentAsFormUrlEncoded] = agentRequest.copy(
    request = mockRequest
      .withFormUrlEncodedBody(formMappings.selectYearForm.fill(SelectYear("invalid-year")).data.toSeq: _*)
  )

  "StartPageController - agent" when {
    ".onPageLoad" must {
      "return OK and the correct view for a GET - English" in {
        val result = startPageController.onPageLoad().apply(agentRequest)

        status(result) mustEqual OK
        contentAsString(result) must include(messages("StartPage.heading." + agentRequest.userType))
        contentAsString(result) must include(messages("StartPage.p5." + agentRequest.userType))
      }

      "return OK and the correct view for a GET - Welsh" in {
        val result = startPageController.onPageLoad().apply(agentRequestWelsh)

        status(result) mustEqual OK
        contentAsString(result) must include(cyMessages("StartPage.heading." + agentRequestWelsh.userType))
        contentAsString(result) must include(cyMessages("StartPage.p5." + agentRequestWelsh.userType))
      }
    }

    ".selectYearPage" must {
      "return OK and the correct view for a GET - when CY data exists" in {
        when(bikListService.currentYearList(any(), any())).thenReturn(Future.successful(bikResponseWithBenefits))
        val result = startPageController.selectYearPage().apply(agentRequest)

        status(result) mustEqual OK
        contentAsString(result) must include(messages("SelectYear.heading"))
        contentAsString(result) must include(
          messages("SelectYear.option1", controllersReferenceData.yearRange.cy.toString)
        )
        contentAsString(result) must include(messages("SelectYear.option2"))
      }

      "return REDIRECT and the correct view for a GET - when no CY data" in {
        when(bikListService.currentYearList(any(), any())).thenReturn(Future.successful(bikResponseEmpty))
        val result = startPageController.selectYearPage().apply(agentRequest)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomePageController.onPageLoadCY1.url)
      }
    }

    ".submitSelectYearPage" must {
      "return OK and the correct view with error for a POST - when invalid form submit" in {
        val result = startPageController.submitSelectYearPage().apply(agentRequest)

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages("Service.errorSummary.heading"))
        contentAsString(result) must include(
          messages("SelectYear.error.empty", controllersReferenceData.yearRange.cy.toString)
        )
      }

      "return REDIRECT and the correct view for a POST - when CY" in {
        val result = startPageController.submitSelectYearPage().apply(agentRequestWithCYForm)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomePageController.onPageLoadCY.url)
      }

      "return REDIRECT and the correct view for a POST - when CYP1" in {
        val result = startPageController.submitSelectYearPage().apply(agentRequestWithCY1Form)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomePageController.onPageLoadCY1.url)
      }

      "return exception for a POST - when invalid tax year" in
        intercept[InvalidYearURIException] {
          await(startPageController.submitSelectYearPage().apply(agentRequestWithInvalidForm))
        }
    }
  }
}
