/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.Configuration
import uk.gov.hmrc.time.TaxYear

import java.text.SimpleDateFormat
import java.time.{LocalDate, LocalDateTime}
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

@Singleton
class TaxDateUtils @Inject() (configuration: Configuration) {

  private val overriddenDateFromConfig: List[Integer] = Try {
    configuration.underlying.getIntList("pbik.date.override")
  } match {
    case Success(value) => value.asScala.toList
    case Failure(_)     => List.empty
  }

  private val sdf                   = new SimpleDateFormat("dd-M-yyyy hh:mm:ss")
  private val startDateBanner: Date =
    sdf.parse(configuration.getOptional[String]("pbik.banner.date.start").getOrElse(""))
  private val endDateBanner: Date   = sdf.parse(configuration.getOptional[String]("pbik.banner.date.end").getOrElse(""))

  def getDefaultDate: LocalDate =
    if (overriddenDateFromConfig.nonEmpty) {
      LocalDate
        .of(overriddenDateFromConfig.head, overriddenDateFromConfig(1), overriddenDateFromConfig(2))
    } else { LocalDate.now() }

  def getDefaultYear: Int = {
    val year = LocalDateTime.now().getYear
    if (overriddenDateFromConfig.nonEmpty) {
      year + 1
    } else {
      year
    }
  }

  def getTaxYearRange(year: Int = getCurrentTaxYear(getDefaultDate)): TaxYearRange = generateTaxYearRange(year)

  def getCurrentTaxYear(dateToCheck: LocalDate = getDefaultDate): Int =
    TaxYear.taxYearFor(dateToCheck).currentYear

  def isCurrentTaxYear(yearToCheck: Int = getDefaultYear, dateToCheck: LocalDate = getDefaultDate): Boolean =
    yearToCheck == TaxYear.taxYearFor(dateToCheck).currentYear

  private def generateTaxYearRange(year: Int): TaxYearRange =
    TaxYearRange(year, year + 1, year + 2)

  def dateWithinAnnualCodingRun(today: Date): Boolean =
    today.getTime >= startDateBanner.getTime && today.getTime <= endDateBanner.getTime

}
