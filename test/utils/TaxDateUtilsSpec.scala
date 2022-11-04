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

import controllers.FakePBIKApplication
import org.joda.time.DateTimeConstants._
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.LocalDate

class TaxDateUtilsSpec extends AnyWordSpecLike with Matchers with OptionValues with FakePBIKApplication {

  val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]
  val year2015                   = 2015
  val year2014                   = 2014
  val year2013                   = 2013
  val day7                       = 7
  val day1                       = 1

  "The current tax year" should {
    " be the same as current year, if the current date is before 6th April in the current year" in {
      val dateBeforeTaxYearButSameYearAsTaxYear = LocalDate.of(year2014, APRIL, day1)
      assert(taxDateUtils.getCurrentTaxYear(dateBeforeTaxYearButSameYearAsTaxYear) == year2013)
    }
  }

  "The application" should {
    "state the service has not launched if the current tax year is before the year of launch" in {
      assert(!taxDateUtils.isServiceLaunched(taxDateUtils.TAX_YEAR_OF_LAUNCH - 1))
    }
  }

  "The current tax year" should {
    " be current year, if the current date is before 6th April and in the previous year" in {
      val dateBeforeTaxYearInPreviousYear = LocalDate.of(year2013, NOVEMBER, 1)
      assert(taxDateUtils.getCurrentTaxYear(dateBeforeTaxYearInPreviousYear) == year2013)
    }
  }

  "The current tax year" should {
    " be current year, if the current date is after 6th April in the current year" in {
      val dateAfterTaxYear = LocalDate.of(year2014, JULY, day1)
      assert(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear) == year2014)
    }
  }

  "The application" should {
    "state the service has NOT launched if the date supplied has the same year as the launch date but is before April 6th" in {
      val dateAfterTaxYear = LocalDate.of(year2015, FEBRUARY, day1)
      assert(!taxDateUtils.isServiceLaunched(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear)))
    }
  }

  "The application" should {
    "state the service has  launched if the current tax year is after the year of launch" in {
      val dateAfterTaxYear = LocalDate.of(taxDateUtils.TAX_YEAR_OF_LAUNCH, APRIL, day7)
      assert(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear) < taxDateUtils.getTaxYearRange().cyminus1)
      assert(taxDateUtils.isServiceLaunched(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear)))
    }
  }

  "The application" should {
    "return the correct range for the current year" in {

      val dateAfterTaxYear = LocalDate.of(taxDateUtils.TAX_YEAR_OF_LAUNCH, APRIL, day7)
      assert(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear) < taxDateUtils.getTaxYearRange().cyminus1)
      assert(taxDateUtils.isServiceLaunched(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear)))
    }
  }

}
