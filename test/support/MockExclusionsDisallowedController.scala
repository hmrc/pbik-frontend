/*
 * Copyright 2019 HM Revenue & Customs
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

import config.{LocalFormPartialRetriever, PbikAppConfig, PbikContext}
import connectors.HmrcTierConnector
import controllers.ExternalUrls
import controllers.actions.{AuthAction, NoSessionCheckAction}
import javax.inject.Inject
import play.api.i18n.MessagesApi
import play.api.{Configuration, Environment}
import services.{BikListService, EiLListService}
import utils.{ControllersReferenceData, FormMappings, SplunkLogger, TaxDateUtils, URIInformation}

class MockExclusionsDisallowedController @Inject()(formMappings: FormMappings,
                                                   messagesApi: MessagesApi,
                                                   pbikAppConfig: PbikAppConfig,
                                                   authenticate: AuthAction,
                                                   noSessionCheck: NoSessionCheckAction,
                                                   eiLListService: EiLListService,
                                                   bikListService: BikListService,
                                                   tierConnector: HmrcTierConnector,
                                                   runModeConfiguration: Configuration,
                                                   environment:Environment,
                                                   context: PbikContext,
                                                   taxDateUtils: TaxDateUtils,
                                                   splunkLogger: SplunkLogger,
                                                   controllersReferenceData: ControllersReferenceData,
                                                   uriInformation: URIInformation,
                                                   externalURLs: ExternalUrls,
                                                   localFormPartialRetriever: LocalFormPartialRetriever) extends MockExclusionListController(
  messagesApi,
  formMappings,
  pbikAppConfig,
  authenticate,
  noSessionCheck,
  eiLListService,
  bikListService,
  tierConnector,
  runModeConfiguration,
  environment,
  context,
  taxDateUtils,
  splunkLogger,
  controllersReferenceData,
  uriInformation,
  externalURLs,
  localFormPartialRetriever) {
  override lazy val exclusionsAllowed = false
}