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

import play.api.Logger

import scala.sys.SystemProperties

/**
  * Helper to check for System properties and marshal them to the correct types
  * or return the supplied default values
  */
trait SystemPropertiesHelper {

  lazy val sysprop = new SystemProperties()

  def getIntProperty(key: String, defaultValue: Int): Int =
    try {
      if (sysprop.get(key).isDefined) {
        sysprop.get(key).get.toInt
      } else {
        doesntExist(key, defaultValue)
      }
    } catch {
      case t: Throwable => doesntParse(key, defaultValue, t.getMessage)
    }

  def getStringProperty(key: String, defaultValue: String): String =
    try {
      if (sysprop.get(key).isDefined) {
        sysprop.get(key).getOrElse(defaultValue)
      } else {
        doesntExist(key, defaultValue)
      }
    } catch {
      case t: Throwable => doesntParse(key, defaultValue, t.getMessage)
    }

  def getBooleanProperty(key: String, defaultValue: Boolean): Boolean =
    try {
      if (sysprop.get(key).isDefined) {
        sysprop.get(key).get.toBoolean
      } else {
        doesntExist(key, defaultValue)
      }
    } catch {
      case t: Throwable => doesntParse(key, defaultValue, t.getMessage)
    }

  def doesntExist[T](key: String, defaultvalue: T): T = {
    Logger.info(
      s"[SystemPropertiesHelper][doesntExist] No system property $key defined. Using default value: $defaultvalue")
    defaultvalue
  }

  def doesntParse[T](key: String, defaultvalue: T, errorMsg: String): T = {
    Logger.warn(
      s"[SystemPropertiesHelper][doesntParse] System property $key exists but could not be parsed to the correct type." +
        s" Please check the value: $defaultvalue. Error was $errorMsg"
    )
    defaultvalue
  }

}
