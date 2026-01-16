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

package models.v1

import base.FakePBIKApplication
import play.api.libs.json.{JsValue, Json}

class BenefitInKindRequestSpec extends FakePBIKApplication {

  private val request = BenefitInKindRequest(
    iabdType = models.v1.IabdType.CarBenefit,
    payrolledBenefitInKindAction = PbikAction.NoAction,
    isAgentSubmission = false
  )

  "BenefitInKindRequest" when {
    ".writes/reads" must {

      "serialize and deserialize correctly" in {
        val json         = Json.toJson(request)
        val deserialized = json.as[BenefitInKindRequest]

        deserialized mustBe request
      }

      "deserialize JSON into BenefitInKindRequest" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "iabdType": "Car Benefit",
            |  "payrolledBenefitInKindAction": "No Action",
            |  "isAgentSubmission": false
            |}
            |""".stripMargin
        )

        val result = Json.fromJson[BenefitInKindRequest](json)
        result.asEither match {
          case Right(r)     =>
            r.iabdType.toString mustBe "Car Benefit"
            r.payrolledBenefitInKindAction.toString mustBe "No Action"
            r.isAgentSubmission mustBe false
          case Left(errors) =>
            fail(s"JSON parse error: $errors")
        }
      }

      "fail to deserialize when iabdType is missing" in {
        val invalidJson =
          """
            |{
            |  "payrolledBenefitInKindAction": { "someField": "value" },
            |  "isAgentSubmission": true
            |}
            |""".stripMargin

        val result = Json.fromJson[BenefitInKindRequest](Json.parse(invalidJson))

        result.isError mustBe true
      }

      "fail to deserialize when JSON is empty" in {
        val emptyJson = Json.parse("{}")

        val result = Json.fromJson[BenefitInKindRequest](emptyJson)
        result.isError mustBe true
      }
    }
  }
}
