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

package models

import models.v1.BenefitListResponse
import models.v1.exclusion.{PbikExclusions, SelectedExclusionToRemove}
import models.v1.trace.TracePersonListResponse
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class PbikSession(
  sessionId: String,
  registrations: Option[RegistrationList] = None,
  bikRemoved: Option[RegistrationItem] = None,
  listOfMatches: Option[TracePersonListResponse] = None,
  eiLPerson: Option[SelectedExclusionToRemove] = None,
  currentExclusions: Option[PbikExclusions] = None,
  cyRegisteredBiks: Option[BenefitListResponse] = None,
  nyRegisteredBiks: Option[BenefitListResponse] = None,
  lastUpdated: Instant = Instant.now()
) {

  def getActiveRegistrationItems: Option[List[RegistrationItem]] = this.registrations.map(_.active.filter(_.active))

}

object PbikSession {
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  implicit val pbikSessionFormats: OFormat[PbikSession] = Json.format[PbikSession]
}
