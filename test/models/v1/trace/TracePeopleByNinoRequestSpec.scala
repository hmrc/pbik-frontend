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

package models.v1.trace

import base.FakePBIKApplication
import models.v1.IabdType
import play.api.libs.json.{JsValue, Json}

class TracePeopleByNinoRequestSpec extends FakePBIKApplication {

  private val request = TracePeopleByNinoRequest(
    iabdType = IabdType.CarBenefit,
    firstForename = "John",
    surname = "Doe",
    nationalInsuranceNumber = "AB123456C"
  )

  "TracePeopleByNinoRequest" when {
    ".writes/reads" must {

      "serialize and deserialize correctly" in {
        val json         = Json.toJson(request)
        val deserialized = json.as[TracePeopleByNinoRequest]

        deserialized mustBe request
      }

      "deserialize JSON into TracePeopleByNinoRequest" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "iabdType": "Car Benefit",
            |  "firstForename": "John",
            |  "surname": "Doe",
            |  "nationalInsuranceNumber": "AB123456C"
            |}
            |""".stripMargin
        )

        val result = Json.fromJson[TracePeopleByNinoRequest](json)
        result.asEither match {
          case Right(r) =>
            r.iabdType mustBe IabdType.CarBenefit
            r.firstForename mustBe "John"
            r.surname mustBe "Doe"
            r.nationalInsuranceNumber mustBe "AB123456C"
          case Left(errors) =>
            fail(s"JSON parse error: $errors")
        }
      }

      "fail to deserialize when iabdType is missing" in {
        val invalidJson =
          """
            |{
            |  "firstForename": "John",
            |  "surname": "Doe",
            |  "nationalInsuranceNumber": "AB123456C"
            |}
            |""".stripMargin

        val result = Json.fromJson[TracePeopleByNinoRequest](Json.parse(invalidJson))

        result.isError mustBe true
      }

      "fail to deserialize when JSON is empty" in {
        val emptyJson = Json.parse("{}")

        val result = Json.fromJson[TracePeopleByNinoRequest](emptyJson)
        result.isError mustBe true
      }
    }
  }
}