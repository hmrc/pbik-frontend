/*
 * Copyright 2025 HM Revenue & Customs
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

package models.v1

import play.api.libs.json.{Json, OWrites, Reads}

case class BenefitListResponse(
  pbikRegistrationDetailsList: Option[List[BenefitInKindWithCount]],
  currentEmployerOptimisticLock: Int
) {

  def getBenefitInKindWithCount: List[BenefitInKindWithCount] = pbikRegistrationDetailsList.getOrElse(List.empty)

}

object BenefitListResponse {

  implicit val writes: OWrites[BenefitListResponse] = Json.writes[BenefitListResponse]

  implicit val reads: Reads[BenefitListResponse] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (
      (__ \ "pbikRegistrationDetailsList").readNullable[List[BenefitInKindWithCount]] and
        (__ \ "currentEmployerOptimisticLock").read[Int]
    )(BenefitListResponse.apply _)
  }

}
