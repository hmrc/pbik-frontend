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

package services

import base.FakePBIKApplication
import connectors.PbikConnector
import controllers.actions.MinimalAuthAction
import models.auth.AuthenticatedRequest
import models.v1.IabdType.IabdType
import models.v1.{BenefitInKindWithCount, BenefitListResponse, IabdType}
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito._
import play.api.Application
import play.api.http.Status.OK
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.{TaxDateUtils, TestMinimalAuthAction}
import views.html.registration.NextTaxYear

import scala.concurrent.Future

class RegistrationServiceSpec extends FakePBIKApplication {

  private val mockConnector      = mock(classOf[PbikConnector])
  private val mockBikListService = mock(classOf[BikListService])

  private val benefitTypes: Set[IabdType]          = IabdType.values
  private val cyCache: Set[BenefitInKindWithCount] =
    benefitTypes
      .map(n => BenefitInKindWithCount(n, 3))
      .filter(_.iabdType != IabdType.OtherItems)

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[MinimalAuthAction].to(classOf[TestMinimalAuthAction]))
    .overrides(bind[BikListService].toInstance(mockBikListService))
    .overrides(bind[PbikConnector].toInstance(mockConnector))
    .build()

  lazy val taxDateUtils: TaxDateUtils = injected[TaxDateUtils]
  lazy val cy: Int                    = taxDateUtils.getCurrentTaxYear()
  lazy val cyp1: Int                  = taxDateUtils.getCurrentTaxYear() + 1

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockConnector)
    reset(mockBikListService)

    // CY
    when(mockBikListService.getAllBenefitsForYear(argEq(cy))(any()))
      .thenReturn(Future.successful(benefitTypes))

    when(
      mockConnector
        .getRegisteredBiks(any(), argEq(cy))(any())
    ).thenReturn(Future.successful(BenefitListResponse(Some(cyCache.toList), 9)))

    // CYP1
    when(mockBikListService.getAllBenefitsForYear(argEq(cyp1))(any()))
      .thenReturn(Future.successful(Set.empty))

    when(
      mockConnector
        .getRegisteredBiks(any(), argEq(cyp1))(any())
    ).thenReturn(
      Future.successful(
        BenefitListResponse(
          Some(List(BenefitInKindWithCount(IabdType.Expenses, 34))),
          19
        )
      )
    )
  }

  private val registrationService: RegistrationService = injected[RegistrationService]

  implicit val messages: Messages = injected[MessagesApi].preferred(Seq(lang))

  val request: FakeRequest[AnyContentAsEmpty.type]                    = mockRequest
  lazy val nextTaxYearView: NextTaxYear                               = injected[NextTaxYear]
  implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = createAuthenticatedRequest(request)
  implicit val hc: HeaderCarrier                                      = HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  "RegistrationService" when {

    ".generateViewForBikRegistrationSelection" should {

      "return the selection page" in {
        val result =
          registrationService.generateViewForBikRegistrationSelection(
            taxDateUtils.getTaxYearRange().cyminus1,
            nextTaxYearView(_, additive = true, taxDateUtils.getTaxYearRange(), _, _, _)
          )

        status(result) mustBe OK
        contentAsString(result) must include(messages("AddBenefits.Heading"))
        contentAsString(result) must include(messages(s"BenefitInKind.label.${IabdType.OtherItems.id}"))
      }

      "return the error page if no more benefits to add" in {
        val result =
          registrationService.generateViewForBikRegistrationSelection(
            taxDateUtils.getTaxYearRange().cy,
            nextTaxYearView(_, additive = true, taxDateUtils.getTaxYearRange(), _, _, _)
          )

        status(result) mustBe OK
        contentAsString(result) must include(messages("ErrorPage.noBenefitsToAdd"))
      }

    }

  }
}
