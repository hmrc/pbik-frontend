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

package models.v1.trace

import play.api.libs.json.{Json, OFormat}

case class TracePerson(
  identifier: String,
  firstForename: String,
  secondForename: Option[String],
  surname: String,
  worksPayrollNumber: Option[String],
  optimisticLock: Int
) {

  override def equals(obj: Any): Boolean =
    obj.isInstanceOf[TracePerson] && obj.asInstanceOf[TracePerson].identifier == identifier

  override def hashCode: Int = identifier.hashCode

  def fullName: String = s"$firstForename ${secondForename.getOrElse("")} $surname"

  def getWorksPayrollNumber: String = worksPayrollNumber.getOrElse("UNKNOWN")

}

object TracePerson {
  implicit val formats: OFormat[TracePerson] = Json.format[TracePerson]
}
