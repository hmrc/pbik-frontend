/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json._

case class BiKsWithExclusions(iabdType: String, status: Int, numberOfExclusions: Int)

case class RegistrationItem(id: String, active: Boolean, enabled: Boolean)

case class RegistrationList(selectAll: Option[String] = None, active: List[RegistrationItem], reason: Option[BinaryRadioButtonWithDesc] = None)

case class EiLPersonList(active: List[EiLPerson])

case class BinaryRadioButton(selectionValue: Option[String])
case class BinaryRadioButtonWithDesc(selectionValue: String, info: Option[String])

case class TaxYearRange(cyminus1: Int, cy: Int, cyplus1: Int)

case class PbikCredentials(payeSchemeType: Int, employerNumber: Int, payeSequenceNumber: Int, aoReference: String, payeSchemeOperatorName: String)

case class Bik(iabdType: String, status: Int, eilCount: Int = 0) {
  override def equals(obj: Any):Boolean = obj match {
    case Bik(iabdType,_,_) => this.iabdType == iabdType
    case _                => false
  }

  override def hashCode:Int = iabdType.hashCode
}


case class EiLPerson(nino: String, firstForename: String, secondForename: Option[String], surname: String, worksPayrollNumber: Option[String],
                     dateOfBirth: Option[String], gender: Option[String], status: Option[Int], perOptLock: Int = 0) {

  override def equals(obj: Any):Boolean = obj match {
    case EiLPerson(nino,_,_,_,_,_,_,_,_) => this.nino == nino
    case _                               => false
  }

  override def hashCode:Int = nino.hashCode
}

case class PbikError(errorCode: String)
object PbikError {
  implicit val pbikErrorFormat = Json.format[PbikError]
}

object EiLPerson {

  val defaultStringArgumentValue = ""
  val defaultIntArgumentValue = -1
  val defaultNino = defaultStringArgumentValue
  val defaultFirstName = defaultStringArgumentValue
  val defaultSecondName = Some(defaultStringArgumentValue)
  val defaultSurname = defaultStringArgumentValue
  val defaultWorksPayrollNumber = Some(defaultStringArgumentValue)
  val defaultDateOfBirth = None
  val defaultGender = Some(defaultStringArgumentValue)
  val defaultStatus = Some(defaultIntArgumentValue)
  val defaultPerOptLock = defaultIntArgumentValue

  def secondaryComparison(x: EiLPerson, y: EiLPerson): Boolean = {
    x.firstForename == y.firstForename &&
      x.surname == y.surname &&
      x.dateOfBirth.getOrElse("") == y.dateOfBirth.getOrElse("") &&
      x.gender.getOrElse("") == y.gender.getOrElse("")
  }

  def defaultEiLPerson(): EiLPerson = {
    EiLPerson(defaultNino, defaultFirstName, defaultSecondName, defaultSurname, defaultWorksPayrollNumber, defaultDateOfBirth, defaultGender, defaultStatus, defaultPerOptLock )
  }
}

case class Person(nino: String, worksPayrollNumber: String, firstForename: String, surname: String, dateOfBirth: String)

/**
 * The Header Tags are used between the PBIK gateway and NPS to control the optimistic locks.
 * Each time a call is made to NPS on an employer specific URL, NPS returns the current value of the optimistic lock for the employer record
 * We need to save that value and send it back in each time we wish to change the employer record ( i.e by updating the registered benefits or
 * excluding an individual ). If the Etag value does not match, NPS will reject the update as it indicates other changes have been made to the
 * employer record thereby invalidating ours ).
 */
object HeaderTags {
  val ETAG: String = "ETag"
  val X_TXID: String = "X-TXID"
}
