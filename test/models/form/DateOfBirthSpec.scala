/*
 * Copyright 2026 HM Revenue & Customs
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

package models.form

import base.FakePBIKApplication

class DateOfBirthSpec extends FakePBIKApplication {
  "DateOfBirth" should {
    "format the date correctly for NPS" in {
      val dob = DateOfBirth("05", "08", "1990")
      dob.dateOfBirthFormatForNPS mustBe "1990-08-05"
    }
  }
}
