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

import config.PbikAppConfig
import connectors.PbikConnector
import models.AuthenticatedRequest
import models.v1.exclusion.{PbikExclusionPerson, PbikExclusions}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Exceptions.GenericServerErrorException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EiLListService @Inject() (
  val pbikAppConfig: PbikAppConfig,
  val tierConnector: PbikConnector
)(implicit ec: ExecutionContext) {

  def currentYearEiL(iabdString: String, year: Int)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[PbikExclusions] = {
    val response = tierConnector.getAllExcludedEiLPersonForBik(
      iabdString,
      request.empRef,
      year
    )

    response.flatMap {
      case Right(eilList) => Future.successful(eilList)
      case Left(error)    =>
        Future.failed(
          new GenericServerErrorException(s"Error getting pbik exclusions for $iabdString and $year: $error")
        )
    }
  }

  def searchResultsRemoveAlreadyExcluded(
    existingEiL: List[PbikExclusionPerson],
    searchResultsEiL: List[PbikExclusionPerson]
  ): List[PbikExclusionPerson] =
    searchResultsEiL diff existingEiL

}
