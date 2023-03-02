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

import play.api.Logging

/**
  * Helper to check for System properties and marshal them to the correct types
  * or return the supplied default values
  */
trait SystemPropertiesHelper extends Logging {

  def getIntProperty(key: String, defaultValue: Int): Int =
    try if (Some(System.getProperty(key)).isDefined) {
      System.getProperty(key).toInt
    } else {
      doesntExist(key, defaultValue)
    } catch {
      case t: Throwable =>
        doesntParse(key, defaultValue, t.getMessage)
    }

  def doesntExist[T](key: String, defaultValue: T): T = {
    logger.info(
      s"[SystemPropertiesHelper][doesntExist] No system property $key defined. Using default value: $defaultValue"
    )
    defaultValue
  }

  def doesntParse[T](key: String, defaultValue: T, errorMsg: String): T = {
    logger.warn(
      s"[SystemPropertiesHelper][doesntParse] System property $key exists but could not be parsed to the correct type." +
        s" Please check the value: $defaultValue. Error was $errorMsg"
    )
    defaultValue
  }

}
