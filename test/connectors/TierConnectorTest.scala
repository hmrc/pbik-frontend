/*
 * Copyright 2015 HM Revenue & Customs
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

import connectors.FrontendAuditConnector
import controllers.FakePBIKApplication
import org.scalatest.Matchers
import play.api.mvc.Results
import play.api.test.Helpers._
import support.TestAuthUser
import uk.gov.hmrc.play.config.{RunMode, AppName}
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.http.ws.{WSDelete, WSPost, WSPut, WSGet}
import uk.gov.hmrc.play.test.UnitSpec
import utils.Exceptions.GenericServerErrorException

import scala.concurrent.Future

class TierConnectorTest  extends UnitSpec with FakePBIKApplication
                                                with Matchers with TestAuthUser with Results {

  "When instantiating the TierConnector it " should {
    "not have a null tierConnector reference " in {
      running(fakeApplication) {
        val tc = TierConnector.tierConnector
        assert(tc != null)
      }
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
  }

  class FakeSevereResponse extends HttpResponse {
    override def status = 500
    override def body = "A severe server error"
  }


  class MockHmrcTierConnector extends HmrcTierConnector {
    object WSHttp extends WSGet with WSPut with WSPost with WSDelete with AppName with RunMode {
      override val auditConnector = FrontendAuditConnector
      override def doGet(url : scala.Predef.String)(implicit hc : uk.gov.hmrc.play.audit.http.HeaderCarrier) :
                scala.concurrent.Future[uk.gov.hmrc.play.http.HttpResponse] = Future.successful(new FakeResponse)
    }
  }

  "When creating a GET URL with an orgainsation needing encoding it " should {
    " encode the slash properly " in {
      running(fakeApplication) {
        val tc = new MockHmrcTierConnector
        val result:String = tc.createGetUrl("theBaseUrl","theURIExtension","780/MODES16",2015)
        assert(result == "theBaseUrl/780%2FMODES16/2015/theURIExtension")
      }
    }
  }

  "When creating a GET URL with no organisation it " should {
    " omit the organisation " in {
      running(fakeApplication) {
        val tc = new MockHmrcTierConnector
        val result:String = tc.createGetUrl("theBaseUrl","theURIExtension","",2015)
        assert(result == "theBaseUrl/2015/theURIExtension")
      }
    }
  }

  "When creating a GET URL with a null organisation it " should {
    " omit the organisation " in {
      running(fakeApplication) {
        val tc = new MockHmrcTierConnector
        val result:String = tc.createGetUrl("theBaseUrl","theURIExtension",null,2015)
        assert(result == "theBaseUrl/2015/theURIExtension")
      }
    }
  }

  "When creating a GET URL with an orgainsation which doesnt need encoding it " should {
    " still be properly formed " in {
      running(fakeApplication) {
        val tc = new MockHmrcTierConnector
        val result:String = tc.createGetUrl("theBaseUrl","theURIExtension","nonEncodedOrganisation",2015)
        assert(result == "theBaseUrl/nonEncodedOrganisation/2015/theURIExtension")
      }
    }
  }

  "When encoding a slash it " should {
    " becomes %2F " in {
      running(fakeApplication) {
        val tc = new MockHmrcTierConnector
        val result:String = tc.encode("/")
        assert(result == "%2F")
      }
    }
  }

  "When creating a POST URL with an organisation which needs encoding it " should {
    " be properly formed with the %2F encoding " in {
      running(fakeApplication) {
        val tc = new MockHmrcTierConnector
        val result:String = tc.createPostUrl("theBaseUrl", "theURIExtension", "780/MODES16", 2015)
        assert(result == "theBaseUrl/780%2FMODES16/2015/theURIExtension")
      }
    }
  }

  "When processing a response if the status is greater than 400 it " should {
    " throw a GenericServerErrorException " in {
      running(fakeApplication) {
        val tc = new MockHmrcTierConnector
        intercept[GenericServerErrorException] {
          tc.processResponse(new FakeSevereResponse)
        }
      }
    }
  }

  "When processing a response if the status is less than 400 it " should {
    " return the response " in {
      running(fakeApplication) {
        val tc = new MockHmrcTierConnector
        val resp = tc.processResponse(new FakeResponse)
        assert(resp.status == 200)
      }
    }
  }

}
