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

package models.agent

import base.FakePBIKApplication
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.EmpRef

import java.time.LocalDateTime

class ClientSpec extends FakePBIKApplication {

  "Client JSON serialization" should {

    "deserialize minimal Client JSON with EmpRef as string" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "empRef": "123/AB456",
          |  "accountsOfficeReference": {
          |    "districtNumber": "12",
          |    "payType": "A",
          |    "checkCode": "C",
          |    "reference": "123456"
          |  },
          |  "lpAuthorisation": true
          |}
          |""".stripMargin
      )

      val client = json.as[Client]

      client.empRef.value mustBe "123/AB456"
      client.accountsOfficeReference.districtNumber mustBe "12"
      client.lpAuthorisation mustBe true
      client.name mustBe None
      client.agentClientRef mustBe None
      client.requestedDeletionOn mustBe None
    }

    "deserialize full Client JSON with EmpRef as object" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "empRef": {
          |    "taxOfficeNumber": "123",
          |    "taxOfficeReference": "AB456"
          |  },
          |  "accountsOfficeReference": {
          |    "districtNumber": "12",
          |    "payType": "A",
          |    "checkCode": "C",
          |    "reference": "123456"
          |  },
          |  "name": "Test Client",
          |  "lpAuthorisation": true,
          |  "agentClientRef": "AG123",
          |  "requestedDeletionOn": "2025-08-18T20:47:56"
          |}
          |""".stripMargin
      )

      val client = json.as[Client]

      client.empRef.value mustBe "123/AB456"
      client.accountsOfficeReference.reference mustBe "123456"
      client.lpAuthorisation mustBe true
      client.name mustBe Some("Test Client")
      client.agentClientRef mustBe Some("AG123")
      client.requestedDeletionOn.map(_.toString) mustBe Some("2025-08-18T20:47:56")
    }

    "serialize Client to JSON correctly" in {
      val client = Client(
        empRef = EmpRef("123", "AB456"),
        accountsOfficeReference = AccountsOfficeReference("12", "A", "X", "12345"),
        name = Some("Test Client"),
        lpAuthorisation = false,
        agentClientRef = Some("AGT001"),
        requestedDeletionOn = Some(LocalDateTime.parse("2025-08-18T20:47:56"))
      )

      val json = Json.toJson(client)
      (json \ "empRef").as[String] mustBe "123/AB456"
      (json \ "accountsOfficeReference" \ "reference").as[String] mustBe "12345"
      (json \ "name").as[String] mustBe "Test Client"
    }

    "fail to deserialize when JSON is invalid (missing fields)" in {
      """
        |{
        |  "empRef": {
        |    "taxOfficeNumber": "123",
        |    "taxOfficeReference": "AB456"
        |  },
        |  "accountsOfficeReference": {
        |    "districtNumber": "12",
        |    "payType": "A",
        |    "checkCode": "C",
        |    "reference": "123456"
        |  },
        |  "agentClientRef": "AG123",
        |  "requestedDeletionOn": "2025-08-18T20:47:56"
        |}
        |""".stripMargin

      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[Client](emptyJson)
      result.isError mustBe true
    }

    "fail to deserialize when JSON is empty" in {
      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[Client](emptyJson)
      result.isError mustBe true
    }

    "fail to deserialize when JSON is empty for AccountsOfficeReference" in {
      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[AccountsOfficeReference](emptyJson)
      result.isError mustBe true
    }

  }
}
