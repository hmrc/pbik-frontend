/*
 * Copyright 2022 HM Revenue & Customs
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
import org.joda.time.DateTime
import play.api.Configuration
import uk.gov.hmrc.time.TaxYear

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

@Singleton
class TaxDateUtils @Inject() (configuration: Configuration) extends PayrollBikDefaults {

  val overriddenDateFromConfig: List[Integer] = Try {
    configuration.underlying.getIntList("pbik.date.override")
  } match {
    case Success(value) => value.asScala.toList
    case Failure(_)     => List.empty
  }

  val sdf                   = new SimpleDateFormat("dd-M-yyyy hh:mm:ss")
  val startDateBanner: Date = sdf.parse(configuration.getOptional[String]("pbik.banner.date.start").getOrElse(""))
  val endDateBanner: Date   = sdf.parse(configuration.getOptional[String]("pbik.banner.date.end").getOrElse(""))

  def getDefaultDate: LocalDate =
    if (overriddenDateFromConfig.nonEmpty) {
      LocalDate
        .of(overriddenDateFromConfig.head, overriddenDateFromConfig(1), overriddenDateFromConfig(2))
    } else { LocalDate.now() }

  def getDefaultYear: Int =
    if (overriddenDateFromConfig.nonEmpty) new DateTime().getYear + 1 else new DateTime().getYear

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
