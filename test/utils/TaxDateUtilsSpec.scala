/*
 * Copyright 2018 HM Revenue & Customs
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

import java.util.Date

import controllers.FakePBIKApplication
import org.joda.time.LocalDate
import org.joda.time.DateTimeConstants._
import play.api.Logger
import utils.TaxDateUtils
import uk.gov.hmrc.play.test.UnitSpec
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._

class TaxDateUtilsSpec extends UnitSpec with FakePBIKApplication {

  "The current tax year" should {
    " be the same as current year, if the current date is before 6th April in the current year" in {
      running(fakeApplication) {
        val dateBeforeTaxYearButSameYearAsTaxYear = new LocalDate(2014, APRIL, 1)
        assert(TaxDateUtils.getCurrentTaxYear(dateBeforeTaxYearButSameYearAsTaxYear) == 2013)
      }
    }
  }


  "The application " should {
    "state the service has not launched if the current tax year is before the year of launch" in {
        assert(TaxDateUtils.isServiceLaunched(TaxDateUtils.TAX_YEAR_OF_LAUNCH-1) == false)
    }
  }

  "The current tax year" should {
    " be current year, if the current date is before 6th April and in the previous year" in {
      val dateBeforeTaxYearInPreviousYear = new LocalDate(2013, NOVEMBER, 1)
        assert(TaxDateUtils.getCurrentTaxYear(dateBeforeTaxYearInPreviousYear) == 2013)
    }
  }

  "The current tax year" should {
    " be current year, if the current date is after 6th April in the current year" in {
      val dateAfterTaxYear = new LocalDate(2014, JULY, 1)
      assert(TaxDateUtils.getCurrentTaxYear(dateAfterTaxYear) == 2014)
    }
  }

  "The application " should {
    "state the service has NOT launched if the date supplied has the same year as the launch date but is before April 6th" in {
      val dateAfterTaxYear = new LocalDate(2015, FEBRUARY, 1)
      assert(TaxDateUtils.isServiceLaunched(TaxDateUtils.getCurrentTaxYear(dateAfterTaxYear)) == false)
    }
  }

  "The application " should {
    "state the service has  launched if the current tax year is after the year of launch" in {
      val dateAfterTaxYear = new LocalDate(TaxDateUtils.TAX_YEAR_OF_LAUNCH, APRIL, 7)
      assert(TaxDateUtils.getCurrentTaxYear(dateAfterTaxYear) < TaxDateUtils.getTaxYearRange().cyminus1)
      assert(TaxDateUtils.isServiceLaunched(TaxDateUtils.getCurrentTaxYear(dateAfterTaxYear)) == true)
    }
  }

  "The application " should {
    "return the correct range for the current year" in {

      val dateAfterTaxYear = new LocalDate(TaxDateUtils.TAX_YEAR_OF_LAUNCH, APRIL, 7)
      assert(TaxDateUtils.getCurrentTaxYear(dateAfterTaxYear)< TaxDateUtils.getTaxYearRange().cyminus1)
      assert(TaxDateUtils.isServiceLaunched(TaxDateUtils.getCurrentTaxYear(dateAfterTaxYear)) == true)
    }
  }


//  "The tax year range for the current year " should {
//    " be cy-1, cy and cy+1" in {
//      val taxYear2014 = new LocalDate(2015, APRIL, 1)
//      val range = TaxDateUtils.getTaxYearRange()
//      if (TaxDateUtils.applyYearOverride) {
//        assert(TaxDateUtils.applyYearOverride)
//      } else {
//         assert(range.cy == 2014 && range.cyminus1 == 2013 && range.cyplus1 == 2015)
//      }
//    }
//  }

}
