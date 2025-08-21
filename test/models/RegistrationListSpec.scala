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
import models.form.BinaryRadioButtonWithDesc
import models.v1.IabdType
import play.api.libs.json.{JsValue, Json}


class RegistrationListSpec extends FakePBIKApplication {

  private val item1: RegistrationItem = RegistrationItem(IabdType.CarBenefit, active = true, enabled = false)
  private val item2: RegistrationItem = RegistrationItem(IabdType.MedicalInsurance, active = false, enabled = true)

  private val reason: BinaryRadioButtonWithDesc =
    BinaryRadioButtonWithDesc(selectionValue = "no", info = Some("Test reason"))

  "RegistrationList" when {
    ".writes/reads" must {

      "serialize and deserialize RegistrationList with all fields" in {
        val list = RegistrationList(
          selectAll = Some("all"),
          active = List(item1, item2),
          reason = Some(reason)
        )

        val json         = Json.toJson(list)
        val deserialized = Json.fromJson[RegistrationList](json).get

        deserialized mustBe list
      }

      "serialize and deserialize RegistrationList with minimal fields" in {
        val list = RegistrationList(
          selectAll = None,
          active = List(item1),
          reason = None
        )

        val json         = Json.toJson(list)
        val deserialized = Json.fromJson[RegistrationList](json).get

        deserialized mustBe list
      }

      "serialize to expected JSON structure" in {
        val list = RegistrationList(
          selectAll = Some("all"),
          active = List(item1),
          reason = Some(reason)
        )

        val json = Json.toJson(list)

        (json \ "selectAll").as[String] mustBe "all"
        (json \ "active").as[List[JsValue]].size mustBe 1
        (json \ "reason" \ "selectionValue").as[String] mustBe "no"
        (json \ "reason" \ "info").as[String] mustBe "Test reason"
      }

      "deserialize JSON into RegistrationList" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "selectAll": "none",
            |  "active": [
            |    { "iabdType": "Car Benefit", "active": true, "enabled": false },
            |    { "iabdType": "Medical Insurance", "active": false, "enabled": true }
            |  ],
            |  "reason": {
            |    "selectionValue": "yes",
            |    "info": "Another reason"
            |  }
            |}
            |""".stripMargin
        )

        val deserialized = Json.fromJson[RegistrationList](json).get

        deserialized mustBe RegistrationList(
          selectAll = Some("none"),
          active = List(item1, item2),
          reason = Some(BinaryRadioButtonWithDesc(selectionValue = "yes", info = Some("Another reason")))
        )
      }

      "fail to deserialize when active list is missing" in {
        val invalidJson =
          """
            |{
            |  "selectAll": "yes"
            |}
            |""".stripMargin

        val result = Json.fromJson[RegistrationList](Json.parse(invalidJson))

        result.isError mustBe true
      }

      "fail to deserialize when JSON is empty" in {
        val emptyJson = Json.parse("{}")

        val result = Json.fromJson[RegistrationList](emptyJson)
        result.isError mustBe true
      }
    }
  }
}