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

package support

import config.PbikAppConfig
import connectors.HmrcTierConnector
import controllers.ExclusionListController
import controllers.actions.{AuthAction, NoSessionCheckAction}
import org.scalatest.concurrent.Futures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import services.{BikListService, EiLListService, SessionService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import utils._
import views.html.ErrorPage
import views.html.exclusion._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MockExclusionListController @Inject() (
  messagesApi: MessagesApi,
  formMappings: FormMappings,
  cc: MessagesControllerComponents,
  pbikAppConfig: PbikAppConfig,
  authenticate: AuthAction,
  noSessionCheck: NoSessionCheckAction,
  eiLListService: EiLListService,
  bikListService: BikListService,
  sessionService: SessionService,
  tierConnector: HmrcTierConnector,
  runModeConfiguration: Configuration,
  taxDateUtils: TaxDateUtils,
  splunkLogger: SplunkLogger,
  controllersReferenceData: ControllersReferenceData,
  exclusionOverviewView: ExclusionOverview,
  errorPageView: ErrorPage,
  exclusionNinoOrNoNinoFormView: ExclusionNinoOrNoNinoForm,
  ninoExclusionSearchFormView: NinoExclusionSearchForm,
  noNinoExclusionSearchFormView: NoNinoExclusionSearchForm,
  searchResultsView: SearchResults,
  whatNextExclusionView: WhatNextExclusion,
  removalConfirmationView: RemovalConfirmation,
  whatNextRescindView: WhatNextRescind
)(implicit ec: ExecutionContext)
    extends ExclusionListController(
      formMappings,
      authenticate,
      cc,
      messagesApi,
      noSessionCheck,
      eiLListService,
      bikListService,
      sessionService,
      tierConnector,
      taxDateUtils,
      splunkLogger,
      controllersReferenceData,
      runModeConfiguration,
      exclusionOverviewView,
      errorPageView,
      exclusionNinoOrNoNinoFormView,
      ninoExclusionSearchFormView,
      noNinoExclusionSearchFormView,
      searchResultsView,
      whatNextExclusionView,
      removalConfirmationView,
      whatNextRescindView
    )
    with Futures {

  implicit val defaultPatience: PatienceConfig = {
    val fiveSeconds       = 5
    val fiveHundredMillis = 500
    PatienceConfig(timeout = Span(fiveSeconds, Seconds), interval = Span(fiveHundredMillis, Millis))
  }

  def logSplunkEvent(dataEvent: DataEvent): Future[AuditResult] =
    Future.successful(AuditResult.Success)

  override lazy val exclusionsAllowed: Boolean = true
}
