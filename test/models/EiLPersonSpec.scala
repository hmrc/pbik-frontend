/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec

class EiLPersonSpec extends PlaySpec {

  private def eiLPerson(
    nino: String = "AB123456C",
    firstForename: String = "John",
    worksPayrollNumber: Option[String] = Some("123/AB123456C")
  ): EiLPerson =
    EiLPerson(
      nino = nino,
      firstForename = firstForename,
      secondForename = Some("Smith"),
      surname = "Smith",
      worksPayrollNumber = worksPayrollNumber,
      dateOfBirth = Some("01/01/1990"),
      gender = Some("Male"),
      status = None,
      perOptLock = 1
    )

  "EiLPerson" when {
    ".equals" must {
      "return true if 2 EiLPerson instances have the same nino" in {
        eiLPerson().equals(eiLPerson(firstForename = "Dan")) mustBe true
      }

      "return false if 2 EiLPerson instances have different nino" in {
        eiLPerson().equals(eiLPerson(nino = "AB123456B")) mustBe false
      }

      "return false if EiLPerson equals wrong object" in {
        //noinspection ComparingUnrelatedTypes
        eiLPerson().equals(BigDecimal("123")) mustBe false
      }
    }

    ".hashCode" must {
      "return a hash integer for the nino rather than the EiLPerson instance" in {
        val nino: String       = "AB123456C"
        val generatedHash: Int = nino.hashCode

        eiLPerson().hashCode mustBe generatedHash
      }
    }

    ".fullName" must {
      "return full name" in {
        val fullName: String = s"${eiLPerson().firstForename} ${eiLPerson().surname}"

        eiLPerson().fullName mustBe fullName
      }
    }

    ".getWorksPayrollNumber" must {
      "return the string value for a works payroll number that is present" in {
        eiLPerson().getWorksPayrollNumber mustBe "123/AB123456C"
      }

      "return UNKNOWN for a works payroll number that is absent" in {
        eiLPerson(worksPayrollNumber = None).getWorksPayrollNumber mustBe "UNKNOWN"
      }
    }
  }
}
