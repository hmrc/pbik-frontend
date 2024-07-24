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

import models.v1
import play.api.libs.json.{Format, Json}

object PbikAction extends Enumeration {

  type PbikAction = Value

  val NoAction: v1.PbikAction.Value                        = Value(0, "No Action")
  val ReinstatePayrolledBenefitInKind: v1.PbikAction.Value = Value(30, "Reinstate Payrolled Benefit In Kind (PBIK)")
  val RemovePayrolledBenefitInKind: v1.PbikAction.Value    = Value(40, "Remove Payrolled Benefit In Kind (PBIK)")

  implicit val formats: Format[PbikAction] = Json.formatEnum(this)

}
