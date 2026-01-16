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
import play.api.libs.json.{JsValue, Json}

class TracePersonListResponseSpec extends FakePBIKApplication {

  private val person1 = TracePersonResponse(
    nationalInsuranceNumber = "AB123456C",
    firstForename = "John",
    secondForename = Some("Michael"),
    surname = "Doe",
    worksPayrollNumber = Some("12345"),
    optimisticLock = 1
  )

  private val person2 = TracePersonResponse(
    nationalInsuranceNumber = "CD789012E",
    firstForename = "Jane",
    secondForename = None,
    surname = "Smith",
    worksPayrollNumber = None,
    optimisticLock = 2
  )

  private val response = TracePersonListResponse(
    updatedEmployerOptimisticLock = 10,
    pbikExclusionList = List(person1, person2)
  )

  "TracePersonListResponse" when {
    ".writes/reads" must {

      "serialize and deserialize correctly" in {
        val json         = Json.toJson(response)
        val deserialized = json.as[TracePersonListResponse]

        deserialized mustBe response
      }

      "deserialize JSON into TracePersonListResponse" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "updatedEmployerOptimisticLock": 10,
            |  "pbikExclusionList": [
            |    {
            |      "nationalInsuranceNumber": "AB123456C",
            |      "firstForename": "John",
            |      "secondForename": "Michael",
            |      "surname": "Doe",
            |      "worksPayrollNumber": "12345",
            |      "optimisticLock": 1
            |    },
            |    {
            |      "nationalInsuranceNumber": "CD789012E",
            |      "firstForename": "Jane",
            |      "secondForename": null,
            |      "surname": "Smith",
            |      "worksPayrollNumber": null,
            |      "optimisticLock": 2
            |    }
            |  ]
            |}
            |""".stripMargin
        )

        val result = Json.fromJson[TracePersonListResponse](json)
        result.asEither match {
          case Right(r)     =>
            r.updatedEmployerOptimisticLock mustBe 10
            r.pbikExclusionList.size mustBe 2
            r.pbikExclusionList.head.nationalInsuranceNumber mustBe "AB123456C"
            r.pbikExclusionList(1).firstForename mustBe "Jane"
          case Left(errors) =>
            fail(s"JSON parse error: $errors")
        }
      }

      "fail to deserialize when updatedEmployerOptimisticLock is missing" in {
        val invalidJson =
          """
            |{
            |  "pbikExclusionList": []
            |}
            |""".stripMargin

        val result = Json.fromJson[TracePersonListResponse](Json.parse(invalidJson))

        result.isError mustBe true
      }

      "fail to deserialize when JSON is empty" in {
        val emptyJson = Json.parse("{}")

        val result = Json.fromJson[TracePersonListResponse](emptyJson)
        result.isError mustBe true
      }
    }
  }
}
