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

import controllers.FakePBIKApplication
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import java.time.Month.{APRIL, JULY, NOVEMBER}

class TaxDateUtilsSpec extends AnyWordSpecLike with Matchers with OptionValues with FakePBIKApplication {

  private val (year2013, year2014): (Int, Int) = (2013, 2014)
  private val day1: Int                        = 1

  private lazy val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]

  "TaxDateUtils" when {
    ".getCurrentTaxYear" should {
      "return the same as current year, if the current date is before 6th April in the current year" in {
        val dateBeforeTaxYearButSameYearAsTaxYear: LocalDate = LocalDate.of(year2014, APRIL, day1)
        assert(taxDateUtils.getCurrentTaxYear(dateBeforeTaxYearButSameYearAsTaxYear) == year2013)
      }

      "return current year, if the current date is before 6th April and in the previous year" in {
        val dateBeforeTaxYearInPreviousYear: LocalDate = LocalDate.of(year2013, NOVEMBER, day1)
        assert(taxDateUtils.getCurrentTaxYear(dateBeforeTaxYearInPreviousYear) == year2013)
      }

      "return current year, if the current date is after 6th April in the current year" in {
        val dateAfterTaxYear: LocalDate = LocalDate.of(year2014, JULY, day1)
        assert(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear) == year2014)
      }
    }

    ".isCurrentTaxYear" should {
      "return true, if the year is the current year" in {
        assert(taxDateUtils.isCurrentTaxYear(TaxYear.current.startYear))
      }

      "return false, if the year is not the current year" in {
        assert(!taxDateUtils.isCurrentTaxYear(year2013))
      }
    }
  }
}
