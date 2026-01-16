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

package config

import base.FakePBIKApplication
import play.api.Configuration

class ServiceSpec extends FakePBIKApplication {

  "Service" should {

    "create an instance correctly" in {
      val service = Service("localhost", "8080", "http")
      service.host mustBe "localhost"
      service.port mustBe "8080"
      service.protocol mustBe "http"
    }

    "convert to string correctly" in {
      val service = Service("localhost", "8080", "http")

      service.toString mustBe "http://localhost:8080"
      Service.convertToString(service) mustBe "http://localhost:8080"
    }

    "load from configuration correctly" in {
      val config = Configuration.from(
        Map(
          "service.host"     -> "localhost",
          "service.port"     -> "8080",
          "service.protocol" -> "http"
        )
      )

      val service = config.get[Service]("service")
      service.host mustBe "localhost"
      service.port mustBe "8080"
      service.protocol mustBe "http"
    }
  }
}
