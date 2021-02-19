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

import controllers.FakePBIKApplication
import org.joda.time.DateTimeConstants._
import org.scalatestplus.play.PlaySpec
import utils.FormMappingsConstants._

class FormMappingsSpec extends PlaySpec with FakePBIKApplication {

  val formMappings: FormMappings = app.injector.instanceOf[FormMappings]

  "An input date" should {
    "be zero padded, if only one digit of the date is supplied" in {
      assert(formMappings.addZeroIfNeeded("1") == "01")
      assert(formMappings.addZeroIfNeeded("2") == "02")
      assert(formMappings.addZeroIfNeeded("3") == "03")
      assert(formMappings.addZeroIfNeeded("4") == "04")
      assert(formMappings.addZeroIfNeeded("5") == "05")
      assert(formMappings.addZeroIfNeeded("6") == "06")
      assert(formMappings.addZeroIfNeeded("7") == "07")
      assert(formMappings.addZeroIfNeeded("8") == "08")
      assert(formMappings.addZeroIfNeeded("9") == "09")
      assert(formMappings.addZeroIfNeeded("10") == "10")
      assert(formMappings.addZeroIfNeeded("11") == "11")
      assert(formMappings.addZeroIfNeeded("20") == "20")
      assert(formMappings.addZeroIfNeeded("22") == "22")
      assert(formMappings.addZeroIfNeeded("30") == "30")
    }
  }

  "A Valid date" should {
    "be valid in all cases when the days in the year are greater than 0 and less than 29" in {
      RANGE_28_DAYS
        .map { i =>
          formMappings.addZeroIfNeeded(i.toString)
        }
        .foreach { a =>
          assert(formMappings.isValidDate((a, "01", "2014")))
        }
    }
  }

  "A day of 29" should {
    "be valid in Febuary during a leap year ( such as 2016 )" in {
      assert(formMappings.isValidDate(("29", "02", "2016")))
    }
  }

  "A day of 29" should {
    "not be valid in February when its not a leap year ( such as 2014 )" in {
      assert(!formMappings.isValidDate(("29", "02", "2014")))
    }
  }

  "A day of 31" should {
    "not be valid in February, April, June, Septmember, November" in {
      assert(!formMappings.isValidDate(("31", NOVEMBER.toString, "2014")))
      assert(!formMappings.isValidDate(("31", APRIL.toString, "2014")))
      assert(!formMappings.isValidDate(("31", SEPTEMBER.toString, "2014")))
      assert(!formMappings.isValidDate(("31", JUNE.toString, "2014")))
    }
  }

  "A day greater than 31" should {
    "not be valid in any month" in {
      (JANUARY to DECEMBER)
        .map { i =>
          formMappings.addZeroIfNeeded(i.toString)
        }
        .foreach { a =>
          assert(!formMappings.isValidDate(("32", a, "2014")))
        }
    }
  }

  "A day equal to 31" should {
    "be valid in Jan, May, Jul,Aug,Oct,Dec" in {
      assert(formMappings.isValidDate(("31", JANUARY.toString, "2014")))
      assert(formMappings.isValidDate(("31", MARCH.toString, "2014")))
      assert(formMappings.isValidDate(("31", MAY.toString, "2014")))
      assert(formMappings.isValidDate(("31", JULY.toString, "2014")))
      assert(formMappings.isValidDate(("31", AUGUST.toString, "2014")))
      assert(formMappings.isValidDate(("31", OCTOBER.toString, "2014")))
      assert(formMappings.isValidDate(("31", DECEMBER.toString, "2014")))
    }
  }

  "A day value less than 1" should {
    "not be valid in a month" in {
      (-1 to 0).foreach { i =>
        val day = formMappings.addZeroIfNeeded(i.toString)
        assert(!formMappings.isValidDate((day, "01", "2014")))
      }
    }
  }

  "A month value less than 1" should {
    "not be valid in a year" in {
      (-1 to 0).foreach { i =>
        val month = formMappings.addZeroIfNeeded(i.toString)
        assert(!formMappings.isValidDate(("01", month, "2014")))
      }
    }
  }

  "A month value greater than 12" should {
    "not be valid in a year" in {
      (DECEMBER + 1 to DECEMBER + 20)
        .map { i =>
          formMappings.addZeroIfNeeded(i.toString)
        }
        .foreach { a =>
          assert(!formMappings.isValidDate(("01", a, "2014")))
        }
    }
  }

  "A month value less than 12" should {
    "not be valid in a year" in {
      (-5 to 0)
        .map { i =>
          formMappings.addZeroIfNeeded(i.toString)
        }
        .foreach { a =>
          assert(!formMappings.isValidDate(("01", a, "2014")))
        }
    }
  }

  "The Regex for a year with 4 digits" should {
    "be [0-9][0-9][0-9][0-9]" in {
      assert(formMappings.generateYearString(4) == "[0-9][0-9][0-9][0-9]")
    }
  }

  "The Regex for a year with 2 digits" should {
    "be [[0-9][0-9]" in {
      assert(formMappings.generateYearString(2) == "[0-9][0-9]")
    }
  }
}
