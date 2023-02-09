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

import controllers.FakePBIKApplication
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import java.time.LocalDate
import java.time.Month.{APRIL, FEBRUARY, JULY, NOVEMBER}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class TaxDateUtilsSpec extends AnyWordSpecLike with Matchers with OptionValues with FakePBIKApplication {

  private val (year2013, year2014, year2015): (Int, Int, Int) = (2013, 2014, 2015)
  private val (day1, day7): (Int, Int)                        = (1, 7)

  private val conf: Map[String, Int] = Map(
    "pbik.date.override.0" -> 2015,
    "pbik.date.override.1" -> 7,
    "pbik.date.override.2" -> 1
  )

  private lazy val appWithPbikDateOverride: Application    = GuiceApplicationBuilder().configure(conf).build()
  private lazy val appWithoutPbikDateOverride: Application = GuiceApplicationBuilder().configure().build()

  private class Test(app: Application = appWithoutPbikDateOverride) {
    val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]
  }

  "TaxDateUtils" when {
    "getDefaultDate" when {
      def test(app: Application, configurationMessage: String, date: LocalDate): Unit =
        s"pbik.date.override is $configurationMessage" should {
          s"return the date $date" in new Test(app) {
            assert(taxDateUtils.getDefaultDate == date)
          }
        }

      val inputArgs = Seq(
        (appWithPbikDateOverride, "configured", LocalDate.of(year2015, day7, day1)),
        (appWithoutPbikDateOverride, "not configured", LocalDate.now())
      )

      inputArgs.foreach(args => (test _).tupled(args))
    }

    "getDefaultYear" when {
      def test(app: Application, configurationMessage: String, year: Int): Unit =
        s"pbik.date.override is $configurationMessage" should {
          s"return the year $year" in new Test(app) {
            assert(taxDateUtils.getDefaultYear == year)
          }
        }

      val inputArgs = Seq(
        (appWithPbikDateOverride, "configured", LocalDate.now().getYear + 1),
        (appWithoutPbikDateOverride, "not configured", LocalDate.now().getYear)
      )

      inputArgs.foreach(args => (test _).tupled(args))
    }

    "The current tax year" should {
      "be the same as current year, if the current date is before 6th April in the current year" in new Test {
        val dateBeforeTaxYearButSameYearAsTaxYear: LocalDate = LocalDate.of(year2014, APRIL, day1)
        assert(taxDateUtils.getCurrentTaxYear(dateBeforeTaxYearButSameYearAsTaxYear) == year2013)
      }
    }

    "The application" should {
      "state the service has not launched if the current tax year is before the year of launch" in new Test {
        assert(!taxDateUtils.isServiceLaunched(taxDateUtils.TAX_YEAR_OF_LAUNCH - 1))
      }
    }

    "The current tax year" should {
      "be current year, if the current date is before 6th April and in the previous year" in new Test {
        val dateBeforeTaxYearInPreviousYear: LocalDate = LocalDate.of(year2013, NOVEMBER, day1)
        assert(taxDateUtils.getCurrentTaxYear(dateBeforeTaxYearInPreviousYear) == year2013)
      }
    }

    "The current tax year" should {
      "be current year, if the current date is after 6th April in the current year" in new Test {
        val dateAfterTaxYear: LocalDate = LocalDate.of(year2014, JULY, day1)
        assert(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear) == year2014)
      }
    }

    "The application" should {
      "state the service has NOT launched if the date supplied has the same year as the launch date but is before April 6th" in new Test {
        val dateAfterTaxYear: LocalDate = LocalDate.of(year2015, FEBRUARY, day1)
        assert(!taxDateUtils.isServiceLaunched(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear)))
      }
    }

    "The application" should {
      "state the service has  launched if the current tax year is after the year of launch" in new Test {
        val dateAfterTaxYear: LocalDate = LocalDate.of(taxDateUtils.TAX_YEAR_OF_LAUNCH, APRIL, day7)
        assert(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear) < taxDateUtils.getTaxYearRange().cyminus1)
        assert(taxDateUtils.isServiceLaunched(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear)))
      }
    }

    "The application" should {
      "return the correct range for the current year" in new Test {
        val dateAfterTaxYear: LocalDate = LocalDate.of(taxDateUtils.TAX_YEAR_OF_LAUNCH, APRIL, day7)
        assert(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear) < taxDateUtils.getTaxYearRange().cyminus1)
        assert(taxDateUtils.isServiceLaunched(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear)))
      }
    }
  }
}
