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

import org.scalatest.{Matchers, OptionValues, WordSpecLike}

class URIValuesSpec extends WordSpecLike with Matchers with OptionValues with URIValues {

  "The Get Benefit Types path" should {
    " be equal to getbenefittypes" in {
      assert(getBenefitTypesPath == "getbenefittypes")
    }
  }

  "The Update Benefit Types path" should {
    " be equal to updatebenefittypes" in {
      assert(updateBenefitTypesPath == "updatebenefittypes")
    }
  }

  "The Add Exclusion path" should {
    " be equal to exclusion/update" in {
      assert(addExclusionPath == "exclusion/update")
    }
  }

  "The Exclusion path" should {
    " be equal to exclusion" in {
      assert(exclusionPath == "exclusion")
    }
  }

  "The Remove Exclusion path" should {
    " be equal to exclusion/remove" in {
      assert(addExclusionRemovePath == "exclusion/remove")
    }
  }

  "The construction of the Exclusion path given the test IABD Value 12345" should {
    " be equal to /12345/exclusion" in {
      assert(exclusionGetPath("12345") == "12345/exclusion")
    }
  }

  "The construction of the Exclusion Post Update path given the test IABD Value 12345" should {
    " be equal to /12345/exclusion/update" in {
      assert(exclusionPostUpdatePath("12345") == "12345/exclusion/update")
    }
  }

  "The construction of the Exclusion Post Remove path given the test IABD Value 12345" should {
    " be equal to /12345/exclusion/remove" in {
      assert(exclusionPostRemovePath("12345") == "12345/exclusion/remove")
    }
  }

}
