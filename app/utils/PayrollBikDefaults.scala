/*
 * Copyright 2020 HM Revenue & Customs
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

/**
  * Some defaults which are useful for setting when the application goes live
  * and allows the current year to be overridden by the OVERRIDE_YEAR_VALUE.
  *
  * Clients can override these values and mixin or specify the values as system properties
  */
trait PayrollBikDefaults extends SystemPropertiesHelper {

  lazy val TAX_YEAR_OF_LAUNCH = taxYearOfLaunch
  lazy val YEAR_LENGTH_VALUE = yearLengthValue
  val DEFAULT_LAUNCH_YEAR = 2015
  val DEFAULT_YEAR_DIGITS = 4

  def taxYearOfLaunch: Int = getIntProperty("TAX_YEAR_OF_LAUNCH", DEFAULT_LAUNCH_YEAR)
  def yearLengthValue: Int = getIntProperty("YEAR_LENGTH_VALUE", DEFAULT_YEAR_DIGITS)

}
