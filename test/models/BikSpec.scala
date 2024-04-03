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

package models

import models.v1.{IabdType, PbikAction, PbikStatus}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BikSpec extends AnyWordSpec with Matchers {
  "Bik" when {
    ".equals" must {

      "return true if 2 Bik instances have the same iabdType" in {
        val bik: Bik          = Bik(IabdType.MedicalInsurance.id.toString, PbikStatus.ValidPayrollingBenefitInKind.id, 2)
        val bikToCompare: Bik =
          Bik(IabdType.MedicalInsurance.id.toString, PbikStatus.InvalidPayrollingBenefitInKind.id, 3)

        bik.equals(bikToCompare) mustBe true
      }

      "return false if 2 Bik instances have different iabdType" in {
        val bik: Bik          = Bik(IabdType.MedicalInsurance.id.toString, PbikStatus.ValidPayrollingBenefitInKind.id, 1)
        val bikToCompare: Bik = Bik(IabdType.Expenses.id.toString, PbikStatus.ValidPayrollingBenefitInKind.id, 1)

        bik.equals(bikToCompare) mustBe false
      }
    }

    ".hashCode" must {
      "return a hash integer for the iabdType rather than the Bik instance" in {
        val iabd               = IabdType.MedicalInsurance.id.toString
        val generatedHash: Int = iabd.hashCode
        val bik: Bik           = Bik(iabd, PbikAction.NoAction.id, 2)

        bik.hashCode mustBe generatedHash
      }
    }

    //TODO add tests for rest of BIK methods
  }
}
