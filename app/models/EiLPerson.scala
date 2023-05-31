/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}

case class EiLPerson(
  nino: String,
  firstForename: String,
  secondForename: Option[String],
  surname: String,
  worksPayrollNumber: Option[String],
  dateOfBirth: Option[String],
  gender: Option[String],
  status: Option[Int],
  perOptLock: Int = 0
) {

  override def equals(obj: Any): Boolean = obj match {
    case EiLPerson(nino, _, _, _, _, _, _, _, _) => this.nino == nino
    case _                                       => false
  }

  override def hashCode: Int = nino.hashCode
}

object EiLPerson {

  val defaultStringArgumentValue: String        = ""
  val defaultNino: String                       = defaultStringArgumentValue
  val defaultSecondName: Option[String]         = Some(defaultStringArgumentValue)
  val defaultWorksPayrollNumber: Option[String] = Some(defaultStringArgumentValue)
  val defaultDateOfBirth: Option[String]        = None
  val defaultGender: Option[String]             = Some(defaultStringArgumentValue)

  implicit val EiLPersonFormats: OFormat[EiLPerson] = Json.format[EiLPerson]
}
