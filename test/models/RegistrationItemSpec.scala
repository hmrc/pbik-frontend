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

package models

import base.FakePBIKApplication
import models.v1.IabdType
import play.api.libs.json.{JsValue, Json}

class RegistrationItemSpec extends FakePBIKApplication {

  private val item: RegistrationItem = RegistrationItem(
    IabdType.CarBenefit,
    active = true,
    enabled = false
  )

  "RegistrationItem" when {
    ".writes/reads" must {

      "serialize and deserialize a RegistrationItem correctly" in {
        val json         = Json.toJson(item)
        val deserialized = Json.fromJson[RegistrationItem](json).get

        deserialized mustBe item
      }

      "serialize a RegistrationItem to expected JSON" in {
        val json = Json.toJson(item)
        (json \ "iabdType").as[String] mustBe "Car Benefit"
        (json \ "active").as[Boolean] mustBe true
        (json \ "enabled").as[Boolean] mustBe false
      }

      "deserialize JSON into a RegistrationItem" in {
        val json: JsValue =
          Json.parse(
            """
              |{
              |  "iabdType": "Car Benefit",
              |  "active": false,
              |  "enabled": true
              |}
              |""".stripMargin
          )

        val deserialized = Json.fromJson[RegistrationItem](json).get
        deserialized mustBe RegistrationItem(IabdType.CarBenefit, active = false, enabled = true)
      }

      "fail to deserialize when iabdType is missing" in {
        val invalidJson =
          """
            |{
            |  "active": true,
            |  "enabled": false
            |}
            |""".stripMargin

        val result = Json.fromJson[RegistrationItem](Json.parse(invalidJson))

        result.isError mustBe true
      }

      "fail to deserialize when JSON is empty" in {
        val emptyJson = Json.parse("{}")

        val result = Json.fromJson[RegistrationItem](emptyJson)
        result.isError mustBe true
      }
    }
  }
}
