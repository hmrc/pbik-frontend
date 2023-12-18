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

package controllers

import config._
import connectors.{BikResponse, HmrcTierConnector}
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.i18n.Lang
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{BikListService, SessionService}
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.time.TaxYear
import utils._

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhatNextPageControllerSpec extends PlaySpec with FakePBIKApplication {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(configMap)
    .overrides(bind[AuthAction].to(classOf[TestAuthAction]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .overrides(bind[BikListService].toInstance(mock(classOf[StubBikListService])))
    .overrides(bind[HmrcTierConnector].toInstance(mock(classOf[HmrcTierConnector])))
    .overrides(bind[SessionService].toInstance(mock(classOf[SessionService])))
    .build()

  implicit val lang: Lang                                             = Lang("en-GB")
  implicit val request: FakeRequest[AnyContentAsEmpty.type]           = mockRequest
  implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
    AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)

  private lazy val CYCache: List[Bik] = List.tabulate(noOfElements)(n => Bik("" + (n + 1), statusValue))

  private val (noOfElements, statusValue): (Int, Int)        = (21, 10)
  private val responseHeaders: Map[String, String]           = Map(
    HeaderTags.ETAG   -> "0",
    HeaderTags.X_TXID -> "1"
  )
  private val whatNextPageController: WhatNextPageController = app.injector.instanceOf[WhatNextPageController]

  private class StubBikListService @Inject() (
    pbikAppConfig: AppConfig,
    tierConnector: HmrcTierConnector,
    controllersReferenceData: ControllersReferenceData,
    uriInformation: URIInformation
  ) extends BikListService(
        pbikAppConfig,
        tierConnector,
        controllersReferenceData,
        uriInformation
      ) {

    override def currentYearList(implicit
      hc: HeaderCarrier,
      request: AuthenticatedRequest[_]
    ): Future[BikResponse] =
      Future.successful(
        BikResponse(
          Map(HeaderTags.ETAG -> "1"),
          CYCache.filter { x: Bik =>
            Integer.parseInt(x.iabdType) <= 10
          }
        )
      )

    override def nextYearList(implicit
      hc: HeaderCarrier,
      request: AuthenticatedRequest[_]
    ): Future[BikResponse] =
      Future.successful(
        BikResponse(
          Map(HeaderTags.ETAG -> "1"),
          CYCache.filter { x: Bik =>
            Integer.parseInt(x.iabdType) > 10
          }
        )
      )

  }

  "WhatNextPageController" when {
    "showWhatNextRegisteredBik" should {
      when(whatNextPageController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                sessionId,
                Some(RegistrationList(active = List(RegistrationItem("30", active = true, enabled = true)))),
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
      def test(year: String): Unit =
        s"state the status is ok and display correct page for year $year for a Single benefit (Register a BIK)" in {
          val result = whatNextPageController.showWhatNextRegisteredBik(year).apply(authenticatedRequest)

          status(result) mustBe OK
          contentAsString(result) must include("Registration complete")
          contentAsString(result) must include(
            "Benefits and expenses you have just registered to tax through your payroll from 6 April"
          )
        }

      Seq("cy1", "cy").foreach(test)

      "state the status is ok and display correct page for Multiple benefits (Register a BIK)" in {
        when(whatNextPageController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  Some(
                    RegistrationList(active =
                      List(
                        RegistrationItem("30", active = true, enabled = true),
                        RegistrationItem("8", active = true, enabled = true)
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
        val result = whatNextPageController.showWhatNextRegisteredBik("cy1").apply(authenticatedRequest)

        status(result) mustBe OK
        contentAsString(result) must include("Registration complete")
        contentAsString(result) must include("Private medical treatment or insurance")
        contentAsString(result) must include("Services supplied")
      }
    }

    "showWhatNextRemovedBik" should {
      "state the status is ok and display correct page (Remove a BIK)" in {
        when(whatNextPageController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  None,
                  Some(RegistrationItem("30", active = true, enabled = true)),
                  None,
                  None,
                  None,
                  None,
                  None
                )
              )
            )
          )
        val result = whatNextPageController.showWhatNextRemovedBik("medical").apply(authenticatedRequest)

        status(result) mustBe OK
        contentAsString(result) must include("Benefit removed")
        contentAsString(result) must include(
          "You will not be taxing Private medical treatment or insurance through your payroll from 6 April"
        )
      }
    }

    "calculateTaxYear" should {
      val startYear = TaxYear.taxYearFor(LocalDate.now())

      "return this tax year and next year if given true" in {
        val result: (Int, Int) = whatNextPageController.calculateTaxYear(isCurrentTaxYear = true)

        assert(result._1 == startYear.startYear)
        assert(result._2 == startYear.startYear + 1)
      }

      "return next year and the year after if given false" in {
        val result: (Int, Int) = whatNextPageController.calculateTaxYear(isCurrentTaxYear = false)

        assert(result._1 == startYear.startYear + 1)
        assert(result._2 == startYear.startYear + 2)
      }
    }
  }
}
