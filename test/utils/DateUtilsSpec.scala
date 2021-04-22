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
import utils.DateUtils._

class DateUtilsSpec extends WordSpecLike with Matchers with OptionValues {

  "NPS date conversion" should {
    " output 1 January 1980" in {
      val date: String = "01/01/1980"
      val convertedDate = NPSDateConversionFormat(date)

      convertedDate shouldBe "1 January 1980"
    }

    " output 30 August 2015" in {
      val date: String = "30/08/2015"
      val convertedDate = NPSDateConversionFormat(date)

      convertedDate shouldBe "30 August 2015"
    }
  }

}
