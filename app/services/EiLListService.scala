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

package services

import config.PbikAppConfig
import connectors.{HmrcTierConnector, TierConnector}
import controllers.auth.EpayeUser
import models.{AuthenticatedRequest, EiLPerson}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{ControllersReferenceData, SplunkLogger, URIInformation}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EiLListService extends EiLListService {
  def pbikAppConfig: PbikAppConfig.type = PbikAppConfig
  val tierConnector = new HmrcTierConnector
}

trait EiLListService extends TierConnector with URIInformation
with ControllersReferenceData with EpayeUser
with SplunkLogger {

  def currentYearEiL(iabdType: String, year: Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[List[EiLPerson]] = {
    val response = tierConnector.genericGetCall[List[EiLPerson]](baseUrl,
      exclusionGetPath(iabdType),
      request.empRef, year)

    response.map {
      resultList: List[EiLPerson] =>
        resultList.distinct
    }
  }

  def searchResultsRemoveAlreadyExcluded(existingEiL: List[EiLPerson], searchResultsEiL: List[EiLPerson]): List[EiLPerson] = {
    searchResultsEiL diff  existingEiL
  }
}
