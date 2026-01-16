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
import play.api.libs.json.Json

class BinaryRadioButtonWithDescSpec extends FakePBIKApplication {

  "BinaryRadioButtonWithDesc" should {

    "serialize and deserialize JSON correctly" in {
      val obj = BinaryRadioButtonWithDesc.apply("yes", Some("Some info"))

      val json = Json.toJson(obj)
      json.toString must include("yes")
      json.toString must include("Some info")

      val parsed = json.as[BinaryRadioButtonWithDesc]
      parsed mustBe obj
    }

    "handle None info field correctly" in {
      val obj = BinaryRadioButtonWithDesc("no", None)

      val json = Json.toJson(obj)
      json.toString must include("no")

      val parsed = json.as[BinaryRadioButtonWithDesc]
      parsed mustBe obj
    }

    "fail to deserialize when selectionValue is empty" in {
      val invalidJson =
        """
          |{
          |  "info": "some info"
          |}
          |""".stripMargin

      val result = Json.fromJson[BinaryRadioButtonWithDesc](Json.parse(invalidJson))

      result.isError mustBe true
    }

    "fail to deserialize when JSON is empty" in {
      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[BinaryRadioButtonWithDesc](emptyJson)
      result.isError mustBe true
    }
  }
}
