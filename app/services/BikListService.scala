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

package services

import config.AppConfig
import connectors.PbikConnector
import models.{AuthenticatedRequest, Bik, BikResponse, EmpRef}
import uk.gov.hmrc.http.HeaderCarrier
import utils.ControllersReferenceData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BikListService @Inject() (
  val pbikAppConfig: AppConfig,
  val tierConnector: PbikConnector,
  controllersReferenceData: ControllersReferenceData
)(implicit ec: ExecutionContext) {

  def currentYearList(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[BikResponse] =
    tierConnector
      .getRegisteredBiks(request.empRef, controllersReferenceData.yearRange.cyminus1)
      .map(response => BikResponse(response.headers, response.bikList.distinct))

  def nextYearList(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[BikResponse] =
    tierConnector
      .getRegisteredBiks(request.empRef, controllersReferenceData.yearRange.cy)
      .map(response => BikResponse(response.headers, response.bikList.distinct))

  def registeredBenefitsList(year: Int, empRef: EmpRef)(implicit hc: HeaderCarrier): Future[List[Bik]] =
    if (empRef.taxOfficeNumber == "" && empRef.taxOfficeReference == "") {
      tierConnector.getAllAvailableBiks(year)
    } else {
      tierConnector.getRegisteredBiks(empRef, year).map(_.bikList)
    }
}
