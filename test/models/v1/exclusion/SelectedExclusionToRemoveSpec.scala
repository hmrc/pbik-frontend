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

package models.v1.exclusion

import base.FakePBIKApplication
import play.api.libs.json.{JsValue, Json}

class SelectedExclusionToRemoveSpec extends FakePBIKApplication {

  private val person: PbikExclusionPerson = PbikExclusionPerson(
    nationalInsuranceNumber = "AB123456C",
    firstForename = "John",
    secondForename = None,
    surname = "Doe",
    worksPayrollNumber = None,
    optimisticLock = 42
  )

  private val exclusion: SelectedExclusionToRemove = SelectedExclusionToRemove(
    employerOptimisticLock = 99,
    personToExclude = person
  )

  "SelectedExclusionToRemove" when {
    ".writes/reads" must {

      "serialize and deserialize correctly" in {
        val json         = Json.toJson(exclusion)
        val deserialized = json.as[SelectedExclusionToRemove]

        deserialized mustBe exclusion
      }

      "serialize to expected JSON structure" in {
        val json = Json.toJson(exclusion)

        (json \ "employerOptimisticLock").as[Int] mustBe 99
        (json \ "personToExclude" \ "nationalInsuranceNumber").as[String] mustBe "AB123456C"
        (json \ "personToExclude" \ "firstForename").as[String] mustBe "John"
        (json \ "personToExclude" \ "surname").as[String] mustBe "Doe"
        (json \ "personToExclude" \ "optimisticLock").as[Int] mustBe 42
      }

      "deserialize from JSON" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "employerOptimisticLock": 99,
            |  "personToExclude": {
            |    "nationalInsuranceNumber": "AB123456C",
            |    "firstForename": "John",
            |    "surname": "Doe",
            |    "optimisticLock": 42
            |  }
            |}
            |""".stripMargin
        )

        val deserialized = Json.fromJson[SelectedExclusionToRemove](json).get
        deserialized mustBe exclusion
      }

      "fail to deserialize when personToExclude is missing" in {
        val invalidJson =
          """
            |{
            |  "employerOptimisticLock": 1
            |}
            |""".stripMargin

        val result = Json.fromJson[SelectedExclusionToRemove](Json.parse(invalidJson))

        result.isError mustBe true
      }

      "fail to deserialize when JSON is empty" in {
        val emptyJson = Json.parse("{}")

        val result = Json.fromJson[SelectedExclusionToRemove](emptyJson)
        result.isError mustBe true
      }
    }
  }
}
