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

package models.v1

import base.FakePBIKApplication
import play.api.libs.json.Json

class NPSErrorSpec extends FakePBIKApplication {

  "NPSError" should {

    "serialize and deserialize correctly" in {
      val error  = NPSError("Some reason", "123")
      val json   = Json.toJson(error)
      val parsed = Json.parse(json.toString).as[NPSError]

      parsed mustBe error
    }

    "return code as Int when code is numeric" in {
      val error = NPSError("Numeric code", "456")
      error.codeAsInt mustBe 456
    }

    "return default code (0) and log error when code is non-numeric" in {
      val error = NPSError("Invalid code", "notANumber")
      error.codeAsInt mustBe 0
    }

    "fail to deserialize when JSON is empty" in {
      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[NPSError](emptyJson)
      result.isError mustBe true
    }
  }
}
