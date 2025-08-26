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

package models.v1

import base.FakePBIKApplication
import play.api.libs.json.Json

class EmployerOptimisticLockRequestSpec extends FakePBIKApplication {

  "EmployerOptimisticLockRequest" should {
    "serialize to JSON and deserialize back correctly" in {
      val original = EmployerOptimisticLockRequest(42)

      val json = Json.toJson(original)
      val parsed = Json.parse(json.toString)
      val deserialized = parsed.as[EmployerOptimisticLockRequest]

      deserialized mustBe original
    }

    "fail to deserialize when keyword is wrong" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "ASD": { "someField": "value" }
          |}
          |""".stripMargin
      )

      val result = Json.fromJson[EmployerOptimisticLockRequest](invalidJson)
      result.isError mustBe true
    }

    "fail to deserialize when JSON is empty" in {
      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[EmployerOptimisticLockRequest](emptyJson)
      result.isError mustBe true
    }
  }
}