/*
 * Copyright 2015 HM Revenue & Customs
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
import org.joda.time.{LocalDate, DateTime}
import play.api.Logger
import uk.gov.hmrc.time.TaxYearResolver

object TaxDateUtils extends PayrollBikDefaults {

  //lazy val TAX_YEAR_RANGE = generateTaxYearRange(getCurrentTaxYear())

  def getTaxYearRange(year:Int = getCurrentTaxYear()):TaxYearRange = generateTaxYearRange(year)

  def getCurrentTaxYear(dateToCheck:LocalDate = new LocalDate):Int = {
      TaxYearResolver.taxYearFor(dateToCheck)
  }

  def isCurrentTaxYear(yearToCheck:Int = new DateTime().getYear, dateToCheck:LocalDate = new LocalDate):Boolean = {
    yearToCheck == TaxYearResolver.taxYearFor(dateToCheck)
  }


  def isServiceLaunched(year:Int = getCurrentTaxYear()):Boolean = {
      val launched = (year >= TAX_YEAR_OF_LAUNCH)
      launched
  }

  private def generateTaxYearRange(year:Int):TaxYearRange = {
      TaxYearRange(year, year + 1, year + 2)
  }


}
