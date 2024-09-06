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

import models.EiLPerson
import models.v1.IabdType.IabdType
import models.v1.exclusion.Gender.Gender
import models.v1.exclusion.PersonalEmploymentStatus.PersonalEmploymentStatus
import play.api.libs.json.{Json, OFormat}

case class PbikExclusionPersonWithBenefitAndStatus(
  iabdType: IabdType,
  identifier: String,
  firstForename: String,
  secondForename: Option[String],
  surname: String,
  worksPayrollNumber: String,
  dateOfBirth: String,
  sex: Gender,
  personalEmploymentStatus: PersonalEmploymentStatus
) {

  //TODO delete or migrate to another object
  def toExclusionPerson: PbikExclusionPerson =
    PbikExclusionPerson(
      identifier,
      firstForename,
      secondForename,
      surname,
      worksPayrollNumber,
      "",
      "",
      0,
      0,
      0
    )
}

object PbikExclusionPersonWithBenefitAndStatus {
  implicit val formats: OFormat[PbikExclusionPersonWithBenefitAndStatus] =
    Json.format[PbikExclusionPersonWithBenefitAndStatus]

  def apply(iabdType: IabdType, eiLPerson: EiLPerson): PbikExclusionPersonWithBenefitAndStatus =
    PbikExclusionPersonWithBenefitAndStatus(
      iabdType,
      eiLPerson.nino,
      eiLPerson.firstForename,
      eiLPerson.secondForename,
      eiLPerson.surname,
      eiLPerson.worksPayrollNumber.getOrElse(""),
      eiLPerson.dateOfBirth.getOrElse(""),
      Gender.fromString(eiLPerson.gender),
      PersonalEmploymentStatus.fromInt(eiLPerson.status)
    )

  //TODO migrate or delete
  def apply(
    iabdType: IabdType,
    exclusion: PbikExclusionPerson,
    withStatus: PersonalEmploymentStatus
  ): PbikExclusionPersonWithBenefitAndStatus =
    PbikExclusionPersonWithBenefitAndStatus(
      iabdType,
      exclusion.nationalInsuranceNumber,
      exclusion.firstForename,
      exclusion.secondForename,
      exclusion.surname,
      exclusion.worksPayrollNumber,
      "",
      Gender.Unknown,
      withStatus
    )
}
