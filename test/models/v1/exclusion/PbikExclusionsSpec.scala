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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class PbikExclusionsSpec extends AnyWordSpec with Matchers {

  "PbikExclusions" should {

    "serialize and deserialize correctly" in {
      val person1 = PbikExclusionPerson(
        nationalInsuranceNumber = "AB123456C",
        firstForename = "John",
        secondForename = Some("M"),
        surname = "Doe",
        worksPayrollNumber = Some("001"),
        optimisticLock = 1
      )
      val person2 = PbikExclusionPerson(
        nationalInsuranceNumber = "CD654321B",
        firstForename = "Jane",
        secondForename = None,
        surname = "Smith",
        worksPayrollNumber = Some("002"),
        optimisticLock = 2
      )

      val exclusions = PbikExclusions(
        currentEmployerOptimisticLock = 10,
        getPBIKExclusionList = Some(List(person1, person2))
      )

      val json   = Json.toJson(exclusions)
      val parsed = json.as[PbikExclusions]

      parsed mustBe exclusions
    }

    "fail to deserialize when 'currentEmployerOptimisticLock' is missing" in {
      val json = Json.parse(
        """{
          | "getPBIKExclusionList": []
          |}""".stripMargin)
      val result = Json.fromJson[PbikExclusions](json)
      result.isError mustBe true
    }

    "fail to deserialize when 'currentEmployerOptimisticLock' is null" in {
      val json = Json.parse(
        """{
          | "currentEmployerOptimisticLock": null,
          | "getPBIKExclusionList": []
          |}""".stripMargin)
      val result = Json.fromJson[PbikExclusions](json)
      result.isError mustBe true
    }

    "fail to deserialize when JSON is empty" in {
      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[PbikExclusions](emptyJson)
      result.isError mustBe true
    }
  }
}
