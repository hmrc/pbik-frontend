/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import config.RunModeConfig
import controllers.FakePBIKApplication
import models.{EmpRef, PbikError}
import org.scalatestplus.play.PlaySpec
import play.api.Play
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results
import support.TestAuthUser
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.ws._
import utils.Exceptions.GenericServerErrorException

import scala.concurrent.Future

class TierConnectorSpec extends PlaySpec  with FakePBIKApplication
                                          with TestAuthUser with Results {

  "When instantiating the TierConnector it" should {
    "not have a null tierConnector reference" in {
      val tc = TierConnector.tierConnector
      assert(tc != null)
    }
  }

  class FakeResponse extends HttpResponse {
    override def status = 200
    override def body = ""
  }

  class FakeResponseWithError extends HttpResponse {
    override def status = 200
    val jsonValue: JsValue = Json.toJson(new PbikError("64990"))
    override def body: String = jsonValue.toString()
    override def json: JsValue = jsonValue
  }

  class FakeSevereResponse extends HttpResponse {
    override def status = 500
    override def body = "A severe server error"
  }

  class MockHmrcTierConnector extends HmrcTierConnector {
    object WSHttp extends WSGet with HttpGet with WSPut with HttpPut with WSPost with HttpPost with WSDelete with HttpDelete with WSPatch with HttpPatch with AppName with RunMode with HttpAuditing with RunModeConfig {
      override val hooks = Seq(AuditingHook)
      override val auditConnector: FrontendAuditConnector.type = FrontendAuditConnector
      override val configuration: Option[Config] = Some(appNameConfiguration.underlying)
      override val actorSystem: ActorSystem = Play.current.actorSystem
      override def doGet(url : scala.Predef.String)(implicit hc : _root_.uk.gov.hmrc.http.HeaderCarrier) :
                scala.concurrent.Future[_root_.uk.gov.hmrc.http.HttpResponse] = Future.successful(new FakeResponse)
    }
  }

  "When creating a GET URL with an orgainsation needing encoding it" should {
   " encode the slash properly" in {
      val tc = new MockHmrcTierConnector
      val result:String = tc.createGetUrl("theBaseUrl","theURIExtension", EmpRef("780", "MODES16"),2015)
      assert(result == "theBaseUrl/780%2FMODES16/2015/theURIExtension")
    }
  }

  "When creating a GET URL with no organisation it" should {
   " omit the organisation" in {
      val tc = new MockHmrcTierConnector
      val result:String = tc.createGetUrl("theBaseUrl","theURIExtension", EmpRef.empty,2015)
      assert(result == "theBaseUrl/2015/theURIExtension")
    }
  }

  "When creating a POST URL with an organisation which needs encoding it" should {
   " be properly formed with the %2F encoding" in {
      val tc = new MockHmrcTierConnector
      val result:String = tc.createPostUrl("theBaseUrl", "theURIExtension", EmpRef("780", "MODES16"), 2015)
      assert(result == "theBaseUrl/780%2FMODES16/2015/theURIExtension")
    }
  }

  "When processing a response if the status is greater than 400 it" should {
   " throw a GenericServerErrorException" in {
      val tc = new MockHmrcTierConnector
      intercept[GenericServerErrorException] {
        tc.processResponse(new FakeSevereResponse)
      }
    }
  }

  "When processing a response if the status is less than 400 it" should {
   " return the response" in {
      val tc = new MockHmrcTierConnector
      val resp = tc.processResponse(new FakeResponse)
      assert(resp.status == 200)
    }
  }

  "When processing a response if there is a PBIK error code" should {
   " throw a GenericServerErrorException" in {
      val tc = new MockHmrcTierConnector
      intercept[GenericServerErrorException] {
        tc.processResponse(new FakeResponseWithError)
      }
    }
  }

}
