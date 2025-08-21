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
import models.v1.IabdType
import play.api.libs.json.{JsValue, Json}

class UpdateExclusionPersonForABenefitRequestSpec extends FakePBIKApplication {

  private val addRequest: PbikExclusionPersonAddRequest = PbikExclusionPersonAddRequest(
    iabdType = IabdType.CarBenefit,
    nationalInsuranceNumber = "AB123456C",
    firstForename = "John",
    surname = "Doe",
    optimisticLock = 1
  )

  "UpdateExclusionPersonForABenefitRequest" when {
    ".writes/reads" must {

      "serialize and deserialize correctly" in {
        val request = UpdateExclusionPersonForABenefitRequest(
          currentEmployerOptimisticLock = 42,
          postPBIKExclusionDetails = addRequest
        )

        val json         = Json.toJson(request)
        val deserialized = json.as[UpdateExclusionPersonForABenefitRequest]

        deserialized mustBe request
      }

      "deserialize JSON into UpdateExclusionPersonForABenefitRequest" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "currentEmployerOptimisticLock": 42,
            |  "postPBIKExclusionDetails": {
            |    "iabdType": "Car Benefit",
            |    "nationalInsuranceNumber": "AB123456C",
            |    "firstForename": "John",
            |    "surname": "Doe",
            |    "optimisticLock": 1
            |  }
            |}
            |""".stripMargin
        )

        val result = Json.fromJson[UpdateExclusionPersonForABenefitRequest](json)
        result.asEither match {
          case Right(req) =>
            req.currentEmployerOptimisticLock mustBe 42
            req.postPBIKExclusionDetails.nationalInsuranceNumber mustBe "AB123456C"
            req.postPBIKExclusionDetails.firstForename mustBe "John"
            req.postPBIKExclusionDetails.surname mustBe "Doe"
            req.postPBIKExclusionDetails.optimisticLock mustBe 1
          case Left(errors) =>
            fail(s"JSON parse error: $errors")
        }
      }

      "fail to deserialize when postPBIKExclusionDetails is missing" in {
        val invalidJson =
          """
            |{
            |  "currentEmployerOptimisticLock": 1
            |}
            |""".stripMargin

        val result = Json.fromJson[UpdateExclusionPersonForABenefitRequest](Json.parse(invalidJson))

        result.isError mustBe true
      }

      "fail to deserialize when JSON is empty" in {
        val emptyJson = Json.parse("{}")

        val result = Json.fromJson[UpdateExclusionPersonForABenefitRequest](emptyJson)
        result.isError mustBe true
      }
    }
  }
}