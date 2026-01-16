/*
 * Copyright 2026 HM Revenue & Customs
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
import models.form._
import models.v1.IabdType
import models.v1.IabdType.IabdType
import models.v1.exclusion.Gender
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
  private val validNinoFormat            = "[[A-Z]&&[^DFIQUV]][[A-Z]&&[^DFIQUVO]] ?\\d{2} ?\\d{2} ?\\d{2} ?[A-D]?"
  private val dateDayRegex               = "^([1-9]|0[1-9]|[12][0-9]|3[01])$"
  private val dateMonthRegex             = "^([1-9]|0[1-9]|1[0-2])$"
  private val dateYearRegex              = "^[0-9]{4}$"
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
    if (date.matches("""\d{1}""")) s"0$date"
    else date

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

  private def isDateInFuture(dob: (String, String, String)): Boolean = {
    val dobDate     = Try {
      val day   = dob._1.toInt
      val month = dob._2.toInt
      val year  = dob._3.toInt
      LocalDate.of(year, month, day)
    }.getOrElse(LocalDate.MIN)
    val currentDate = LocalDate.now()
    dobDate.isBefore(currentDate)
  }

  private def isDateYearInPastValid(dob: (String, String, String)): Boolean = {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val dobYear     = Try(dob._3.toInt).getOrElse(0)

    !((dobYear + 130) < currentYear)
  }

  private def cleanupNino(nino: String): String =
    nino.replaceAll(" ", "").trim.toUpperCase

  val binaryRadioButton: Form[MandatoryRadioButton] = Form(
    mapping(
      "confirmation" -> nonEmptyText(1) //  must contain minimum characters for supported biks 1-99
    )(MandatoryRadioButton.apply)(o => Some(o.selectionValue))
  )

  val navigationRadioButton: Form[MandatoryRadioButton] = Form(
    mapping(
      "navigation" -> nonEmptyText(2) //  must contain minimum characters for cy or cyp1
    )(MandatoryRadioButton.apply)(o => Some(o.selectionValue))
  )

  val objSelectedForm: Form[RegistrationList] = Form(
    mapping(
      "select-all" -> optional(text),
      "actives"    -> list(
        mapping(
          "uid"     -> text.transform[IabdType](bik => IabdType(bik.trim.toInt), iabdType => iabdType.id.toString),
          "active"  -> boolean,
          "enabled" -> boolean
        )(RegistrationItem.apply)(o => Some(Tuple.fromProductTyped(o)))
      )
        .verifying("Error message goes here", selectionList => selectionList.exists(listItem => listItem.active)),
      "reason"     -> optional(
        mapping(
          "selectionValue" -> text,
          "info"           -> optional(text)
        )(BinaryRadioButtonWithDesc.apply)(o => Some(Tuple.fromProductTyped(o)))
      )
    )(RegistrationList.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

  def exclusionSearchFormWithNino[A](implicit request: Request[A]): Form[NinoForm] = Form(
    mapping(
      "firstname" -> text
        .verifying(Messages("error.empty.firstname"), firstname => firstname.trim.nonEmpty)
        .verifying(Messages("error.incorrect.firstname"), firstname => firstname.matches(nameValidationRegex)),
      "surname"   -> text
        .verifying(Messages("error.empty.lastname"), lastname => lastname.trim.nonEmpty)
        .verifying(Messages("error.incorrect.lastname"), lastname => lastname.matches(nameValidationRegex)),
      "nino"      -> text
        .verifying(Messages("error.empty.nino"), nino => cleanupNino(nino).nonEmpty)
        .verifying(
          Messages("error.incorrect.nino"),
          nino => cleanupNino(nino).matches(validNinoFormat)
        )
    )((firstname, surname, nino) =>
      NinoForm(
        firstname.trim,
        surname.trim,
        cleanupNino(nino)
      )
    )((ninoForm: NinoForm) => Some((ninoForm.firstName, ninoForm.surname, ninoForm.nino)))
  )

  def exclusionSearchFormWithoutNino[A](implicit request: Request[A]): Form[NoNinoForm] =
    Form(
      mapping(
        "firstname" -> text
          .verifying(Messages("error.empty.firstname"), firstname => firstname.trim.nonEmpty)
          .verifying(Messages("error.incorrect.firstname"), firstname => firstname.matches(nameValidationRegex)),
        "surname"   -> text
          .verifying(Messages("error.empty.lastname"), lastname => lastname.trim.nonEmpty)
          .verifying(Messages("error.incorrect.lastname"), lastname => lastname.matches(nameValidationRegex)),
        "dob"       -> mapping(
          "day"   -> text,
          "month" -> text,
          "year"  -> text
        )((day, month, year) => (day, month, year))((dob: (String, String, String)) => Some((dob._1, dob._2, dob._3)))
          .verifying(emptyDateError, dob => !(dob._1.isEmpty && dob._2.isEmpty && dob._3.isEmpty))
          .verifying(invalidDayDateError, dob => dob._1.matches(dateDayRegex))
          .verifying(invalidMonthDateError, dob => dob._2.matches(dateMonthRegex))
          .verifying(invalidYearDateError, dob => dob._3.matches(dateYearRegex))
          .verifying(invalidYearFutureDateError, dob => isDateInFuture(dob))
          .verifying(invalidYearPastDateError, dob => isDateYearInPastValid(dob))
          .verifying(invalidDateError, dob => isValidDate(dob)),
        "gender"    -> text.verifying("error.required", gender => Try(Gender.fromString(gender)).isSuccess)
      )((firstname, surname, dob, gender) =>
        NoNinoForm(
          firstname.trim,
          surname.trim,
          DateOfBirth(addZeroIfNeeded(dob._1), addZeroIfNeeded(dob._2), dob._3),
          Gender.fromString(gender)
        )
      )((noNinoForm: NoNinoForm) =>
        Some(
          (
            noNinoForm.firstName,
            noNinoForm.surname,
            (
              noNinoForm.dateOfBirth.day,
              noNinoForm.dateOfBirth.month,
              noNinoForm.dateOfBirth.year
            ),
            noNinoForm.gender.toString
          )
        )
      )
    )

  val individualSelectionForm: Form[ExclusionNino] = Form(
    mapping(
      "individualNino" -> nonEmptyText
    )(ExclusionNino.apply)(o => Some(o.nino))
  )

  val removalReasonForm: Form[BinaryRadioButtonWithDesc] = {
    val noReasonError = "RemoveBenefits.reason.no.selection"
    Form(
      mapping(
        "selectionValue" -> text.verifying(noReasonError, selectionValue => selectionValue.trim.nonEmpty),
        "info"           -> optional(text)
      )(BinaryRadioButtonWithDesc.apply)(o => Some(Tuple.fromProductTyped(o)))
    )

  }

  val removalOtherReasonForm: Form[OtherReason] =
    Form(
      mapping(
        "otherReason" -> text
          .verifying("RemoveBenefits.other.error.required", _.trim.nonEmpty)
          .verifying("RemoveBenefits.other.error.length", _.length <= maxLength)
      )(OtherReason.apply)(o => Some(o.reason))
    )

  val selectYearForm: Form[SelectYear] =
    Form(mapping("year" -> nonEmptyText)(SelectYear.apply)(o => Some(o.year)))

}
