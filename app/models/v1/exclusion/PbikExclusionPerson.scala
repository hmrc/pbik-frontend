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

import play.api.libs.json.{Json, OFormat}

case class PbikExclusionPerson(
  nationalInsuranceNumber: String,
  firstForename: String,
  secondForename: Option[String],
  surname: String,
  worksPayrollNumber: Option[String],
  optimisticLock: Int
) {

  override def equals(obj: Any): Boolean =
    obj.isInstanceOf[PbikExclusionPerson] && obj
      .asInstanceOf[PbikExclusionPerson]
      .nationalInsuranceNumber == nationalInsuranceNumber

  override def hashCode: Int = nationalInsuranceNumber.hashCode

  def fullName: String = s"$firstForename $surname"

}

object PbikExclusionPerson {
  implicit val formats: OFormat[PbikExclusionPerson] = Json.format[PbikExclusionPerson]
}
