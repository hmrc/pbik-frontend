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

package models.form

import base.FakePBIKApplication
import play.api.libs.json.Json

class OtherReasonSpec extends FakePBIKApplication {

  "OtherReason" should {
    "serialize to JSON and back" in {
      val original = OtherReason("Some reason")
      val json = Json.toJson(original)
      val deserialized = json.as[OtherReason]

      deserialized mustBe original
    }

    "fail to deserialize when keyword is wrong" in {
      val invalidJson =
        """
          |{
          |  "otjherReasin": ""
          |}
          |""".stripMargin

      val result = Json.fromJson[OtherReason](Json.parse(invalidJson))

      result.isError mustBe true
    }

    "fail to deserialize when JSON is empty" in {
      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[OtherReason](emptyJson)
      result.isError mustBe true
    }
  }
}