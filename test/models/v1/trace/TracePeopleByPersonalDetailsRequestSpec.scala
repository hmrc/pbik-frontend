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

package models.v1.trace

import base.FakePBIKApplication
import models.v1.IabdType
import models.v1.exclusion.Gender
import play.api.libs.json.{JsValue, Json}

class TracePeopleByPersonalDetailsRequestSpec extends FakePBIKApplication {

  private val request = TracePeopleByPersonalDetailsRequest(
    iabdType = IabdType.CarBenefit,
    firstForename = Some("John"),
    surname = "Doe",
    dateOfBirth = "1990-01-01",
    gender = Gender.Male
  )

  "TracePeopleByPersonalDetailsRequest" when {
    ".writes/reads" must {

      "serialize and deserialize correctly" in {
        val json         = Json.toJson(request)
        val deserialized = json.as[TracePeopleByPersonalDetailsRequest]

        deserialized mustBe request
      }

      "deserialize JSON into TracePeopleByPersonalDetailsRequest" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "iabdType": "Car Benefit",
            |  "firstForename": "John",
            |  "surname": "Doe",
            |  "dateOfBirth": "1990-01-01",
            |  "gender": "MALE"
            |}
            |""".stripMargin
        )

        val result = Json.fromJson[TracePeopleByPersonalDetailsRequest](json)
        result.asEither match {
          case Right(r)     =>
            r.iabdType mustBe IabdType.CarBenefit
            r.firstForename mustBe Some("John")
            r.surname mustBe "Doe"
            r.dateOfBirth mustBe "1990-01-01"
            r.gender mustBe Gender.Male
          case Left(errors) =>
            fail(s"JSON parse error: $errors")
        }
      }

      "fail to deserialize when surname is missing" in {
        val invalidJson =
          """
            |{
            |  "iabdType": { "someField": "value" },
            |  "dateOfBirth": "1980-01-01",
            |  "gender": "Male"
            |}
            |""".stripMargin

        val result = Json.fromJson[TracePeopleByPersonalDetailsRequest](Json.parse(invalidJson))

        result.isError mustBe true
      }

      "fail to deserialize when JSON is empty" in {
        val emptyJson = Json.parse("{}")

        val result = Json.fromJson[TracePeopleByPersonalDetailsRequest](emptyJson)
        result.isError mustBe true
      }
    }
  }
}
