/*
 * Copyright 2025 HM Revenue & Customs
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

import models.TaxYearRange
import play.api.i18n.Messages
import uk.gov.hmrc.time.TaxYear
import utils.Exceptions.InvalidYearURIException

import java.time.LocalDate
import javax.inject.Singleton

@Singleton
class TaxDateUtils {

  private def getDefaultDate: LocalDate = LocalDate.now()

  private def generateTaxYearRange(year: Int): TaxYearRange =
    TaxYearRange(year, year + 1, year + 2)

  def getTaxYearRange(year: Int = getCurrentTaxYear(getDefaultDate)): TaxYearRange = generateTaxYearRange(year)

  def getCurrentTaxYear(dateToCheck: LocalDate = getDefaultDate): Int =
    TaxYear.taxYearFor(dateToCheck).currentYear

  def isCurrentTaxYear(yearToCheck: Int): Boolean = yearToCheck == getCurrentTaxYear(getDefaultDate)

  def mapYearStringToInt(yearUri: String): Int = {
    val range = getTaxYearRange()
    yearUri match {
      case utils.FormMappingsConstants.CY   => range.cyminus1
      case utils.FormMappingsConstants.CYP1 => range.cy
      case _                                => throw new InvalidYearURIException()
    }
  }

  def getDisplayTodayDate(today: LocalDate = getDefaultDate)(implicit messages: Messages): String =
    s"${today.getDayOfMonth} ${messages("Service.month." + today.getMonth.getValue)} ${today.getYear}"

  def getDisplayStartOfCYP1()(implicit messages: Messages): String =
    s"06 ${messages("Service.month.04")} ${getTaxYearRange().cy}"

}
