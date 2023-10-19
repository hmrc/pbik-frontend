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
import models.{EiLPerson, EiLPersonList, ExclusionNino, MandatoryRadioButton}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import utils.FormMappingsConstants._

import java.time.Month._

class FormMappingsSpec extends PlaySpec with FakePBIKApplication with Matchers {

  private val formMappings: FormMappings = app.injector.instanceOf[FormMappings]
  private val eilPerson: EiLPerson       =
    EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None)

  "FormMappings" when {
    "an input date" should {
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

    "a valid date" should {
      "be valid in all cases when the days in the year are greater than 0 and less than 29" in {
        Range.inclusive(1, 28)
          .map { i =>
            formMappings.addZeroIfNeeded(i.toString)
          }
          .foreach { a =>
            assert(formMappings.isValidDate((a, "01", "2014")))
          }
      }
    }

    "a day of 29" should {
      "be valid in February during a leap year (such as 2016)" in {
        assert(formMappings.isValidDate(("29", "02", "2016")))
      }
    }

    "a day of 29" should {
      "not be valid in February when its not a leap year (such as 2014)" in {
        assert(!formMappings.isValidDate(("29", "02", "2014")))
      }
    }

    "a day of 31" should {
      "not be valid in February, April, June, September, November" in {
        assert(!formMappings.isValidDate(("31", NOVEMBER.getValue.toString, "2014")))
        assert(!formMappings.isValidDate(("31", APRIL.getValue.toString, "2014")))
        assert(!formMappings.isValidDate(("31", SEPTEMBER.getValue.toString, "2014")))
        assert(!formMappings.isValidDate(("31", JUNE.getValue.toString, "2014")))
      }
    }

    "a day greater than 31" should {
      "not be valid in any month" in {
        (JANUARY.getValue to DECEMBER.getValue)
          .map { i =>
            formMappings.addZeroIfNeeded(i.toString)
          }
          .foreach { a =>
            assert(!formMappings.isValidDate(("32", a, "2014")))
          }
      }
    }

    "a day equal to 31" should {
      "be valid in January, May, July, August, October, December" in {
        assert(formMappings.isValidDate(("31", JANUARY.getValue.toString, "2014")))
        assert(formMappings.isValidDate(("31", MARCH.getValue.toString, "2014")))
        assert(formMappings.isValidDate(("31", MAY.getValue.toString, "2014")))
        assert(formMappings.isValidDate(("31", JULY.getValue.toString, "2014")))
        assert(formMappings.isValidDate(("31", AUGUST.getValue.toString, "2014")))
        assert(formMappings.isValidDate(("31", OCTOBER.getValue.toString, "2014")))
        assert(formMappings.isValidDate(("31", DECEMBER.getValue.toString, "2014")))
      }
    }

    "a day value less than 1" should {
      "not be valid in a month" in {
        (-1 to 0).foreach { i =>
          val day = formMappings.addZeroIfNeeded(i.toString)
          assert(!formMappings.isValidDate((day, "01", "2014")))
        }
      }
    }

    "a month value less than 1" should {
      "not be valid in a year" in {
        (-1 to 0).foreach { i =>
          val month = formMappings.addZeroIfNeeded(i.toString)
          assert(!formMappings.isValidDate(("01", month, "2014")))
        }
      }
    }

    "a month value greater than 12" should {
      "not be valid in a year" in {
        (DECEMBER.getValue + 1 to DECEMBER.getValue + 20)
          .map { i =>
            formMappings.addZeroIfNeeded(i.toString)
          }
          .foreach { a =>
            assert(!formMappings.isValidDate(("01", a, "2014")))
          }
      }
    }

    "a month value less than 12" should {
      "not be valid in a year" in {
        val number = -5
        (number to 0)
          .map { i =>
            formMappings.addZeroIfNeeded(i.toString)
          }
          .foreach { a =>
            assert(!formMappings.isValidDate(("01", a, "2014")))
          }
      }
    }

    "the regex for a year is 4 digits" should {
      "be [0-9][0-9][0-9][0-9]" in {
        val yearLength = 4
        assert(formMappings.generateYearString(yearLength) == "[0-9][0-9][0-9][0-9]")
      }
    }

    "the regex for a year is 2 digits" should {
      "be [0-9][0-9]" in {
        assert(formMappings.generateYearString(2) == "[0-9][0-9]")
      }
    }

    "individualsFormWithRadio is filled" should {
      "result in correct result" in {
        formMappings.individualsFormWithRadio.fill(("AB111111", EiLPersonList(List(eilPerson)))) mustBe
          formMappings.individualsFormWithRadio.bind(
            Map(
              ("individualSelection", "AB111111"),
              ("individuals[0].nino", "AB111111"),
              ("individuals[0].firstName", "Adam"),
              ("individuals[0].surname", "Smith"),
              ("individuals[0].dateOfBirth", "01/01/1980"),
              ("individuals[0].gender", "male"),
              ("individuals[0].perOptLock", "0")
            )
          )
      }
    }

    "individualsForm is filled" should {
      "result in correct result" in {
        formMappings.individualsForm.fill(EiLPersonList(List(eilPerson))) mustBe
          formMappings.individualsForm.bind(
            Map(
              ("individuals[0].nino", "AB111111"),
              ("individuals[0].firstName", "Adam"),
              ("individuals[0].surname", "Smith"),
              ("individuals[0].dateOfBirth", "01/01/1980"),
              ("individuals[0].gender", "male"),
              ("individuals[0].perOptLock", "0")
            )
          )
      }
    }

    "individualSelectionForm is filled" should {
      "result in correct result" in {
        formMappings.individualSelectionForm.fill(ExclusionNino("AB111111")) mustBe
          formMappings.individualSelectionForm.bind(Map(("individualNino", "AB111111")))
      }
    }

    "navigationRadioButton is filled" should {
      "result in correct result" in {
        formMappings.navigationRadioButton.fill(MandatoryRadioButton("cy")) mustBe
          formMappings.navigationRadioButton.bind(Map(("navigation", "cy")))
      }
    }
  }
}
