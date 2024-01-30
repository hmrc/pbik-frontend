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

import models.v1.BenefitInKindWithCount
import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import utils.Exceptions.InvalidBikTypeException

case class Bik(iabdType: String, status: Int, eilCount: Int = 0) {
  override def equals(obj: Any): Boolean = obj match {
    case Bik(iabdType, _, _) => this.iabdType == iabdType
    case _                   => false
  }

  override def hashCode: Int = iabdType.hashCode

  def asBenefitString: String = Bik.asBenefitString(iabdType)
}

object Bik extends Logging {
  private val iabdValueMap: Map[String, String] = Map(
    ("40", "assets-transferred"),
    ("48", "payments-employee"),
    ("54", "vouchers-credit-cards"),
    ("44", "mileage"),
    ("31", "car"),
    ("29", "car-fuel"),
    ("35", "vans"),
    ("36", "van-fuel"),
    ("30", "medical"),
    ("50", "qualifying-relocation"),
    ("8", "services"),
    ("39", "assets-disposal"),
    ("47", "other"),
    ("52", "income-tax"),
    ("53", "travelling-subsistence"),
    ("42", "entertainment"),
    ("43", "business-travel"),
    ("32", "telephone"),
    ("45", "non-qualifying-relocation")
  )

  def apply(benefitInKindWithCount: BenefitInKindWithCount): Bik =
    new Bik(
      benefitInKindWithCount.iabdType.id.toString,
      benefitInKindWithCount.payrolledBenefitInKindStatus.id,
      benefitInKindWithCount.payrolledBenefitInKindExclusionCount
    )

  def asBenefitString(iabdType: String): String =
    iabdValueMap.getOrElse(iabdType, reportMissingBik(iabdType))

  def asNPSTypeValue(iabdString: String): String =
    iabdValueMap.map(_.swap).getOrElse(iabdString, reportMissingBik(iabdString))

  private def reportMissingBik(iabdValue: String): Nothing = {
    logger.info("[Bik][reportMissingBik] Invalid Bik cannot be remapped: " + iabdValue)
    throw new InvalidBikTypeException
  }

  implicit val bikFormats: OFormat[Bik] = Json.format[Bik]
}
