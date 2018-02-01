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

import scala.sys.SystemProperties
import play.api.Logger

/**
 * Helper to check for System properties and marshal them to the correct types
 * or return the supplied default values
 */
trait SystemPropertiesHelper {

  lazy val sysprop = new SystemProperties()

  def getIntProperty(key:String, defaultValue:Int):Int = {
    try {
      sysprop.get(key).isDefined match {
        case true => sysprop.get(key).get.toInt
        case false => dosntExist(key,defaultValue)
      }
    } catch {
      case t:Throwable => dosntParse(key,defaultValue,t.getMessage)
    }
  }

  def getStringProperty(key:String, defaultValue:String):String = {
    try {
      sysprop.get(key).isDefined match {
        case true => sysprop.get(key).getOrElse(defaultValue)
        case false => dosntExist(key,defaultValue)
      }
    } catch {
      case t:Throwable => dosntParse(key,defaultValue,t.getMessage)
    }
  }

  def getBooleanProperty(key:String, defaultValue:Boolean):Boolean = {
    try {
      sysprop.get(key).isDefined match {
        case true => sysprop.get(key).get.toBoolean
        case false => dosntExist(key,defaultValue)
      }
    } catch {
      case t:Throwable => dosntParse(key,defaultValue,t.getMessage)
    }
  }

  def dosntExist[T](key:String, defaultvalue:T):T = {
    Logger.info("No system property " + key + " defined. Using default value: " + defaultvalue)
    defaultvalue
  }

  def dosntParse[T](key:String, defaultvalue:T, errorMsg:String):T = {
    Logger.warn("System property " + key +
      " exists but could not be parsed to the correct type. Please check the value: " + defaultvalue +
      ". Error was " + errorMsg)
    defaultvalue
  }


}
