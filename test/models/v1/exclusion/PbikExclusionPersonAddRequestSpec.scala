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
import models.v1.IabdType
import play.api.libs.json.Json

class PbikExclusionPersonAddRequestSpec extends FakePBIKApplication {

  "PbikExclusionPersonAddRequest" should {

    "serialize and deserialize correctly with all fields populated" in {
      val request = PbikExclusionPersonAddRequest(
        iabdType = IabdType.VanBenefit,
        nationalInsuranceNumber = "AB123456C",
        firstForename = "John",
        surname = "Doe",
        optimisticLock = 1
      )

      val json   = Json.toJson(request)
      val parsed = json.as[PbikExclusionPersonAddRequest]

      parsed mustBe request
    }

    "serialize and deserialize correctly with only required fields" in {
      val request = PbikExclusionPersonAddRequest(
        iabdType = IabdType.VanBenefit,
        nationalInsuranceNumber = "AB123456C",
        firstForename = "Jane",
        surname = "Smith",
        optimisticLock = 2
      )

      val json   = Json.toJson(request)
      val parsed = json.as[PbikExclusionPersonAddRequest]

      parsed mustBe request
    }

    "fail to deserialize when required fields are missing" in {
      val jsonMissingFields = Json.parse(
        """
          |{
          |  "iabdType": "VanBenefit",
          |  "nationalInsuranceNumber": "AB123456C",
          |  "firstForename": "John"
          |}
          |""".stripMargin
      )

      val result = Json.fromJson[PbikExclusionPersonAddRequest](jsonMissingFields)
      result.isError mustBe true
    }

    "fail to deserialize when fields have incorrect types" in {
      val jsonInvalidTypes = Json.parse(
        """
          |{
          |  "iabdType": 123,
          |  "nationalInsuranceNumber": "AB123456C",
          |  "firstForename": "John",
          |  "surname": "Doe",
          |  "optimisticLock": "one"
          |}
          |""".stripMargin
      )

      val result = Json.fromJson[PbikExclusionPersonAddRequest](jsonInvalidTypes)
      result.isError mustBe true
    }

    "fail to deserialize when iabdType is missing" in {
      val invalidJson =
        """
          |{
          |  "nationalInsuranceNumber": "AB123456C",
          |  "firstForename": "John",
          |  "surname": "Doe",
          |  "optimisticLock": 1
          |}
          |""".stripMargin

      val result = Json.fromJson[PbikExclusionPersonAddRequest](Json.parse(invalidJson))

      result.isError mustBe true
    }

    "fail to deserialize empty JSON" in {
      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[PbikExclusionPersonAddRequest](emptyJson)
      result.isError mustBe true
    }
  }
}
