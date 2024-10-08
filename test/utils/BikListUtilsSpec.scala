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

package utils

import base.FakePBIKApplication
import models.RegistrationItem
import models.v1.IabdType._
import models.v1.{BenefitInKindWithCount, IabdType}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

class BikListUtilsSpec extends FakePBIKApplication {

  val bikListUtils: BikListUtils                            = injected[BikListUtils]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val iabds: Set[IabdType]                                  = IabdType.values
    .filter(x => x.id != IabdType.CarFuelBenefit.id)
  val biks: List[BenefitInKindWithCount]                    = iabds
    .filter(x => x.id != IabdType.CarFuelBenefit.id)
    .map(x => BenefitInKindWithCount(x, 1))
    .toList
  val bikIabds: Set[IabdType]                               = biks.map(x => x.iabdType).toSet
  private val alphaSorted                                   = List(
    Assets,
    AssetTransfer,
    CarBenefit,
    Entertaining,
    Expenses,
    IncomeTaxPaidButNotDeductedFromDirectorRemuneration,
    Mileage,
    NonQualifyingRelocationExpenses,
    OtherItems,
    Telephone,
    PaymentsOnEmployeeBehalf,
    MedicalInsurance,
    QualifyingRelocationExpenses,
    EmployerProvidedServices,
    TravelAndSubsistence,
    VanFuelBenefit,
    VanBenefit,
    VouchersAndCreditCards
  )
  private val registeredIabs                                = Set(AssetTransfer, PaymentsOnEmployeeBehalf, VouchersAndCreditCards, VanBenefit, Mileage)
  private val registered: Set[BenefitInKindWithCount]       =
    registeredIabs.map(x => BenefitInKindWithCount(x, 1))

  "The Biks, when sorted Alphabetically according to labels" should {
    "result in the correct order" in {
      bikListUtils
        .sortAlphabeticallyByLabels(biks)
        .map(x => x.iabdType) must contain theSameElementsInOrderAs alphaSorted
    }
  }

  "The Biks, when sorted Alphabetically according to labels" should {
    "be the same size as the original list" in {
      bikListUtils.sortAlphabeticallyByLabels(biks).size mustBe biks.size
    }
  }

  "When removing matches is supplied the same list for the initial & checked lists, the remainder" should {
    "should be empty" in {
      val mustBeEmpty = bikListUtils.removeMatches(registeredIabs, registered.map(_.iabdType))
      mustBeEmpty.active must be(empty)
    }
  }

  "When removing matches the head element, the remainder" should {
    "should be the tail" in {
      val fullIabds = Set(IabdType.Assets, IabdType.CarFuelBenefit, IabdType.CarBenefit, IabdType.VanBenefit)
      val finalBiks = BenefitInKindWithCount(IabdType.Assets, 3)

      val expectedRegistrationList = fullIabds.tail.map { x =>
        RegistrationItem(x, active = false, enabled = true)
      }

      val mustBeTail = bikListUtils.removeMatches(fullIabds, Set(finalBiks).map(_.iabdType)).active

      expectedRegistrationList must contain allElementsOf mustBeTail
    }
  }

  "When removing two identical lists , the size of the merge results" should {
    "equal zero" in {
      bikListUtils.removeMatches(iabds, bikIabds).active must be(empty)
    }
  }

  "When removing lists where one list has different elements the size" should {
    "equal the size of the different elements" in {
      bikListUtils.removeMatches(iabds, bikIabds).active must be(empty)
    }
  }

  "When removing lists where both list have unique elements the size" should {
    "of the result should equal the total number of differences" in {
      val registeredIabds = registered.map(x => x.iabdType)
      bikListUtils.removeMatches(iabds, registeredIabds).active.size mustBe biks.size - registered.size
    }
  }

}
