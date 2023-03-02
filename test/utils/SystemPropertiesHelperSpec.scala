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

import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike

class SystemPropertiesHelperSpec extends AnyWordSpecLike with OptionValues {

  private class StubSystemProperties extends SystemPropertiesHelper

  private val stubSystemProperties: StubSystemProperties = new StubSystemProperties

  "SystemPropertiesHelper" when {
    "getting an Int system property which does not exist, the helper" should {
      "return the default value" in {

        val defaultValue = 5
        stubSystemProperties.getIntProperty("some-non-existing-key", defaultValue) mustBe 5
      }
    }

    "getting an Int system property which does exist, the helper" should {
      "return the default value" in {

        val defaultValue = -1

        System.setProperty("searchInt", "555")
        stubSystemProperties.getIntProperty("searchInt", defaultValue) mustBe 555
      }
    }
  }
}
