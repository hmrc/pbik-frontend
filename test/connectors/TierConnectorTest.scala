/*
 * Copyright 2017 HM Revenue & Customs
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
import models.PbikError
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test.Helpers._
import support.TestAuthUser
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.ws._
import utils.Exceptions.GenericServerErrorException

import scala.concurrent.Future
import uk.gov.hmrc.http._

class TierConnectorTest extends PlaySpec with OneAppPerSuite with FakePBIKApplication
                                          with TestAuthUser with Results {

  "When instantiating the TierConnector it " should {
    "not have a null tierConnector reference " in {
      val tc = TierConnector.tierConnector
      assert(tc != null)
    }
  }

//  "When instantiating the HmrcTierConnector it " should {
//    "not have a null serviceUrl reference " in {
//      running(fakeApplication) {
//        val tc = HmrcTierConnector
//        assert(tc.serviceUrl != null)
//      }
//    }
//  }

//  "When instantiating the HmrcTierConnector it " should {
//    "not have a null http reference " in {
//      running(fakeApplication) {
//        val tc = HmrcTierConnector.http
//        assert(tc != null)
//      }
//    }
//  }

  class FakeResponse extends HttpResponse {
    override def status = 200
    override def body = ""
  }

  class FakeResponseWithError extends HttpResponse {
    override def status = 200
    val jsonValue = Json.toJson(new PbikError("64990"))
    override def body = jsonValue.toString()
    override def json = jsonValue
  }

  class FakeSevereResponse extends HttpResponse {
    override def status = 500
    override def body = "A severe server error"
  }


  class MockHmrcTierConnector extends HmrcTierConnector {
    object WSHttp extends WSGet with HttpGet with WSPut with HttpPut with WSPost with HttpPost with WSDelete with HttpDelete with WSPatch with HttpPatch with AppName with RunMode with HttpAuditing {
      override val hooks = Seq(AuditingHook)
      override val auditConnector = FrontendAuditConnector
      override def doGet(url : scala.Predef.String)(implicit hc : _root_.uk.gov.hmrc.http.HeaderCarrier) :
                scala.concurrent.Future[_root_.uk.gov.hmrc.http.HttpResponse] = Future.successful(new FakeResponse)
    }
  }

  "When creating a GET URL with an orgainsation needing encoding it " should {
    " encode the slash properly " in {
      val tc = new MockHmrcTierConnector
      val result:String = tc.createGetUrl("theBaseUrl","theURIExtension","780/MODES16",2015)
      assert(result == "theBaseUrl/780%2FMODES16/2015/theURIExtension")
    }
  }

  "When creating a GET URL with no organisation it " should {
    " omit the organisation " in {
      val tc = new MockHmrcTierConnector
      val result:String = tc.createGetUrl("theBaseUrl","theURIExtension","",2015)
      assert(result == "theBaseUrl/2015/theURIExtension")
    }
  }

  "When creating a GET URL with a null organisation it " should {
    " omit the organisation " in {
      val tc = new MockHmrcTierConnector
      val result:String = tc.createGetUrl("theBaseUrl","theURIExtension",null,2015)
      assert(result == "theBaseUrl/2015/theURIExtension")
    }
  }

  "When creating a GET URL with an orgainsation which doesnt need encoding it " should {
    " still be properly formed " in {
      val tc = new MockHmrcTierConnector
      val result:String = tc.createGetUrl("theBaseUrl","theURIExtension","nonEncodedOrganisation",2015)
      assert(result == "theBaseUrl/nonEncodedOrganisation/2015/theURIExtension")
    }
  }

  "When encoding a slash it " should {
    " becomes %2F " in {
      val tc = new MockHmrcTierConnector
      val result:String = tc.encode("/")
      assert(result == "%2F")
    }
  }

  "When creating a POST URL with an organisation which needs encoding it " should {
    " be properly formed with the %2F encoding " in {
      val tc = new MockHmrcTierConnector
      val result:String = tc.createPostUrl("theBaseUrl", "theURIExtension", "780/MODES16", 2015)
      assert(result == "theBaseUrl/780%2FMODES16/2015/theURIExtension")
    }
  }

  "When processing a response if the status is greater than 400 it " should {
    " throw a GenericServerErrorException " in {
      val tc = new MockHmrcTierConnector
      intercept[GenericServerErrorException] {
        tc.processResponse(new FakeSevereResponse)
      }
    }
  }

  "When processing a response if the status is less than 400 it " should {
    " return the response " in {
      val tc = new MockHmrcTierConnector
      val resp = tc.processResponse(new FakeResponse)
      assert(resp.status == 200)
    }
  }

  "When processing a response if there is a PBIK error code " should {
    " throw a GenericServerErrorException " in {
      val tc = new MockHmrcTierConnector
      intercept[GenericServerErrorException] {
        tc.processResponse(new FakeResponseWithError)
      }
    }
  }

}
