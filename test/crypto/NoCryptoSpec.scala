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

package crypto;

import base.FakePBIKApplication
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.crypto.{Crypted, PlainBytes, PlainText}

import java.nio.charset.StandardCharsets
import java.util.Base64;

class NoCryptoSpec extends FakePBIKApplication with MockitoSugar {

  "encrypt" should {

    "wrap the PlainText value in a Crypted object" in {
      val result = NoCrypto.encrypt(PlainText("value"))

      result shouldBe Crypted("value")
    }

    "wrap the PlainBytes value in a Crypted object" in {
      val rawBytes = new Array[Byte](10)
      val expected =
        new String(Base64.getEncoder.encode(rawBytes), StandardCharsets.UTF_8)

      val result = NoCrypto.encrypt(PlainBytes(rawBytes))

      result shouldBe Crypted(expected)
    }
  }

  "decrypt" should {

    "wrap the Crypted value in a PlainText object" in {
      val result = NoCrypto.decrypt(Crypted("value"))

      result shouldBe PlainText("value")
    }
  }

  "decryptAsBytes" should {

    "wrap the Crypted value in a PlainBytes object and Base64 decode the value" in {
      val result = NoCrypto.decryptAsBytes(Crypted("dmFsdWU="))

      result shouldBe a[PlainBytes]
    }
  }
}
