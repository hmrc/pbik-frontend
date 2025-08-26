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

package models.bindable

import base.FakePBIKApplication
import models.bindable.Bindables._
import models.v1.IabdType
import org.scalatest.Inspectors.forAll

class BindablesSpec extends FakePBIKApplication {

  "Bindables" when {
    ".pathBinder" should {
      forAll(IabdType.values.toSeq) { iabdType =>
        s"bind and unbind IabdType '${iabdType.toString}'" in {
          val result = pathBinder.bind("key", iabdType.id.toString)
          result mustBe Right(iabdType)
        }
      }

      "fail to bind an invalid IabdType" in {
        val invalidId = "invalid"
        val result    = pathBinder.bind("key", invalidId)
        result mustBe Left(s"Invalid IabdType $invalidId")
      }

      forAll(IabdType.values.toSeq) { iabdType =>
        s"unbind an IabdType '${iabdType.toString}' to its id '${iabdType.id}'" in {
          val result = pathBinder.unbind("key", iabdType)
          result mustBe iabdType.id.toString
        }
      }
    }
  }
}
