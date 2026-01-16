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

package models.v1.exclusion

import base.FakePBIKApplication
import play.api.libs.json.Json

class PbikExclusionPersonSpec extends FakePBIKApplication {

  "PbikExclusionPerson" when {

    "return same hashCode for same NINO regardless of other fields" in {
      val p1 = PbikExclusionPerson("AA123456A", "John", Some("X"), "Smith", Some("123"), 1)
      val p2 = PbikExclusionPerson("AA123456A", "Jane", None, "Doe", None, 99)

      p1.hashCode mustBe p2.hashCode
    }

    "return different hashCode for different NINO" in {
      val p1 = PbikExclusionPerson("AA123456A", "John", None, "Smith", None, 1)
      val p2 = PbikExclusionPerson("BB123456B", "John", None, "Smith", None, 1)

      p1.hashCode must not be p2.hashCode
    }

    "be equal if NINOs match, even if other fields differ" in {
      val p1 = PbikExclusionPerson("AA123456A", "John", None, "Smith", None, 1)
      val p2 = PbikExclusionPerson("AA123456A", "Jane", Some("Middle"), "Doe", Some("123"), 99)

      p1 mustBe p2
    }

    "not be equal if NINOs differ" in {
      val p1 = PbikExclusionPerson("AA123456A", "John", None, "Smith", None, 1)
      val p2 = PbikExclusionPerson("BB123456B", "John", None, "Smith", None, 1)

      p1 must not equal p2
    }

    "construct fullName from firstForename and surname" in {
      val p = PbikExclusionPerson("AA123456A", "John", None, "Smith", None, 1)
      p.fullName mustBe "John Smith"
    }

    "serialise and deserialise to/from JSON" in {
      val original = PbikExclusionPerson("AA123456A", "John", Some("Middle"), "Smith", Some("123"), 1)
      val json     = Json.toJson(original)
      val parsed   = json.as[PbikExclusionPerson]

      parsed mustBe original
    }

    "fail to deserialize when nationalInsuranceNumber is missing" in {
      val invalidJson =
        """
          |{
          |  "firstForename": "John",
          |  "surname": "Doe",
          |  "optimisticLock": 1
          |}
          |""".stripMargin

      val result = Json.fromJson[PbikExclusionPerson](Json.parse(invalidJson))

      result.isError mustBe true
    }

    "fail to deserialize when JSON is empty" in {
      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[PbikExclusionPerson](emptyJson)
      result.isError mustBe true
    }
  }

}
