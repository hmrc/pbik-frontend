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

package support

import config.PbikAppConfig
import connectors.PbikConnector
import controllers.ExclusionListController
import controllers.actions.{AuthAction, NoSessionCheckAction}
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import services.{BikListService, ExclusionService, SessionService}
import utils._
import views.html.ErrorPage
import views.html.exclusion._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class MockExclusionListController @Inject() (
  messagesApi: MessagesApi,
  formMappings: FormMappings,
  cc: MessagesControllerComponents,
  pbikAppConfig: PbikAppConfig,
  authenticate: AuthAction,
  noSessionCheck: NoSessionCheckAction,
  exclusionService: ExclusionService,
  bikListService: BikListService,
  sessionService: SessionService,
  tierConnector: PbikConnector,
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
      exclusionService,
      bikListService,
      sessionService,
      tierConnector,
      taxDateUtils,
      splunkLogger,
      controllersReferenceData,
      pbikAppConfig,
      exclusionOverviewView,
      errorPageView,
      exclusionNinoOrNoNinoFormView,
      ninoExclusionSearchFormView,
      noNinoExclusionSearchFormView,
      searchResultsView,
      whatNextExclusionView,
      removalConfirmationView,
      whatNextRescindView
    ) {

  override val exclusionsAllowed: Boolean = true

}
