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

package support

import config.PbikAppConfig
import connectors.PbikConnector
import models._
import services.BikListService
import uk.gov.hmrc.http.HeaderCarrier
import utils.ControllersReferenceData

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StubbedBikListService @Inject() (
  pbikAppConfig: PbikAppConfig,
  tierConnector: PbikConnector,
  controllersReferenceData: ControllersReferenceData
)(implicit ec: ExecutionContext)
    extends BikListService(
      pbikAppConfig,
      tierConnector,
      controllersReferenceData
    ) {

  //scalastyle:off magic.number
  lazy val CYCache: List[Bik] = List.range(3, 32).map(n => Bik("" + n, 10))
  //scalastyle:on magic.number
  /*(n => new Bik("" + (n + 1), 10))*/

  override def currentYearList(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[BikResponse] =
    Future.successful(
      BikResponse(
        HeaderTags.createResponseHeaders(),
        CYCache.filter { x: Bik =>
          Integer.parseInt(x.iabdType) == 31
        }
      )
    )

  override def nextYearList(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[BikResponse] =
    Future.successful(
      BikResponse(
        HeaderTags.createResponseHeaders(),
        CYCache.filter { x: Bik =>
          Integer.parseInt(x.iabdType) == 31
        }
      )
    )

  override def registeredBenefitsList(year: Int, empRef: EmpRef)(implicit
    hc: HeaderCarrier
  ): Future[List[Bik]] =
    Future.successful(CYCache)

}
