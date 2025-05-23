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

package models.v1.exclusion

import models.v1
import play.api.libs.json.{Format, Json}

object Gender extends Enumeration {

  type Gender = Value

  val Female: v1.exclusion.Gender.Value  = Value(0, "FEMALE")
  val Male: v1.exclusion.Gender.Value    = Value(1, "MALE")
  val Unknown: v1.exclusion.Gender.Value = Value(99, "NOT KNOWN")

  implicit val formats: Format[Gender] = Json.formatEnum(this)

  def fromString(value: String): Gender =
    value.strip().toLowerCase match {
      case "female"    => Female
      case "male"      => Male
      case "not known" => Unknown
      case _           => throw new IllegalArgumentException(s"Invalid Gender passed $value")
    }

}
