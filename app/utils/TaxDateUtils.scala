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

import java.text.SimpleDateFormat
import java.util
import java.util.Date
import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import models.TaxYearRange
import org.joda.time.DateTime
import play.api.Configuration
import uk.gov.hmrc.time.TaxYear

@Singleton
class TaxDateUtils @Inject()(configuration: Configuration) extends PayrollBikDefaults {

  val overridedDateFromConfig: Option[util.List[Integer]] = configuration.getIntList("pbik.date.override")

  val sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss")
  val startDateBanner: Date = sdf.parse(configuration.getString("pbik.banner.date.start").getOrElse(""))
  val endDateBanner: Date = sdf.parse(configuration.getString("pbik.banner.date.end").getOrElse(""))

  def getDefaultDate: LocalDate =
    if (overridedDateFromConfig.isDefined) {
      LocalDate
        .of(overridedDateFromConfig.get.get(0), overridedDateFromConfig.get.get(1), overridedDateFromConfig.get.get(2))
    } else LocalDate.now()

  def getDefaultYear: Int =
    if (overridedDateFromConfig.isDefined) new DateTime().getYear + 1 else new DateTime().getYear

  def getTaxYearRange(year: Int = getCurrentTaxYear(getDefaultDate)): TaxYearRange = generateTaxYearRange(year)

  def getCurrentTaxYear(dateToCheck: LocalDate = getDefaultDate): Int =
    TaxYear.taxYearFor(dateToCheck).currentYear

  def isCurrentTaxYear(yearToCheck: Int = getDefaultYear, dateToCheck: LocalDate = getDefaultDate): Boolean =
    yearToCheck == TaxYear.taxYearFor(dateToCheck).currentYear

  def isServiceLaunched(year: Int = getCurrentTaxYear()): Boolean = {
    val launched = year >= TAX_YEAR_OF_LAUNCH
    launched
  }

  private def generateTaxYearRange(year: Int): TaxYearRange =
    TaxYearRange(year, year + 1, year + 2)

  def dateWithinAnnualCodingRun(today: Date): Boolean =
    today.getTime >= startDateBanner.getTime && today.getTime <= endDateBanner.getTime

}
