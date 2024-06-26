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

package utils

import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Request

import java.time.LocalDate
import java.util.Calendar
import javax.inject.{Inject, Singleton}
import scala.util.Try

object FormMappingsConstants {

  val CY   = "cy"
  val CYP1 = "cyp1"

}

@Singleton
class FormMappings @Inject() (val messagesApi: MessagesApi) extends I18nSupport {

  private val nameValidationRegex        = "([a-zA-Z-'\\sôéàëŵŷáîïâêûü])*"
  private val ninoValidationRegex        = "([a-zA-Z])([a-zA-Z])[0-9][0-9][0-9][0-9][0-9][0-9]([a-zA-Z]?)"
  private val ninoTrimmedRegex           = "([a-zA-Z])([a-zA-Z])[0-9][0-9][0-9][0-9][0-9][0-9]"
  private val dateDayRegex               = "([0-9])"
  private val dateMonthRegex             = "([0-9])"
  private val dateYearRegex              = "([0-9]){4}"
  private val emptyDateError             = "error.empty.dob"
  private val invalidDateError           = "error.invaliddate"
  private val invalidDayDateError        = "error.invaliddate.day"
  private val invalidMonthDateError      = "error.invaliddate.month"
  private val invalidYearDateError       = "error.invaliddate.year"
  private val invalidYearFutureDateError = "error.invaliddate.future.year"
  private val invalidYearPastDateError   = "error.invaliddate.past.year"
  private val maxLength                  = 100

  def generateYearString(length: Int): String =
    if (length > 0) {
      "[0-9]" + generateYearString(length - 1)
    } else {
      ""
    }

  def addZeroIfNeeded(date: String): String =
    if (date.length == 1) {
      "0" + date
    } else {
      date
    }

  def isValidDate(dob: (String, String, String)): Boolean =
    try {
      val monthToInt: Int = dob._2.toInt
      val dayToInt: Int   = dob._1.toInt
      val yearToInt: Int  = dob._3.toInt
      LocalDate.of(yearToInt, monthToInt, dayToInt)
      true
    } catch {
      case _: Exception => false
    }

  private def isDateYearInFuture(dob: (String, String, String)): Boolean = {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val dobYear     = Try(dob._3.toInt).getOrElse(0)

    dobYear < currentYear
  }

  private def isDateYearInPastValid(dob: (String, String, String)): Boolean = {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val dobYear     = Try(dob._3.toInt).getOrElse(0)

    !((dobYear + 130) < currentYear)
  }

  val binaryRadioButton: Form[MandatoryRadioButton] = Form(
    mapping(
      "confirmation" -> nonEmptyText(1) //  must contain minimum characters for supported biks 1-99
    )(MandatoryRadioButton.apply)(MandatoryRadioButton.unapply)
  )

  val navigationRadioButton: Form[MandatoryRadioButton] = Form(
    mapping(
      "navigation" -> nonEmptyText(2) //  must contain minimum characters for cy or cyp1
    )(MandatoryRadioButton.apply)(MandatoryRadioButton.unapply)
  )

  val objSelectedForm: Form[RegistrationList] = Form(
    mapping(
      "select-all" -> optional(text),
      "actives"    -> list(
        mapping(
          "uid"     -> text,
          "active"  -> boolean,
          "enabled" -> boolean
        )(RegistrationItem.apply)(RegistrationItem.unapply)
      )
        .verifying("Error message goes here", selectionList => selectionList.exists(listItem => listItem.active)),
      "reason"     -> optional(
        mapping(
          "selectionValue" -> text,
          "info"           -> optional(text)
        )(BinaryRadioButtonWithDesc.apply)(BinaryRadioButtonWithDesc.unapply)
      )
    )(RegistrationList.apply)(RegistrationList.unapply)
  )

  def exclusionSearchFormWithNino[A](implicit request: Request[A]): Form[EiLPerson] = Form(
    mapping(
      "firstname"  -> text
        .verifying(Messages("error.empty.firstname"), firstname => firstname.trim.nonEmpty)
        .verifying(Messages("error.incorrect.firstname"), firstname => firstname.matches(nameValidationRegex)),
      "surname"    -> text
        .verifying(Messages("error.empty.lastname"), lastname => lastname.trim.nonEmpty)
        .verifying(Messages("error.incorrect.lastname"), lastname => lastname.matches(nameValidationRegex)),
      "nino"       -> text
        .verifying(Messages("error.empty.nino"), nino => nino.trim.nonEmpty)
        .verifying(
          Messages("error.incorrect.nino"),
          nino => nino.trim.isEmpty || nino.replaceAll(" ", "").matches(ninoValidationRegex)
        ),
      "status"     -> optional(number),
      "perOptLock" -> default(number, 0)
    )((firstname, surname, nino, status, perOptLock) =>
      EiLPerson(
        stripTrailingNinoCharacterForNPS(nino.replaceAll(" ", "").toUpperCase),
        firstname.trim,
        EiLPerson.defaultSecondName,
        surname.trim,
        EiLPerson.defaultWorksPayrollNumber,
        EiLPerson.defaultDateOfBirth,
        EiLPerson.defaultGender,
        status,
        perOptLock
      )
    )((eilPerson: EiLPerson) =>
      Some((eilPerson.firstForename, eilPerson.surname, eilPerson.nino, eilPerson.status, eilPerson.perOptLock))
    )
  )

  private def stripTrailingNinoCharacterForNPS(nino: String): String =
    ninoTrimmedRegex.r.findFirstIn(nino).getOrElse("").mkString

  def exclusionSearchFormWithoutNino[A](implicit request: Request[A]): Form[EiLPerson] =
    Form(
      mapping(
        "firstname"  -> text
          .verifying(Messages("error.empty.firstname"), firstname => firstname.trim.nonEmpty)
          .verifying(Messages("error.incorrect.firstname"), firstname => firstname.matches(nameValidationRegex)),
        "surname"    -> text
          .verifying(Messages("error.empty.lastname"), lastname => lastname.trim.nonEmpty)
          .verifying(Messages("error.incorrect.lastname"), lastname => lastname.matches(nameValidationRegex)),
        "dob"        -> mapping(
          "day"   -> text,
          "month" -> text,
          "year"  -> text
        )((day, month, year) => (day, month, year))((dob: (String, String, String)) => Some((dob._1, dob._2, dob._3)))
          .verifying(emptyDateError, dob => !(dob._1.isEmpty && dob._2.isEmpty && dob._3.isEmpty))
          .verifying(invalidYearFutureDateError, dob => isDateYearInFuture(dob))
          .verifying(invalidYearPastDateError, dob => isDateYearInPastValid(dob))
          .verifying(invalidDayDateError, dob => !addZeroIfNeeded(dob._1).matches(dateDayRegex))
          .verifying(invalidMonthDateError, dob => !addZeroIfNeeded(dob._2).matches(dateMonthRegex))
          .verifying(invalidYearDateError, dob => dob._3.matches(dateYearRegex))
          .verifying(invalidDateError, dob => isValidDate(dob)),
        "gender"     -> text.verifying("Error message goes here", gender => gender.nonEmpty),
        "status"     -> optional(number),
        "perOptLock" -> default(number, 0)
      )((firstname, surname, dob, gender, status, perOptLock) =>
        EiLPerson(
          EiLPerson.defaultNino,
          firstname.trim,
          EiLPerson.defaultSecondName,
          surname.trim,
          EiLPerson.defaultWorksPayrollNumber,
          Some(addZeroIfNeeded(dob._1) + "/" + addZeroIfNeeded(dob._2) + "/" + dob._3),
          Some(gender),
          status,
          perOptLock
        )
      )((eilPerson: EiLPerson) =>
        Some(
          (
            eilPerson.firstForename,
            eilPerson.surname,
            (
              eilPerson.dateOfBirth.get.split('/')(0),
              eilPerson.dateOfBirth.get.split('/')(1),
              eilPerson.dateOfBirth.get.split('/')(2)
            ),
            eilPerson.gender.get,
            eilPerson.status,
            eilPerson.perOptLock
          )
        )
      )
    )

  val individualsForm: Form[EiLPersonList] = Form(
    mapping(
      "individuals" -> list(
        mapping(
          "nino"               -> text,
          "firstName"          -> text,
          "secondName"         -> optional(text),
          "surname"            -> text,
          "worksPayrollNumber" -> optional(text),
          "dateOfBirth"        -> optional(text),
          "gender"             -> optional(text),
          "status"             -> optional(number),
          "perOptLock"         -> default(number, 0)
        )(EiLPerson.apply)(EiLPerson.unapply)
      )
    )(EiLPersonList.apply)(EiLPersonList.unapply)
  )

  val individualsFormWithRadio: Form[(String, EiLPersonList)] = Form(
    mapping(
      "individualSelection" -> text,
      "individuals"         -> list(
        mapping(
          "nino"               -> text,
          "firstName"          -> text,
          "secondName"         -> optional(text),
          "surname"            -> text,
          "worksPayrollNumber" -> optional(text),
          "dateOfBirth"        -> optional(text),
          "gender"             -> optional(text),
          "status"             -> optional(number),
          "perOptLock"         -> default(number, 0)
        )(EiLPerson.apply)(EiLPerson.unapply)
      )
    )((individualSelection, individuals) => (individualSelection, EiLPersonList(individuals)))(
      (individualsTuple: (String, EiLPersonList)) => Some((individualsTuple._1, individualsTuple._2.active))
    )
  )

  val individualSelectionForm: Form[ExclusionNino] = Form(
    mapping(
      "individualNino" -> nonEmptyText
    )(ExclusionNino.apply)(ExclusionNino.unapply)
  )

  val removalReasonForm: Form[BinaryRadioButtonWithDesc] = {
    val noReasonError = "RemoveBenefits.reason.no.selection"
    Form(
      mapping(
        "selectionValue" -> text.verifying(noReasonError, selectionValue => selectionValue.trim.nonEmpty),
        "info"           -> optional(text)
      )(BinaryRadioButtonWithDesc.apply)(BinaryRadioButtonWithDesc.unapply)
    )

  }

  val removalOtherReasonForm: Form[OtherReason] =
    Form(
      mapping(
        "otherReason" -> text
          .verifying("RemoveBenefits.other.error.required", _.trim.nonEmpty)
          .verifying("RemoveBenefits.other.error.length", _.length <= maxLength)
      )(OtherReason.apply)(OtherReason.unapply)
    )

  val selectYearForm: Form[SelectYear] =
    Form(mapping("year" -> nonEmptyText)(SelectYear.apply)(SelectYear.unapply))

}
