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
import org.mockito.Mockito._
import org.scalatest.{Matchers, OptionValues, WordSpecLike}

import scala.sys.SystemProperties

class SystemPropertiesHelperSpec extends WordSpecLike with Matchers with OptionValues with FakePBIKApplication {

  class StubSystemProperties extends SystemPropertiesHelper {

    override lazy val sysprop = mock(classOf[SystemProperties])

    when(sysprop.get("searchString")).thenReturn(Some("foundString"))
    when(sysprop.get("searchInt")).thenReturn(Some("555"))
    when(sysprop.get("searchBoolean")).thenReturn(Some("true"))
  }

  "When getting a Boolean system property which doesnt exist the helper" should {
    "return the default value" in {
      val s = new StubSystemProperties
      assert(s.getBooleanProperty("Wibble", false) == false)
    }
  }

  "When getting an Int system property which doesnt exist the helper" should {
    "return the default value" in {
      val s = new StubSystemProperties
      assert(s.getIntProperty("Intible", 5) == 5)
    }
  }

  "When getting a String system property which doesnt exist the helper" should {
    "return the default value" in {
      val s = new StubSystemProperties
      assert(s.getStringProperty("Wibble", "bibble") == "bibble")
    }
  }

  "When getting a Boolean system property which does exist the helper" should {
    "return the default value" in {
      val s = new StubSystemProperties
      assert(s.getBooleanProperty("searchBoolean", false) == true)
    }
  }

  "When getting an Int system property which does exist the helper" should {
    "return the default value" in {
      val s = new StubSystemProperties
      assert(s.getIntProperty("searchInt", -1) == 555)
    }
  }

  "When getting a String system property which does exist the helper" should {
    "return the default value" in {
      val s = new StubSystemProperties
      assert(s.getStringProperty("searchString", null) == "foundString")
    }
  }

}
