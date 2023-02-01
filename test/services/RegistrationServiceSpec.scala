/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.HmrcTierConnector
import controllers.FakePBIKApplication
import controllers.actions.MinimalAuthAction
import models._
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Application
import play.api.http.Status.OK
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.{TaxDateUtils, TestMinimalAuthAction}
import views.html.registration.NextTaxYear

import scala.concurrent.Future

class RegistrationServiceSpec extends AnyWordSpecLike with Matchers with FakePBIKApplication with I18nSupport {

  override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  override lazy val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .overrides(bind[MinimalAuthAction].to(classOf[TestMinimalAuthAction]))
    .overrides(bind[BikListService].toInstance(mock(classOf[BikListService])))
    .overrides(bind[HmrcTierConnector].toInstance(mock(classOf[HmrcTierConnector])))
    .build()

  private val registrationService: RegistrationService = {

    val service                   = app.injector.instanceOf[RegistrationService]
    val (noOfElements, bikStatus) = (5, 10)

    lazy val CYCache: List[Bik] = List.tabulate(noOfElements)(n => Bik("" + (n + 1), bikStatus))

    when(service.bikListService.pbikHeaders).thenReturn(Map(HeaderTags.ETAG -> "0", HeaderTags.X_TXID -> "1"))

    when(service.bikListService.registeredBenefitsList(any[Int], any[EmpRef])(any[String])(any[HeaderCarrier]))
      .thenReturn(Future.successful(CYCache))

    // Return instance where not all Biks have been registered for CY
    when(
      service.tierConnector.genericGetCall[List[Bik]](
        any[String],
        any[String],
        any[EmpRef],
        argEq(injected[TaxDateUtils].getCurrentTaxYear())
      )(any[HeaderCarrier], any[json.Format[List[Bik]]])
    )
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) <= 3
      }))

    // Return instance where not all Biks have been registered for CYP1
    when(
      service.tierConnector.genericGetCall[List[Bik]](
        any[String],
        any[String],
        any[EmpRef],
        argEq(injected[TaxDateUtils].getCurrentTaxYear() + 1)
      )(any[HeaderCarrier], any[json.Format[List[Bik]]])
    )
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) <= 5
      }))
    service
  }

  "When generating a page which allows registrations, the service" should {
    "return the selection page" in {
      val request: FakeRequest[AnyContentAsEmpty.type]                    = mockRequest
      val nextTaxYearView                                                 = app.injector.instanceOf[NextTaxYear]
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      implicit val hc: HeaderCarrier                                      = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
      val taxDateUtils                                                    = injected[TaxDateUtils]
      val YEAR_RANGE                                                      = taxDateUtils.getTaxYearRange()

      val result =
        registrationService.generateViewForBikRegistrationSelection(
          YEAR_RANGE.cyminus1,
          "add",
          nextTaxYearView(_, additive = true, YEAR_RANGE, _, _, _, _, _, EmpRef.empty)
        )

      status(result)        shouldBe OK
      contentAsString(result) should include(Messages("AddBenefits.Heading"))
      contentAsString(result) should include(Messages("BenefitInKind.label.4"))
    }
  }

}
