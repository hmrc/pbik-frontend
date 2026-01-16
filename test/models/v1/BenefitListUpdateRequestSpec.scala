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
import play.api.libs.json.Json

class BenefitListUpdateRequestSpec extends FakePBIKApplication {

  "BenefitListUpdateRequest" should {
    "serialize to JSON and deserialize back correctly" in {
      val original = BenefitListUpdateRequest(
        pbikRegistrationUpdate = List(
          BenefitInKindRequest(
            iabdType = IabdType.CarBenefit,
            payrolledBenefitInKindAction = PbikAction.NoAction,
            isAgentSubmission = false
          )
        ),
        employerOptimisticLockRequest = EmployerOptimisticLockRequest(currentEmployerOptimisticLock = 1)
      )

      val json         = Json.toJson(original)
      val parsed       = Json.parse(json.toString)
      val deserialized = parsed.as[BenefitListUpdateRequest]

      deserialized mustBe original
    }

    "fail to deserialize when pbikRegistrationUpdate is missing" in {
      val invalidJson =
        """
          |{
          |  "employerOptimisticLockRequest": { "someField": "value" }
          |}
          |""".stripMargin

      val result = Json.fromJson[BenefitListUpdateRequest](Json.parse(invalidJson))

      result.isError mustBe true
    }

    "fail to deserialize when JSON is empty" in {
      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[BenefitListUpdateRequest](emptyJson)
      result.isError mustBe true
    }
  }
}
