/*
 * Copyright 2024 HM Revenue & Customs
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

class BenefitListResponseSpec extends FakePBIKApplication {

  private val (currentOptimisticLock, payrolledBenefitInKindExclusionCount): (Int, Int) = (99, 9)

  private val bik: BenefitInKindWithCount = BenefitInKindWithCount(
    IabdType.CarBenefit,
    payrolledBenefitInKindExclusionCount
  )

  "BenefitInKindWithCount" when {
    ".writes/reads" must {
      "serialize and deserialize BenefitListResponse without benefit list" in {
        val response: BenefitListResponse = BenefitListResponse(
          None,
          currentOptimisticLock
        )
        val json                          = BenefitListResponse.writes.writes(response)
        val deserialized                  = BenefitListResponse.reads.reads(json).get

        deserialized mustBe response
      }

      "serialize and deserialize BenefitListResponse with benefit list empty" in {
        val response: BenefitListResponse = BenefitListResponse(
          Some(List()),
          currentOptimisticLock
        )
        val json                          = BenefitListResponse.writes.writes(response)
        val deserialized                  = BenefitListResponse.reads.reads(json).get

        deserialized mustBe response
      }

      "serialize and deserialize BenefitListResponse with benefit list" in {
        val response: BenefitListResponse = BenefitListResponse(
          Some(List(bik, bik)),
          currentOptimisticLock
        )
        val json                          = BenefitListResponse.writes.writes(response)
        val deserialized                  = BenefitListResponse.reads.reads(json).get

        deserialized mustBe response
      }
    }
  }
}
