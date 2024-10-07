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

import base.FakePBIKApplication
import play.api.i18n.{Messages, MessagesApi}
import uk.gov.hmrc.time.TaxYear
import utils.Exceptions.InvalidYearURIException

import java.time.Month.{APRIL, JULY, NOVEMBER}
import java.time.{LocalDate, Month}

class TaxDateUtilsSpec extends FakePBIKApplication {

  private lazy val taxDateUtils: TaxDateUtils = new TaxDateUtils()
  private val cy: Int                         = taxDateUtils.getTaxYearRange().cyminus1
  private val cyp1: Int                       = taxDateUtils.getTaxYearRange().cy
  private val day1: Int                       = 1
  private val inputDates: Seq[LocalDate]      = Month.values().toList.map(month => LocalDate.of(cy, month, day1))
  private val messages: Messages              = app.injector.instanceOf[MessagesApi].preferred(Seq(lang))
  private val cyMessages: Messages            = app.injector.instanceOf[MessagesApi].preferred(Seq(cyLang))

  "TaxDateUtils" when {
    ".getCurrentTaxYear" should {
      "return the same as current year, if the current date is before 6th April in the current year" in {
        val dateBeforeTaxYearButSameYearAsTaxYear: LocalDate = LocalDate.of(cyp1, APRIL, day1)
        assert(taxDateUtils.getCurrentTaxYear(dateBeforeTaxYearButSameYearAsTaxYear) == cy)
      }

      "return current year, if the current date is before 6th April and in the previous year" in {
        val dateBeforeTaxYearInPreviousYear: LocalDate = LocalDate.of(cy, NOVEMBER, day1)
        assert(taxDateUtils.getCurrentTaxYear(dateBeforeTaxYearInPreviousYear) == cy)
      }

      "return current year, if the current date is after 6th April in the current year" in {
        val dateAfterTaxYear: LocalDate = LocalDate.of(cyp1, JULY, day1)
        assert(taxDateUtils.getCurrentTaxYear(dateAfterTaxYear) == cyp1)
      }
    }

    ".isCurrentTaxYear" should {
      "return true, if the year is the current year" in {
        assert(taxDateUtils.isCurrentTaxYear(TaxYear.current.startYear))
      }

      "return false, if the year is not the current year" in {
        assert(!taxDateUtils.isCurrentTaxYear(cyp1))
      }
    }

    ".mapYearStringToInt" should {
      "return the CY year for the given year" in {
        assert(taxDateUtils.mapYearStringToInt(FormMappingsConstants.CY) == cy)
      }

      "return the CY1 year for the given year" in {
        assert(taxDateUtils.mapYearStringToInt(FormMappingsConstants.CYP1) == cyp1)
      }

      "mapping an unknown string throw an InvalidYearURIException" in {
        intercept[InvalidYearURIException] {
          taxDateUtils.mapYearStringToInt("ceeewhyploosWon")
        }
      }
    }

    ".getDisplayTodayDate" should {
      inputDates.foreach(date =>
        s"return the correct date for ${date.getMonth} ${date.getYear} - English" in {
          assert(
            taxDateUtils
              .getDisplayTodayDate(date)(messages) == s"$day1 ${messages(s"Service.month.${date.getMonthValue}")} ${date.getYear}"
          )
        }
      )

      inputDates.foreach(date =>
        s"return the correct date for ${date.getMonth} ${date.getYear} - Welsh" in {
          assert(
            taxDateUtils
              .getDisplayTodayDate(date)(cyMessages) == s"$day1 ${cyMessages(s"Service.month.${date.getMonthValue}")} ${date.getYear}"
          )
        }
      )
    }
  }

}
