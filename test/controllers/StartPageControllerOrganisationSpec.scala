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

import controllers.actions.{AuthAction, NoSessionCheckAction}
import models._
import models.v1.{IabdType, PbikAction}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.Helpers._
import services.BikListService
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.HeaderCarrier
import utils.Exceptions.InvalidYearURIException
import utils._

import scala.concurrent.Future

class StartPageControllerOrganisationSpec extends PlaySpec with FakePBIKApplication {

  private val bikListService: BikListService = mock[BikListService]

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[AuthAction].to(classOf[TestAuthActionOrganisation]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .overrides(bind[BikListService].to(bikListService))
    .build()

  val lang: Lang   = Lang("en")
  val cyLang: Lang = Lang("cy")

  private val messages: Messages                                 = fakeApplication.injector.instanceOf[MessagesApi].preferred(Seq(lang))
  private val cyMessages: Messages                               = fakeApplication.injector.instanceOf[MessagesApi].preferred(Seq(cyLang))
  private val startPageController: StartPageController           = fakeApplication.injector.instanceOf[StartPageController]
  private val formMappings: FormMappings                         = fakeApplication.injector.instanceOf[FormMappings]
  private val controllersReferenceData: ControllersReferenceData =
    fakeApplication.injector.instanceOf[ControllersReferenceData]
  private val bikResponseWithBenefits                            = BikResponse(
    Map.empty,
    List(Bik(IabdType.CarBenefit.id.toString, PbikAction.ReinstatePayrolledBenefitInKind.id))
  )
  private val bikResponseEmpty                                   = BikResponse(
    Map.empty,
    List.empty
  )
  implicit val hc: HeaderCarrier                                 = HeaderCarrier()

  val organisationRequest: AuthenticatedRequest[AnyContentAsEmpty.type]      =
    AuthenticatedRequest(
      EmpRef("taxOfficeNumber", "taxOfficeReference"),
      UserName(Name(None, None)),
      mockRequest,
      organisationClient
    )
  val organisationRequestWelsh: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(
      EmpRef("taxOfficeNumber", "taxOfficeReference"),
      UserName(Name(None, None)),
      mockWelshRequest,
      organisationClient
    )

  val organisationRequestWithCYForm: AuthenticatedRequest[AnyContentAsFormUrlEncoded]      = organisationRequest.copy(
    request = mockRequest
      .withFormUrlEncodedBody(
        formMappings.selectYearForm.fill(SelectYear(FormMappingsConstants.CY)).data.toSeq: _*
      )
  )
  val organisationRequestWithCY1Form: AuthenticatedRequest[AnyContentAsFormUrlEncoded]     = organisationRequest.copy(
    request = mockRequest
      .withFormUrlEncodedBody(
        formMappings.selectYearForm.fill(SelectYear(FormMappingsConstants.CYP1)).data.toSeq: _*
      )
  )
  val organisationRequestWithInvalidForm: AuthenticatedRequest[AnyContentAsFormUrlEncoded] = organisationRequest.copy(
    request = mockRequest
      .withFormUrlEncodedBody(formMappings.selectYearForm.fill(SelectYear("invalid-year")).data.toSeq: _*)
  )

  "StartPageController - organisation" when {
    ".onPageLoad" must {
      "return OK and the correct view for a GET - English" in {
        val result = startPageController.onPageLoad().apply(organisationRequest)

        status(result) mustEqual OK
        contentAsString(result) must include(messages("StartPage.heading." + organisationRequest.userType))
        contentAsString(result) must include(messages("StartPage.p5." + organisationRequest.userType))
      }

      "return OK and the correct view for a GET - Welsh" in {
        val result = startPageController.onPageLoad().apply(organisationRequestWelsh)

        status(result) mustEqual OK
        contentAsString(result) must include(cyMessages("StartPage.heading." + organisationRequestWelsh.userType))
        contentAsString(result) must include(cyMessages("StartPage.p5." + organisationRequestWelsh.userType))
      }
    }

    ".selectYearPage" must {
      "return OK and the correct view for a GET - when CY data exists" in {
        when(bikListService.currentYearList(any(), any())).thenReturn(Future.successful(bikResponseWithBenefits))
        val result = startPageController.selectYearPage().apply(organisationRequest)

        status(result) mustEqual OK
        contentAsString(result) must include(messages("SelectYear.heading"))
        contentAsString(result) must include(
          messages("SelectYear.option1", controllersReferenceData.yearRange.cy.toString)
        )
        contentAsString(result) must include(messages("SelectYear.option2"))
      }

      "return REDIRECT and the correct view for a GET - when no CY data" in {
        when(bikListService.currentYearList(any(), any())).thenReturn(Future.successful(bikResponseEmpty))
        val result = startPageController.selectYearPage().apply(organisationRequest)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomePageController.onPageLoadCY1.url)
      }
    }

    ".submitSelectYearPage" must {
      "return OK and the correct view with error for a POST - when invalid form submit" in {
        val result = startPageController.submitSelectYearPage().apply(organisationRequest)

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(messages("Service.errorSummary.heading"))
        contentAsString(result) must include(
          messages("SelectYear.error.empty", controllersReferenceData.yearRange.cy.toString)
        )
      }

      "return REDIRECT and the correct view for a POST - when CY" in {
        val result = startPageController.submitSelectYearPage().apply(organisationRequestWithCYForm)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomePageController.onPageLoadCY.url)
      }

      "return REDIRECT and the correct view for a POST - when CYP1" in {
        val result = startPageController.submitSelectYearPage().apply(organisationRequestWithCY1Form)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomePageController.onPageLoadCY1.url)
      }

      "return exception for a POST - when invalid tax year" in {
        intercept[InvalidYearURIException] {
          await(startPageController.submitSelectYearPage().apply(organisationRequestWithInvalidForm))
        }
      }
    }
  }

}
