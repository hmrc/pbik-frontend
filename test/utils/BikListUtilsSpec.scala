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

import controllers.FakePBIKApplication
import models.Bik
import models.v1.IabdType._
import models.v1.{IabdType, PbikAction, PbikStatus}
import org.scalatestplus.play.PlaySpec
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

class BikListUtilsSpec extends PlaySpec with FakePBIKApplication {

  val bikListUtils: BikListUtils                            = app.injector.instanceOf[BikListUtils]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val biks: List[Bik]                                       = IabdType.values.toList
    .filter(x => x.id != IabdType.CarFuelBenefit.id)
    .map(x => Bik(x.id.toString, PbikStatus.ValidPayrollingBenefitInKind.id))
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
  ).map(x => x.id)
  private val registered: List[Bik]                         =
    List(AssetTransfer, PaymentsOnEmployeeBehalf, VouchersAndCreditCards, VanBenefit, Mileage).map(x =>
      Bik(x.id.toString, PbikAction.ReinstatePayrolledBenefitInKind.id)
    )
  private val modifications: List[Bik]                      = List(
    Bik(AssetTransfer.id.toString, PbikAction.RemovePayrolledBenefitInKind.id),
    Bik(PaymentsOnEmployeeBehalf.id.toString, PbikAction.RemovePayrolledBenefitInKind.id),
    Bik(VouchersAndCreditCards.id.toString, PbikAction.RemovePayrolledBenefitInKind.id),
    Bik(NonQualifyingRelocationExpenses.id.toString, PbikAction.RemovePayrolledBenefitInKind.id),
    Bik(OtherItems.id.toString, PbikAction.ReinstatePayrolledBenefitInKind.id),
    Bik(IncomeTaxPaidButNotDeductedFromDirectorRemuneration.id.toString, PbikAction.ReinstatePayrolledBenefitInKind.id)
  )
  private val normaliseResult: List[Int]                    = List(
    AssetTransfer.id,
    OtherItems.id,
    PaymentsOnEmployeeBehalf.id,
    IncomeTaxPaidButNotDeductedFromDirectorRemuneration.id,
    VouchersAndCreditCards.id
  ).sorted

  "The Biks, when sorted Alphabetically according to labels" should {
    "result in the correct order" in {
      assert(bikListUtils.sortAlphabeticallyByLabels(biks).map(x => x.iabdType.toInt) == alphaSorted)
    }
  }

  "The Biks, when sorted Alphabetically according to labels" should {
    "be the same size as the original list" in {
      assert(bikListUtils.sortAlphabeticallyByLabels(biks).size == biks.size)
    }
  }

  "The Registration Items, when sorted Alphabetically according to labels" should {
    "result in the correct order" in {
      assert(
        bikListUtils
          .sortRegistrationsAlphabeticallyByLabels(bikListUtils.mergeSelected(biks, biks))
          .active
          .map(x => x.id.toInt) == alphaSorted
      )
    }
  }

  "The Registration Items, when sorted Alphabetically according to labels" should {
    "be the same size as the original list" in {
      assert(
        bikListUtils
          .sortRegistrationsAlphabeticallyByLabels(bikListUtils.mergeSelected(biks, biks))
          .active
          .size == biks.size
      )
    }
  }

  "When normalising additions and removals from a registered list of Biks, the remainder" should {
    "result in the correct values" in {
      val ubinNormed =
        bikListUtils.normaliseSelectedBenefits(registered, modifications).map(x => x.iabdType.toInt).sorted
      assert(ubinNormed == normaliseResult)
    }
  }

  "When normalising additions and removals from a registered list of Biks, the remainder" should {
    "not contain duplicates" in {
      val normalised = bikListUtils.normaliseSelectedBenefits(registered, modifications)
      assert(normalised.size == normalised.distinct.size)
    }
  }

  "When removing matches is supplied the same list for the initial & checked lists, the remainder" should {
    "should be empty" in {
      val shouldBeEmpty = bikListUtils.removeMatches(registered, registered)
      assert(shouldBeEmpty.active.isEmpty)
    }
  }

  "When removing matches the head element, the remainder" should {
    "should be the tail" in {
      val expectedTail = bikListUtils.removeMatches(biks, List()).active.tail // manually remove an element
      val shouldBeTail =
        bikListUtils.removeMatches(biks, List(biks.head)).active // check the function does the same
      assert(shouldBeTail == expectedTail)
    }
  }

  "When merging the same list, the size of the matches" should {
    "equal the size of the list" in {
      assert(bikListUtils.mergeSelected(biks, biks).active.size == biks.size)
    }
  }

  "When merging a large list, with a subset of that list, the size of the merge results" should {
    "equal the size of the superset" in {
      assert(bikListUtils.mergeSelected(biks, registered).active.size == biks.size)
    }
  }

  "When merging an original list, with an unconnected list, the size of the merge results" should {
    "equal the size of the original list as the unconnected elements wont be added" in {
      val iabdType1       = 100000
      val iabdType2       = 100001
      val status          = 40
      val unconnectedList = List(Bik("" + iabdType1, status), Bik("" + iabdType2, status))
      assert(bikListUtils.mergeSelected(biks, unconnectedList).active.size == biks.size)
    }
  }

  "When merging selected lists  all of the results" should {
    "have their active flags set as false" in {
      assert(bikListUtils.mergeSelected(biks, biks).active.map(x => x.active).count(identity) == biks.size)
    }
  }

  "When removing two identical lists , the size of the merge results" should {
    "equal zero" in {
      assert(bikListUtils.removeMatches(biks, biks).active.isEmpty)
    }
  }

  "When removing lists where one list has different elements the size" should {
    "equal the size of the different elements" in {
      assert(bikListUtils.removeMatches(biks, biks).active.isEmpty)
    }
  }

  "When removing lists where both list have unique elements the size" should {
    "of the result should equal the total number of differences" in {
      assert(bikListUtils.removeMatches(biks, registered).active.size == biks.size - registered.size)
    }
  }

}
