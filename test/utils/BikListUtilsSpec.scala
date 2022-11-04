/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatestplus.play.PlaySpec
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

import scala.language.reflectiveCalls

class BikListUtilsSpec extends PlaySpec with FakePBIKApplication {

  val bikListUtils                                          = app.injector.instanceOf[BikListUtils]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  // scalastyle:off magic.number
  private val alphaSorted = List(39, 40, 31, 42, 43, 52, 37, 38, 44, 45, 47, 32, 48, 30, 50, 8, 53, 36, 35, 54)

  val biks = List(
    Bik("" + 40, 10),
    Bik("" + 48, 10),
    Bik("" + 54, 10),
    Bik("" + 38, 10),
    Bik("" + 44, 10),
    Bik("" + 31, 10),
    Bik("" + 35, 10),
    Bik("" + 36, 10),
    Bik("" + 37, 10),
    Bik("" + 30, 10),
    Bik("" + 50, 10),
    Bik("" + 8, 10),
    Bik("" + 39, 10),
    Bik("" + 47, 10),
    Bik("" + 52, 10),
    Bik("" + 53, 10),
    Bik("" + 42, 10),
    Bik("" + 43, 10),
    Bik("" + 32, 10),
    Bik("" + 45, 10)
  )

  private val registered                 =
    List(Bik("" + 40, 30), Bik("" + 48, 30), Bik("" + 54, 30), Bik("" + 38, 30), Bik("" + 44, 30))
  private val modifications              =
    List(Bik("" + 40, 40), Bik("" + 48, 40), Bik("" + 54, 40), Bik("" + 45, 40), Bik("" + 47, 30), Bik("" + 52, 30))
  private val normaliseResult: List[Int] = List(40, 47, 48, 52, 54).sorted
  // scalastyle:on magic.number

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
      val unconnectedList = List(Bik("" + 100000, 40), Bik("" + 100001, 40))
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
