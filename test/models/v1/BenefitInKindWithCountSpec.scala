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
import play.api.libs.json.{JsObject, Json}

class BenefitInKindWithCountSpec extends FakePBIKApplication {

  "BenefitInKindWithCount" when {

    "reads" must {
      "serialize BenefitInKindWithCount without agent flag" in {
        val bik: BenefitInKindWithCount = BenefitInKindWithCount(
          IabdType.CarBenefit,
          2
        )
        val json                        = Json.toJson(bik).as[JsObject] ++ Json.obj("isAgentSubmission" -> false)
        val deserialized                = BenefitInKindWithCount.reads.reads(json).get

        deserialized mustBe bik
      }

      "serialize BenefitInKindWithCount with agent flag" in {
        val bik: BenefitInKindWithCount = BenefitInKindWithCount(
          IabdType.CarBenefit,
          3
        )
        val json                        = Json.toJson(bik).as[JsObject]
        val deserialized                = BenefitInKindWithCount.reads.reads(json).get

        deserialized mustBe bik
      }

      "fail to deserialize when iabdType is missing" in {
        val invalidJson =
          """
            |{
            |  "payrolledBenefitInKindExclusionCount": 5
            |}
            |""".stripMargin

        val result = Json.fromJson[BenefitInKindWithCount](Json.parse(invalidJson))

        result.isError mustBe true
      }

      "fail to deserialize when JSON is empty" in {
        val emptyJson = Json.parse("{}")

        val result = Json.fromJson[BenefitInKindWithCount](emptyJson)
        result.isError mustBe true
      }
    }

  }

}
