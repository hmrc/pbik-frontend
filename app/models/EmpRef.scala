/*
 * Copyright 2022 HM Revenue & Customs
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

case class EmpRef(taxOfficeNumber: String, taxOfficeReference: String) {
  def encodedEmpRef: String = s"$taxOfficeNumber%2F$taxOfficeReference"
  def unencodedEmpRef: String = s"$taxOfficeNumber/$taxOfficeReference"

  override def toString: String = unencodedEmpRef.toString

  def getOrElse(default: String): String = (taxOfficeNumber, taxOfficeReference) match {
    case ("", "") => default
    case _        => toString
  }
}

object EmpRef {
  val empty: EmpRef = EmpRef("", "")
}
