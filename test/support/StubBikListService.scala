/*
 * Copyright 2022 HM Revenue & Customs
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

import config.AppConfig
import connectors.HmrcTierConnector
import javax.inject.Inject
import models.HeaderTags
import services.BikListService
import utils.{ControllersReferenceData, URIInformation}

class StubBikListService @Inject()(
  pbikAppConfig: AppConfig,
  tierConnector: HmrcTierConnector,
  controllersReferenceData: ControllersReferenceData,
  uriInformation: URIInformation)
    extends BikListService(pbikAppConfig, tierConnector, controllersReferenceData, uriInformation) {
  override lazy val pbikHeaders: Map[String, String] = Map(HeaderTags.ETAG -> "1", HeaderTags.X_TXID -> "1")
}
