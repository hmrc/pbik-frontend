/*
 * Copyright 2025 HM Revenue & Customs
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
import models.v1.IabdType.IabdType
import models.v1.{BenefitInKindWithCount, BenefitListResponse, IabdType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.Helpers._
import services.{BikListService, SessionService}
import utils._

import scala.concurrent.Future

class WhatNextPageControllerSpec extends FakePBIKApplication {

  private val mockBikListService: BikListService = mock(classOf[BikListService])
  private val mockConnector: PbikConnector       = mock(classOf[PbikConnector])
  private val mockSessionService: SessionService = mock(classOf[SessionService])

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[AuthAction].to(classOf[TestAuthActionOrganisation]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .overrides(bind[BikListService].toInstance(mockBikListService))
    .overrides(bind[PbikConnector].toInstance(mockConnector))
    .overrides(bind[SessionService].toInstance(mockSessionService))
    .build()

  implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = createAuthenticatedRequest(mockRequest)

  private val iabdType: IabdType                             = IabdType.MedicalInsurance
  private val whatNextPageController: WhatNextPageController = injected[WhatNextPageController]

  private val cyBenefits   = IabdType.values.toList
    .slice(2, 7)
    .map(iabd => BenefitInKindWithCount(iabd, 1))
  private val cyp1Benefits = IabdType.values.toList
    .slice(10, 15)
    .map(iabd => BenefitInKindWithCount(iabd, 4))

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockBikListService)
    reset(mockConnector)
    reset(mockSessionService)

    when(mockBikListService.currentYearList(any(), any()))
      .thenReturn(Future.successful(BenefitListResponse(Some(cyBenefits), 5)))
    when(mockBikListService.nextYearList(any(), any()))
      .thenReturn(Future.successful(BenefitListResponse(Some(cyp1Benefits), 5)))
  }

  "WhatNextPageController" when {
    "showWhatNextRegisteredBik" should {
      def test(year: String): Unit =
        s"state the status is ok and display correct page for year $year for a Single benefit (Register a BIK)" in {
          when(mockSessionService.fetchPbikSession()(any()))
            .thenReturn(
              Future.successful(
                Some(
                  PbikSession(
                    sessionId,
                    Some(RegistrationList(active = List(RegistrationItem(iabdType, active = true, enabled = true)))),
                    None,
                    None,
                    None,
                    None,
                    None,
                    None
                  )
                )
              )
            )
          val result = whatNextPageController.showWhatNextRegisteredBik(year).apply(authenticatedRequest)

          status(result) mustBe OK
          contentAsString(result) must include("Registration complete")
          contentAsString(result) must include(
            "Benefits and expenses you have registered to tax through your payroll from 6 April"
          )
        }

      Seq(utils.FormMappingsConstants.CYP1, utils.FormMappingsConstants.CY).foreach(test)

      "state the status is ok and display correct page for Multiple benefits (Register a BIK)" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  Some(
                    RegistrationList(active =
                      List(
                        RegistrationItem(iabdType, active = true, enabled = true),
                        RegistrationItem(IabdType.EmployerProvidedServices, active = true, enabled = true)
                      )
                    )
                  ),
                  None,
                  None,
                  None,
                  None,
                  None,
                  None
                )
              )
            )
          )
        val result =
          whatNextPageController.showWhatNextRegisteredBik(FormMappingsConstants.CYP1).apply(authenticatedRequest)

        status(result) mustBe OK
        contentAsString(result) must include("Registration complete")
        contentAsString(result) must include("Private medical treatment or insurance")
        contentAsString(result) must include("Services supplied")
      }
    }

    "showWhatNextRemovedBik" should {
      "state the status is ok and display correct page (Remove a BIK)" in {
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
        val result = whatNextPageController.showWhatNextRemovedBik(iabdType).apply(authenticatedRequest)

        status(result) mustBe OK
        contentAsString(result) must include("Benefit removed")
        contentAsString(result) must include(
          "You have removed Private medical treatment or insurance from being taxed through payroll from 6 April"
        )
      }
    }
  }
}
