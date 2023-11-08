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

import config.AppConfig
import connectors.HmrcTierConnector
import models.{AuthenticatedRequest, Bik, EmpRef, HeaderTags}
import services.BikListService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{ControllersReferenceData, URIInformation}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StubbedBikListService @Inject() (
  pbikAppConfig: AppConfig,
  tierConnector: HmrcTierConnector,
  controllersReferenceData: ControllersReferenceData,
  uriInformation: URIInformation
)(implicit ec: ExecutionContext)
    extends BikListService(
      pbikAppConfig,
      tierConnector,
      controllersReferenceData,
      uriInformation
    ) {

  //scalastyle:off magic.number
  lazy val CYCache: List[Bik]                        = List.range(3, 32).map(n => Bik("" + n, 10))
  //scalastyle:on magic.number
  /*(n => new Bik("" + (n + 1), 10))*/
  override lazy val pbikHeaders: Map[String, String] = Map(HeaderTags.ETAG -> "0", HeaderTags.X_TXID -> "1")

  override def currentYearList(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[(Map[String, String], List[Bik])] =
    Future.successful(
      (
        Map(HeaderTags.ETAG -> "1"),
        CYCache.filter { x: Bik =>
          Integer.parseInt(x.iabdType) == 31
        }
      )
    )

  override def getNextYearList(implicit
                               hc: HeaderCarrier,
                               request: AuthenticatedRequest[_]
  ): Future[(Map[String, String], List[Bik])] =
    Future.successful(
      (
        Map(HeaderTags.ETAG -> "1"),
        CYCache.filter { x: Bik =>
          Integer.parseInt(x.iabdType) == 31
        }
      )
    )

  override def registeredBenefitsList(year: Int, empRef: EmpRef)(path: String)(implicit
    hc: HeaderCarrier
  ): Future[List[Bik]] =
    Future.successful(CYCache)

}
