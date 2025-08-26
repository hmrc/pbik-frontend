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
import play.api.libs.json.{JsValue, Json}

class TracePersonResponseSpec extends FakePBIKApplication {

  private val person1 = TracePersonResponse(
    nationalInsuranceNumber = "AB123456C",
    firstForename = "John",
    secondForename = Some("Michael"),
    surname = "Doe",
    worksPayrollNumber = Some("12345"),
    optimisticLock = 1
  )

  private val person2 = TracePersonResponse(
    nationalInsuranceNumber = "AB123456C",
    firstForename = "Jane",
    secondForename = None,
    surname = "Smith",
    worksPayrollNumber = None,
    optimisticLock = 2
  )

  "TracePersonResponse" when {
    ".writes/reads" must {

      "serialize and deserialize correctly" in {
        val json         = Json.toJson(person1)
        val deserialized = json.as[TracePersonResponse]

        deserialized mustBe person1
      }

      "deserialize JSON into TracePersonResponse" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "nationalInsuranceNumber": "AB123456C",
            |  "firstForename": "John",
            |  "secondForename": "Michael",
            |  "surname": "Doe",
            |  "worksPayrollNumber": "12345",
            |  "optimisticLock": 1
            |}
            |""".stripMargin
        )

        val result = Json.fromJson[TracePersonResponse](json)
        result.asEither match {
          case Right(p)     =>
            p.nationalInsuranceNumber mustBe "AB123456C"
            p.firstForename mustBe "John"
            p.secondForename mustBe Some("Michael")
            p.surname mustBe "Doe"
            p.worksPayrollNumber mustBe Some("12345")
            p.optimisticLock mustBe 1
          case Left(errors) =>
            fail(s"JSON parse error: $errors")
        }
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

        val result = Json.fromJson[TracePersonResponse](Json.parse(invalidJson))

        result.isError mustBe true
      }

      "fail to deserialize when JSON is empty" in {
        val emptyJson = Json.parse("{}")

        val result = Json.fromJson[TracePersonResponse](emptyJson)
        result.isError mustBe true
      }
    }

    ".equals and .hashCode" must {
      "consider two instances equal if nationalInsuranceNumber matches" in {
        person1 mustBe person2
        person1.hashCode mustBe person2.hashCode
      }

      "consider two instances not equal if nationalInsuranceNumber differs" in {
        val differentPerson = person1.copy(nationalInsuranceNumber = "ZZ987654D")
        differentPerson must not be person1
      }
    }

    ".fullName and .getWorksPayrollNumber" must {
      "return correct fullName" in {
        person1.fullName mustBe "John Michael Doe"
        val personNoSecond = person1.copy(secondForename = None)
        personNoSecond.fullName mustBe "John  Doe" // boşluk iki isim arasında kaldı
      }

      "return correct worksPayrollNumber" in {
        person1.getWorksPayrollNumber mustBe "12345"
        val unknownPayroll = person1.copy(worksPayrollNumber = None)
        unknownPayroll.getWorksPayrollNumber mustBe "UNKNOWN"
      }
    }
  }
}
