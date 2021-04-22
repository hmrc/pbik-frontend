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

import java.time.LocalDate

import controllers.FakePBIKApplication
import org.joda.time.DateTimeConstants._
import org.scalatest.{Matchers, OptionValues, WordSpecLike}

class TaxDateUtilsSpec extends WordSpecLike with Matchers with OptionValues with FakePBIKApplication {

  val taxDateUtils = app.injector.instanceOf[TaxDateUtils]

  "The current tax year" should {
    " be the same as current year, if the current date is before 6th April in the current year" in {
      val dateBeforeTaxYearButSameYearAsTaxYear = LocalDate.of(2014, APRIL, 1)
      assert(taxDateUtils.getCurrentTaxYear(dateBeforeTaxYearButSameYearAsTaxYear) == 2013)
    }
  }

  "The application" should {
    "state the service has not launched if the current tax year is before the year of launch" in {
      assert(!taxDateUtils.isServiceLaunched(taxDateUtils.TAX_YEAR_OF_LAUNCH - 1))
    }
  }

  "The current tax year" should {
    " be current year, if the current date is before 6th April and in the previous year" in {
      val dateBeforeTaxYearInPreviousYear = LocalDate.of(2013, NOVEMBER, 1)
      assert(taxDateUtils.getCurrentTaxYear(dateBeforeTaxYearInPreviousYear) == 2013)
    }
  }

  "The current tax year" should {
    " be current year, if the current date is after 6th April in the current year" in {
      val dateAfterTaxYear = LocalDate.of(2014, JULY, 1)
      assert(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear) == 2014)
    }
  }

  "The application" should {
    "state the service has NOT launched if the date supplied has the same year as the launch date but is before April 6th" in {
      val dateAfterTaxYear = LocalDate.of(2015, FEBRUARY, 1)
      assert(!taxDateUtils.isServiceLaunched(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear)))
    }
  }

  "The application" should {
    "state the service has  launched if the current tax year is after the year of launch" in {
      val dateAfterTaxYear = LocalDate.of(taxDateUtils.TAX_YEAR_OF_LAUNCH, APRIL, 7)
      assert(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear) < taxDateUtils.getTaxYearRange().cyminus1)
      assert(taxDateUtils.isServiceLaunched(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear)))
    }
  }

  "The application" should {
    "return the correct range for the current year" in {

      val dateAfterTaxYear = LocalDate.of(taxDateUtils.TAX_YEAR_OF_LAUNCH, APRIL, 7)
      assert(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear) < taxDateUtils.getTaxYearRange().cyminus1)
      assert(taxDateUtils.isServiceLaunched(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear)))
    }
  }

}
