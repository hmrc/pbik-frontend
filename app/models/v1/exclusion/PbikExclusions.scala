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

package models.v1.exclusion

import models.v1.trace.TracePersonResponse
import play.api.libs.json.{Json, OFormat}

case class PbikExclusions(currentEmployerOptimisticLock: Int, getPBIKExclusionList: List[PbikExclusionPerson]) {

  def mapToTracePerson: List[TracePersonResponse] = getPBIKExclusionList.map { pbikExclusionPerson =>
    TracePersonResponse(
      pbikExclusionPerson.nationalInsuranceNumber,
      pbikExclusionPerson.firstForename,
      pbikExclusionPerson.secondForename,
      pbikExclusionPerson.surname,
      Some(pbikExclusionPerson.worksPayrollNumber),
      pbikExclusionPerson.optimisticLock
    )
  }
}

object PbikExclusions {
  implicit val formats: OFormat[PbikExclusions] = Json.format[PbikExclusions]
}
