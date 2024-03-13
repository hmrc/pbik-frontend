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

import models.TaxYearRange
import uk.gov.hmrc.time.TaxYear

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

  def isCurrentTaxYear(yearToCheck: Int): Boolean = yearToCheck == TaxYear(getDefaultDate.getYear).currentYear

}
