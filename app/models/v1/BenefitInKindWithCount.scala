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

package models.v1

import models.v1.IabdType.IabdType
import models.v1.PbikStatus.PbikStatus
import play.api.libs.json.{Json, OWrites, Reads}

case class BenefitInKindWithCount(
  iabdType: IabdType,
  payrolledBenefitInKindStatus: PbikStatus,
  payrolledBenefitInKindExclusionCount: Int
)

object BenefitInKindWithCount {

  implicit val writes: OWrites[BenefitInKindWithCount] = Json.writes[BenefitInKindWithCount]

  implicit val reads: Reads[BenefitInKindWithCount] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (
      (__ \ "iabdType").read[IabdType] and
        (__ \ "payrolledBenefitInKindStatus").read[PbikStatus] and
        (__ \ "payrolledBenefitInKindExclusionCount").read[Int]
    )(BenefitInKindWithCount.apply _)
  }

}
