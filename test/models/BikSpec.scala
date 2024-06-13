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
import org.scalatestplus.play.PlaySpec
import utils.Exceptions.InvalidBikTypeException

class BikSpec extends PlaySpec {

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

      "return false if Bik equals wrong object" in {
        val bik: Bik = Bik(IabdType.MedicalInsurance.id.toString, PbikStatus.ValidPayrollingBenefitInKind.id, 1)

        //noinspection ComparingUnrelatedTypes
        bik.equals(BigDecimal("123")) mustBe false
      }
    }

    ".hashCode" must {
      "return a hash integer for the iabdType rather than the Bik instance" in {
        val iabd: String       = IabdType.MedicalInsurance.id.toString
        val generatedHash: Int = iabd.hashCode
        val bik: Bik           = Bik(iabd, PbikAction.NoAction.id, 2)

        bik.hashCode mustBe generatedHash
      }
    }

    ".asBenefitString" must {
      "return the mapped benefit as string when iabdType passed is mapped" in {
        val iabdType: String = IabdType.CarBenefit.id.toString

        Bik.asBenefitString(iabdType) mustBe "car"
      }

      "return InvalidBikTypeException when iabdType passed is not mapped" in {
        intercept[InvalidBikTypeException] {
          Bik.asBenefitString("90")
        }
      }
    }

    ".asNPSTypeValue" must {
      "return the mapped NPS type value when iabdString passed is mapped" in {
        val iabdString: String = "mileage"

        Bik.asNPSTypeValue(iabdString) mustBe IabdType.Mileage.id.toString
      }

      "return InvalidBikTypeException when iabdString passed is not mapped" in {
        intercept[InvalidBikTypeException] {
          Bik.asNPSTypeValue("bus")
        }
      }
    }
  }
}
