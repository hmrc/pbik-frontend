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

package models.v1.exclusion

import base.FakePBIKApplication
import play.api.libs.json.{JsValue, Json}

class PbikExclusionPersonDeleteRequestSpec extends FakePBIKApplication {

  private val request: PbikExclusionPersonDeleteRequest = PbikExclusionPersonDeleteRequest(
    nationalInsuranceNumber = "AB123456C",
    firstForename = "John",
    surname = "Doe",
    optimisticLock = 42
  )

  "PbikExclusionPersonDeleteRequest" when {
    ".writes/reads" must {

      "serialize and deserialize correctly" in {
        val json         = Json.toJson(request)
        val deserialized = json.as[PbikExclusionPersonDeleteRequest]

        deserialized mustBe request
      }

      "serialize to expected JSON" in {
        val json = Json.toJson(request)

        (json \ "nationalInsuranceNumber").as[String] mustBe "AB123456C"
        (json \ "firstForename").as[String] mustBe "John"
        (json \ "surname").as[String] mustBe "Doe"
        (json \ "optimisticLock").as[Int] mustBe 42
      }

      "deserialize from JSON" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "nationalInsuranceNumber": "AB123456C",
            |  "firstForename": "John",
            |  "surname": "Doe",
            |  "optimisticLock": 42
            |}
            |""".stripMargin
        )

        val deserialized = Json.fromJson[PbikExclusionPersonDeleteRequest](json).get
        deserialized mustBe request
      }

      "fail to deserialize when nationalInsuranceNumber is missing" in {
        val invalidJson =
          """
            |{
            |  "firstForename": "John",
            |  "surname": "Doe",
            |  "optimisticLock": 1
            |}
            |""".stripMargin

        val result = Json.fromJson[PbikExclusionPersonDeleteRequest](Json.parse(invalidJson))

        result.isError mustBe true
      }

      "fail to deserialize when JSON is empty" in {
        val emptyJson = Json.parse("{}")

        val result = Json.fromJson[PbikExclusionPersonDeleteRequest](emptyJson)
        result.isError mustBe true
      }
    }
  }
}
