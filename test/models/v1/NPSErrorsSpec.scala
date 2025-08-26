/*
 * Copyright 2025 HM Revenue & Customs
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

class NPSErrorsSpec extends FakePBIKApplication {

  "NPSErrors" should {

    "serialize and deserialize correctly" in {
      val error1   = NPSError("Reason 1", "123")
      val error2   = NPSError("Reason 2", "456")
      val errors   = NPSErrors(Seq(error1, error2))

      val json     = Json.toJson(errors)
      val parsed   = Json.parse(json.toString).as[NPSErrors]

      parsed mustBe errors
    }

    "handle empty list of failures" in {
      val errors = NPSErrors(Seq.empty)

      val json   = Json.toJson(errors)
      val parsed = Json.parse(json.toString).as[NPSErrors]

      parsed.failures mustBe empty
    }

    "fail to deserialize when JSON is empty" in {
      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[NPSErrors](emptyJson)
      result.isError mustBe true
    }
  }
}