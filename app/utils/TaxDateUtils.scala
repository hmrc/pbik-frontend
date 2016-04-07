/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.{Play, Logger}
import uk.gov.hmrc.time.TaxYearResolver
import play.api.Play.current

object TaxDateUtils extends PayrollBikDefaults {

  val overridedDateFromConfig = Play.configuration.getIntList("pbik.date.override")

  def getDefaultDate = {
    if(overridedDateFromConfig.isDefined) {
      new LocalDate(overridedDateFromConfig.get.get(0), overridedDateFromConfig.get.get(1), overridedDateFromConfig.get.get(2))} else new LocalDate()
  }

  def getDefaultYear = {
    if(overridedDateFromConfig.isDefined) new DateTime().getYear + 1 else new DateTime().getYear
  }

  def getTaxYearRange(year:Int = getCurrentTaxYear(getDefaultDate)):TaxYearRange = generateTaxYearRange(year)

  def getCurrentTaxYear(dateToCheck:LocalDate = getDefaultDate):Int = {
      TaxYearResolver.taxYearFor(dateToCheck)
  }

  def isCurrentTaxYear(yearToCheck:Int = getDefaultYear, dateToCheck:LocalDate = getDefaultDate):Boolean = {
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
