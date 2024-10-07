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

import connectors.PbikConnector
import models.v1.IabdType.IabdType
import models.v1.exclusion.PbikExclusions
import models.v1.trace.TracePersonResponse
import play.api.Logging
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.http.HeaderCarrier
import utils.Exceptions.GenericServerErrorException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExclusionService @Inject() (tierConnector: PbikConnector)(implicit ec: ExecutionContext) extends Logging {

  def exclusionListForYear(iabdType: IabdType, year: Int, empRef: EmpRef)(implicit
    hc: HeaderCarrier
  ): Future[PbikExclusions] = {
    val response = tierConnector.getAllExcludedEiLPersonForBik(
      iabdType,
      empRef,
      year
    )

    response.flatMap {
      case Right(eilList) =>
        Future.successful(eilList)
      case Left(error)    =>
        logger.error(
          s"[ExclusionService][exclusionListForYear] Error getting pbik exclusions for ${iabdType.toString} and $year: $error"
        )
        Future.failed(
          new GenericServerErrorException(s"Error getting pbik exclusions for ${iabdType.toString} and $year")
        )
    }
  }

  def searchResultsRemoveAlreadyExcluded(
    existingEiL: List[TracePersonResponse],
    searchResultsEiL: List[TracePersonResponse]
  ): List[TracePersonResponse] =
    searchResultsEiL diff existingEiL

}
