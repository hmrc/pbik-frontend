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

package services

import base.FakePBIKApplication
import connectors.PbikConnector
import controllers.actions.MinimalAuthAction
import models.auth.AuthenticatedRequest
import models.v1.IabdType.IabdType
import models.v1.{BenefitInKindWithCount, BenefitListResponse, IabdType, PbikStatus}
import org.mockito.ArgumentMatchers.{any, anyInt, eq => argEq}
import org.mockito.Mockito._
import play.api.Application
import play.api.http.Status.OK
import play.api.i18n.{Lang, Messages, MessagesApi}
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

  private val benefitTypes: Set[IabdType]            = IabdType.values
  private val cyCache: Set[BenefitInKindWithCount]   =
    benefitTypes
      .map(n => BenefitInKindWithCount(n, PbikStatus.ValidPayrollingBenefitInKind, 3))
      .filter(_.iabdType != IabdType.OtherItems)
  private val cyp1Cache: Set[BenefitInKindWithCount] =
    benefitTypes.map(n => BenefitInKindWithCount(n, PbikStatus.ValidPayrollingBenefitInKind, 3))

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[MinimalAuthAction].to(classOf[TestMinimalAuthAction]))
    .overrides(bind[BikListService].toInstance(mockBikListService))
    .overrides(bind[PbikConnector].toInstance(mockConnector))
    .build()

  lazy val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]
  lazy val cy: Int                    = taxDateUtils.getCurrentTaxYear()
  lazy val cyp1: Int                  = taxDateUtils.getCurrentTaxYear() + 1

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
    reset(mockBikListService)

    when(mockBikListService.getAllBenefitsForYear(anyInt())(any()))
      .thenReturn(Future.successful(benefitTypes))

    // Return instance where not all Biks have been registered for CY
    when(
      mockConnector
        .getRegisteredBiks(any(), argEq(cy))(any())
    ).thenReturn(Future.successful(BenefitListResponse(Some(cyCache.toList), 9)))

    // Return instance where not all Biks have been registered for CYP1
    when(
      mockConnector
        .getRegisteredBiks(any(), argEq(cyp1))(any())
    ).thenReturn(Future.successful(BenefitListResponse(Some(cyp1Cache.toList), 19)))
  }

  private val registrationService: RegistrationService = app.injector.instanceOf[RegistrationService]

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))

  val request: FakeRequest[AnyContentAsEmpty.type]                    = mockRequest
  lazy val nextTaxYearView: NextTaxYear                               = app.injector.instanceOf[NextTaxYear]
  implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = createAuthenticatedRequest(request)
  implicit val hc: HeaderCarrier                                      = HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  "When generating a page which allows registrations, the service" should {
    "return the selection page" in {
      val result =
        registrationService.generateViewForBikRegistrationSelection(
          taxDateUtils.getTaxYearRange().cyminus1,
          nextTaxYearView(_, additive = true, taxDateUtils.getTaxYearRange(), _, _, _, _)
        )

      status(result) mustBe OK
      contentAsString(result) must include(messages("AddBenefits.Heading"))
      contentAsString(result) must include(messages(s"BenefitInKind.label.${IabdType.OtherItems.id}"))
    }
  }

}
