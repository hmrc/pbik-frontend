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

package models.form

import base.FakePBIKApplication
import play.api.libs.json.Json

class ExclusionNinoSpec extends FakePBIKApplication {

  "ExclusionNino" should {

    "serialize and deserialize JSON correctly" in {
      val nino = ExclusionNino("AB123456C")
      val json = Json.toJson(nino)

      (json \ "nino").as[String] mustBe "AB123456C"

      val fromJson = json.as[ExclusionNino]
      fromJson mustBe nino
    }

    "fail to deserialize when json is wrong" in {
      val invalidJson =
        """
          |{
          |  "ninoA": ""
          |}
          |""".stripMargin

      val result = Json.fromJson[ExclusionNino](Json.parse(invalidJson))

      result.isError mustBe true
    }

    "fail to deserialize when JSON is empty" in {
      val emptyJson = Json.parse("{}")

      val result = Json.fromJson[ExclusionNino](emptyJson)
      result.isError mustBe true
    }

  }

}
