/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors

import controllers.FakePBIKApplication
import models.{EmpRef, PbikError}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.mvc.Results
import support.TestAuthUser
import uk.gov.hmrc.http._
import utils.Exceptions.GenericServerErrorException

class TierConnectorSpec extends PlaySpec with FakePBIKApplication with TestAuthUser with Results {

  val fakeResponse: HttpResponse = HttpResponse(OK, "")

  val fakeResponseWithError: HttpResponse =
    HttpResponse(OK, Json.toJson(new PbikError("64990")), Map.empty[String, Seq[String]])

  val fakeSevereResponse: HttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, "A severe server error")

  val hmrcTierConnector = app.injector.instanceOf[HmrcTierConnector]

  val year = 2015

  "When creating a GET URL with an orgainsation needing encoding it" should {
    " encode the slash properly" in {

      val result: String =
        hmrcTierConnector.createGetUrl("theBaseUrl", "theURIExtension", EmpRef("780", "MODES16"), year)
      assert(result == "theBaseUrl/780%2FMODES16/2015/theURIExtension")
    }
  }

  "When creating a GET URL with no organisation it" should {
    " omit the organisation" in {
      val result: String = hmrcTierConnector.createGetUrl("theBaseUrl", "theURIExtension", EmpRef.empty, year)
      assert(result == "theBaseUrl/2015/theURIExtension")
    }
  }

  "When creating a POST URL with an organisation which needs encoding it" should {
    " be properly formed with the %2F encoding" in {
      val result: String =
        hmrcTierConnector.createPostUrl("theBaseUrl", "theURIExtension", EmpRef("780", "MODES16"), year)
      assert(result == "theBaseUrl/780%2FMODES16/2015/theURIExtension")
    }
  }

  "When processing a response if the status is greater than 400 it" should {
    " throw a GenericServerErrorException" in {
      intercept[GenericServerErrorException] {
        hmrcTierConnector.processResponse(fakeSevereResponse)
      }
    }
  }

  "When processing a response if the status is less than 400 it" should {
    " return the response" in {
      val resp = hmrcTierConnector.processResponse(fakeResponse)
      assert(resp.status == OK)
    }
  }

  "When processing a response if there is a PBIK error code" should {
    " throw a GenericServerErrorException" in {
      intercept[GenericServerErrorException] {
        hmrcTierConnector.processResponse(fakeResponseWithError)
      }
    }
  }

}
