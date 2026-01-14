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

package models

import base.FakePBIKApplication
import models.form.BinaryRadioButtonWithDesc
import models.v1.IabdType
import play.api.libs.json.{JsValue, Json}

import java.time.Instant

class PbikSessionSpec extends FakePBIKApplication {

  private val item1: RegistrationItem = RegistrationItem(IabdType.CarBenefit, active = true, enabled = false)
  private val item2: RegistrationItem = RegistrationItem(IabdType.MedicalInsurance, active = false, enabled = true)

  private val reason: BinaryRadioButtonWithDesc =
    BinaryRadioButtonWithDesc(selectionValue = "no", info = Some("Test reason"))

  private val registrations: RegistrationList =
    RegistrationList(selectAll = Some("all"), active = List(item1, item2), reason = Some(reason))

  private val fixedInstant: Instant = Instant.parse("2025-08-18T12:00:00Z")

  "PbikSession" when {
    ".writes/reads" must {

      "serialize and deserialize a PbikSession" in {
        val session = PbikSession(
          sessionId = "12345",
          registrations = Some(registrations),
          bikRemoved = Some(item1),
          lastUpdated = fixedInstant
        )

        val json         = Json.toJson(session)
        val deserialized = Json.fromJson[PbikSession](json).get

        deserialized mustBe session
      }

      "serialize and deserialize a PbikSession with minimal fields" in {
        val session = PbikSession(
          sessionId = "67890",
          lastUpdated = fixedInstant
        )

        val json         = Json.toJson(session)
        val deserialized = Json.fromJson[PbikSession](json).get

        deserialized mustBe session
      }

      "serialize to expected JSON structure" in {
        val session = PbikSession(
          sessionId = "12345",
          registrations = Some(registrations),
          lastUpdated = fixedInstant
        )

        val json = Json.toJson(session)

        (json \ "sessionId").as[String] mustBe "12345"
        (json \ "registrations" \ "selectAll").as[String] mustBe "all"
        (json \ "registrations" \ "active").as[List[JsValue]].size mustBe 2
        (json \ "registrations" \ "reason" \ "selectionValue").as[String] mustBe "no"
        (json \ "registrations" \ "reason" \ "info").as[String] mustBe "Test reason"
      }

      "deserialize JSON into PbikSession" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "sessionId": "12345",
            |  "registrations": {
            |    "selectAll": "all",
            |    "active": [
            |      { "iabdType": "Car Benefit", "active": true, "enabled": false },
            |      { "iabdType": "Medical Insurance", "active": false, "enabled": true }
            |    ],
            |    "reason": { "selectionValue": "no", "info": "Test reason" }
            |  },
            |  "lastUpdated": { "$date": { "$numberLong": "1692362876570" } }
            |}
            |""".stripMargin
        )

        val result = Json.fromJson[PbikSession](json)
        result.asEither match {
          case Right(session) =>
            session.sessionId mustBe "12345"
            session.registrations.map(_.active.size) mustBe Some(2)
            session.registrations.flatMap(_.reason.map(_.selectionValue)) mustBe Some("no")
          case Left(errors)   =>
            fail(s"JSON parse error: $errors")
        }
      }

      "fail to deserialize when sessionId is empty" in {
        val invalidJson =
          """
            |{
            |  "registrations": null
            |}
            |""".stripMargin

        val result = Json.fromJson[PbikSession](Json.parse(invalidJson))

        result.isError mustBe true
      }

      "fail to deserialize when JSON is empty" in {
        val emptyJson = Json.parse("{}")

        val result = Json.fromJson[PbikSession](emptyJson)
        result.isError mustBe true
      }

    }
  }
}
