/*
 * Copyright 2024 HM Revenue & Customs
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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.i18n.{Messages, MessagesApi}
import utils.DateUtils._

class DateUtilsSpec extends AnyWordSpecLike with Matchers with FakePBIKApplication {

  private val messages: Messages   = app.injector.instanceOf[MessagesApi].preferred(Seq(lang))
  private val cyMessages: Messages = app.injector.instanceOf[MessagesApi].preferred(Seq(cyLang))

  private val npsMonths = Seq(
    "01",
    "02",
    "03",
    "04",
    "05",
    "06",
    "07",
    "08",
    "09",
    "10",
    "11",
    "12"
  )

  "DateUtils" should {
    npsMonths.foreach { month =>
      s"output 1 $month 1980 - English" in {
        val date: String  = s"01/$month/1980"
        val convertedDate = npsDateConversionFormat(date)(messages)
        val expectedMonth = messages(s"Service.month.$month")

        convertedDate shouldBe s"1 $expectedMonth 1980"
      }

      s"output 1 $month 1980 - Welsh" in {
        val date: String  = s"01/$month/1980"
        val convertedDate = npsDateConversionFormat(date)(cyMessages)
        val expectedMonth = cyMessages(s"Service.month.$month")

        convertedDate shouldBe s"1 $expectedMonth 1980"
      }
    }
  }

}
