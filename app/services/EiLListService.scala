/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.HmrcTierConnector
import javax.inject.Inject
import models.{AuthenticatedRequest, EiLPerson}
import uk.gov.hmrc.http.HeaderCarrier
import utils.URIInformation

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EiLListService @Inject()(
  val pbikAppConfig: PbikAppConfig,
  val tierConnector: HmrcTierConnector,
  uRIInformation: URIInformation) {

  def currentYearEiL(iabdType: String, year: Int)(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_]): Future[List[EiLPerson]] = {
    val response = tierConnector.genericGetCall[List[EiLPerson]](
      uRIInformation.baseUrl,
      uRIInformation.exclusionGetPath(iabdType),
      request.empRef,
      year)

    response.map { resultList: List[EiLPerson] =>
      resultList.distinct
    }
  }

  def searchResultsRemoveAlreadyExcluded(
    existingEiL: List[EiLPerson],
    searchResultsEiL: List[EiLPerson]): List[EiLPerson] =
    searchResultsEiL diff existingEiL

}
