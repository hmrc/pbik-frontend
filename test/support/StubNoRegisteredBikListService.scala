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
import models.{Bik, EmpRef}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json
import services.BikListService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{ControllersReferenceData, URIInformation}

import scala.concurrent.Future

class StubNoRegisteredBikListService @Inject() (
  pbikAppConfig: AppConfig,
  tierConnector: HmrcTierConnector,
  controllersReferenceData: ControllersReferenceData,
  uriInformation: URIInformation
) extends BikListService(pbikAppConfig: AppConfig, tierConnector, controllersReferenceData, uriInformation) {
  //scalastyle:off magic.number
  lazy val CYCache: List[Bik] = List.tabulate(21)(n => Bik("" + (n + 1), 10))
  //scalastyle:on magic.number

  when(
    tierConnector.genericGetCall[List[Bik]](any[String], any[String], any[EmpRef], any[Int])(
      any[HeaderCarrier],
      any[json.Format[List[Bik]]]
    )
  ).thenReturn(Future.successful(CYCache.filter { x: Bik =>
    Integer.parseInt(x.iabdType) > 50
  }))
}
