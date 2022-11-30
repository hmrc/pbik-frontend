/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import utils.Exceptions.InvalidBikTypeURIException

class URIInformationSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite {

  private val uriInformation = new URIInformation(app.injector.instanceOf[Configuration])

  "URIInformation" when {
    "iabdValueURLMapper" should {
      "return the appropriate mapped url for a valid bik" in {
        uriInformation.iabdValueURLMapper("39") shouldBe "assets-disposal"
      }

      "return InvalidBikTypeURIException for an invalid bik" in {
        intercept[InvalidBikTypeURIException] {
          uriInformation.iabdValueURLMapper("100")
        }
      }
    }

    "iabdValueURLDeMapper" should {
      "return the mapped iabd value for a valid Bik" in {
        uriInformation.iabdValueURLDeMapper("entertainment") shouldBe "42"
      }

      "return InvalidBikTypeURIException for an invalid bik" in {
        intercept[InvalidBikTypeURIException] {
          uriInformation.iabdValueURLDeMapper("plane")
        }
      }
    }
  }

}
