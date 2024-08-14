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
import models.v1.IabdType.IabdType
import models.v1.{IabdType, PbikAction, PbikStatus}
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

  lazy val CYCache: List[Bik] = IabdType.values.toList
    .map(value => Bik(value.id.toString, PbikStatus.ValidPayrollingBenefitInKind.id))

  override def currentYearList(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[BikResponse] =
    Future.successful(
      BikResponse(
        HeaderTags.createResponseHeaders(),
        Set(Bik(IabdType.CarBenefit.id.toString, PbikAction.ReinstatePayrolledBenefitInKind.id))
      )
    )

  override def nextYearList(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[BikResponse] =
    Future.successful(
      BikResponse(
        HeaderTags.createResponseHeaders(),
        Set(Bik(IabdType.CarBenefit.id.toString, PbikAction.ReinstatePayrolledBenefitInKind.id))
      )
    )

  override def registeredBenefitsList(year: Int, empRef: EmpRef)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[List[Bik]] =
    Future.successful(CYCache)

  override def getAllBenefitsForYear(year: Int)(implicit hc: HeaderCarrier): Future[Set[IabdType]] =
    Future.successful(IabdType.values)

}
