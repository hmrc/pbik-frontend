/*
 * Copyright 2021 HM Revenue & Customs
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

import java.util.Calendar

import javax.inject.{Inject, Singleton}
import models._
import org.joda.time.DateTimeConstants._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Request
import utils.FormMappingsConstants._

import scala.util.Try

object FormMappingsConstants {

  val START_OF_MONTH = 1
  val MONTH_28_DAYS = 28
  val MONTH_29_DAYS = 29
  val MONTH_30_DAYS = 30
  val MONTH_31_DAYS = 31
  val LEAP_YEAR_FREQ = 4

  // Note - scala range syntax needs extra one added
  val RANGE_28_DAYS = Range(START_OF_MONTH, MONTH_28_DAYS + 1)
  val RANGE_29_DAYS = Range(START_OF_MONTH, MONTH_29_DAYS + 1)
  val RANGE_30_DAYS = Range(START_OF_MONTH, MONTH_30_DAYS + 1)
  val RANGE_31_DAYS = Range(START_OF_MONTH, MONTH_31_DAYS + 1)

  val CY = "cy"
  val CYP1 = "cyp1"

}

@Singleton
class FormMappings @Inject()(val messagesApi: MessagesApi) extends PayrollBikDefaults with I18nSupport {

  private val nameValidationRegex = "([a-zA-Z-'\\sôéàëŵŷáîïâêûü])*"
  private val ninoValidationRegex = "([a-zA-Z])([a-zA-Z])[0-9][0-9][0-9][0-9][0-9][0-9]([a-zA-Z]?)"
  private val ninoTrimmedRegex = "([a-zA-Z])([a-zA-Z])[0-9][0-9][0-9][0-9][0-9][0-9]"

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
      val dayToInt: Int = dob._1.toInt
      val yearToInt: Int = dob._3.toInt
      monthToInt match {
        case month if JANUARY to DECEMBER contains month => {
          monthToInt match {
            case JANUARY => RANGE_31_DAYS.contains(dayToInt)
            case FEBRUARY =>
              if (yearToInt % LEAP_YEAR_FREQ == 0) {
                RANGE_29_DAYS.contains(dayToInt)
              } else {
                RANGE_28_DAYS.contains(dayToInt)
              }
            case MARCH     => RANGE_31_DAYS.contains(dayToInt)
            case APRIL     => RANGE_30_DAYS.contains(dayToInt)
            case MAY       => RANGE_31_DAYS.contains(dayToInt)
            case JUNE      => RANGE_30_DAYS.contains(dayToInt)
            case JULY      => RANGE_31_DAYS.contains(dayToInt)
            case AUGUST    => RANGE_31_DAYS.contains(dayToInt)
            case SEPTEMBER => RANGE_30_DAYS.contains(dayToInt)
            case OCTOBER   => RANGE_31_DAYS.contains(dayToInt)
            case NOVEMBER  => RANGE_30_DAYS.contains(dayToInt)
            case DECEMBER  => RANGE_31_DAYS.contains(dayToInt)
            case _         => throw new NumberFormatException()
          }
        }
        case _ => false
      }
    } catch {
      case ex: NumberFormatException => false
    }

  def isDateYearInFuture(dob: (String, String, String)): Boolean = {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val dobYear = Try(dob._3.toInt).getOrElse(0)

    if (dobYear < currentYear) true
    else false
  }

  def isDateYearInPastValid(dob: (String, String, String)): Boolean = {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val dobYear = Try(dob._3.toInt).getOrElse(0)

    if ((dobYear + 130) < currentYear) false
    else true
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
      "actives" -> list(
        mapping(
          "uid"     -> text,
          "active"  -> boolean,
          "enabled" -> boolean
        )(RegistrationItem.apply)(RegistrationItem.unapply)).verifying(
        "Error message goes here",
        selectionList => !selectionList.filter(listItem => listItem.active).isEmpty),
      "reason" -> optional(
        mapping(
          "selectionValue" -> text,
          "info"           -> optional(text)
        )(BinaryRadioButtonWithDesc.apply)(BinaryRadioButtonWithDesc.unapply))
    )(RegistrationList.apply)(RegistrationList.unapply)
  )

  def exclusionSearchFormWithNino[A](implicit request: Request[A]): Form[EiLPerson] = Form(
    mapping(
      "firstname" -> text
        .verifying(Messages("error.empty.firstname"), firstname => firstname.trim.length != 0)
        .verifying(Messages("error.incorrect.firstname"), firstname => firstname.matches(nameValidationRegex)),
      "surname" -> text
        .verifying(Messages("error.empty.lastname"), lastname => lastname.trim.length != 0)
        .verifying(Messages("error.incorrect.lastname"), lastname => lastname.matches(nameValidationRegex)),
      "nino" -> text
        .verifying(Messages("error.empty.nino"), nino => nino.trim.length != 0)
        .verifying(
          Messages("error.incorrect.nino"),
          nino => (nino.trim.length == 0 || nino.replaceAll(" ", "").matches(ninoValidationRegex))),
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
    ))((eilPerson: EiLPerson) =>
      Some((eilPerson.firstForename, eilPerson.surname, eilPerson.nino, eilPerson.status, eilPerson.perOptLock)))
  )

  def stripTrailingNinoCharacterForNPS(nino: String): String =
    ninoTrimmedRegex.r.findFirstIn(nino).getOrElse("").mkString

  def exclusionSearchFormWithoutNino[A](implicit request: Request[A]): Form[EiLPerson] = {
    val dateDayRegex: String = "([0-9])"
    val dateMonthRegex: String = "([0-9])"
    val dateYearRegex: String = "([0-9]){4}"
    val emptyDateError = "error.empty.dob"
    val invalidDateError = "error.invaliddate"
    val invalidDayDateError = "error.invaliddate.day"
    val invalidMonthDateError = "error.invaliddate.month"
    val invalidYearDateError = "error.invaliddate.year"
    val invalidYearFutureDateError = "error.invaliddate.future.year"
    val invalidYearPastDateError = "error.invaliddate.past.year"

    Form(
      mapping(
        "firstname" -> text
          .verifying(Messages("error.empty.firstname"), firstname => firstname.trim.length != 0)
          .verifying(Messages("error.incorrect.firstname"), firstname => firstname.matches(nameValidationRegex)),
        "surname" -> text
          .verifying(Messages("error.empty.lastname"), lastname => lastname.trim.length != 0)
          .verifying(Messages("error.incorrect.lastname"), lastname => lastname.matches(nameValidationRegex)),
        "dob" -> mapping(
          "day"   -> text,
          "month" -> text,
          "year"  -> text
        )((day, month, year) => (day, month, year))((dob: (String, String, String)) => Some((dob._1, dob._2, dob._3)))
          .verifying(emptyDateError, dob => !(dob._1.isEmpty && dob._2.isEmpty && dob._3.isEmpty))
          .verifying(invalidYearFutureDateError, dob => isDateYearInFuture(dob))
          .verifying(invalidYearPastDateError, dob => isDateYearInPastValid(dob))
          .verifying(invalidDayDateError, dob => !(addZeroIfNeeded(dob._1).matches(dateDayRegex)))
          .verifying(invalidMonthDateError, dob => !(addZeroIfNeeded(dob._2).matches(dateMonthRegex)))
          .verifying(invalidYearDateError, dob => (dob._3.matches(dateYearRegex)))
          .verifying(invalidDateError, dob => isValidDate(dob)),
        "gender"     -> text.verifying("Error message goes here", gender => !gender.isEmpty),
        "status"     -> optional(number),
        "perOptLock" -> default(number, 0)
      )((firstname, surname, dob, gender, status, perOptLock) =>
        EiLPerson(
          EiLPerson.defaultNino,
          firstname.trim,
          EiLPerson.defaultSecondName,
          surname.trim,
          EiLPerson.defaultWorksPayrollNumber,
          Some(addZeroIfNeeded(dob._1) + "/" +
            addZeroIfNeeded(dob._2) + "/" + dob._3),
          Some(gender),
          status,
          perOptLock
      ))(
        (eilPerson: EiLPerson) =>
          Some(
            (
              eilPerson.firstForename,
              eilPerson.surname,
              (
                eilPerson.dateOfBirth.get.split('/')(0),
                eilPerson.dateOfBirth.get.split('/')(1),
                eilPerson.dateOfBirth.get.split('/')(2)),
              eilPerson.gender.get,
              eilPerson.status,
              eilPerson.perOptLock)))
    )
  }

  val individualsForm: Form[EiLPersonList] = Form(
    mapping(
      "individuals" -> list(mapping(
        "nino"               -> text,
        "firstName"          -> text,
        "secondName"         -> optional(text),
        "surname"            -> text,
        "worksPayrollNumber" -> optional(text),
        "dateOfBirth"        -> optional(text),
        "gender"             -> optional(text),
        "status"             -> optional(number),
        "perOptLock"         -> default(number, 0)
      )(EiLPerson.apply)(EiLPerson.unapply)))(EiLPersonList.apply)(EiLPersonList.unapply)
  )

  val individualsFormWithRadio: Form[(String, EiLPersonList)] = Form(
    mapping(
      "individualSelection" -> text,
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
        )(EiLPerson.apply)(EiLPerson.unapply))
    )((individualSelection, individuals) => ((individualSelection, EiLPersonList(individuals))))(
      (individualsTuple: (String, EiLPersonList)) => Some((individualsTuple._1, individualsTuple._2.active)))
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
        "selectionValue" -> text.verifying(noReasonError, selectionValue => selectionValue.trim.length != 0),
        "info"           -> optional(text)
      )(BinaryRadioButtonWithDesc.apply)(BinaryRadioButtonWithDesc.unapply))

  }

}
