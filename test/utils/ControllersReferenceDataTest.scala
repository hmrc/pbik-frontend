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

package utils

import java.util.Collections

import config.{AppConfig, PbikAppConfig, RunModeConfig}
import controllers.FakePBIKApplication
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Play
import play.api.i18n.Messages
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import support.TestAuthUser
import uk.gov.hmrc.play.config.ServicesConfig
import utils.Exceptions.{GenericServerErrorException, InvalidBikTypeURIException, InvalidYearURIException}
import play.api.http.HttpEntity.Strict
import play.api.i18n.Messages.Implicits._

import scala.concurrent.{Future, Promise}
import scala.util.Try
import uk.gov.hmrc.http.Upstream5xxResponse
import uk.gov.hmrc.play.frontend.auth.AuthContext

class ControllersReferenceDataTest extends PlaySpec with FakePBIKApplication
                                                    with TestAuthUser with Results {


  object StubPbikConfig extends AppConfig with ServicesConfig with RunModeConfig {

      private def loadConfig(key: String): String = Play.current.configuration.getString(key).
                                                            getOrElse(throw new Exception(s"Missing key: $key"))
      private val citizenAuthHost: String = Try{baseUrl("citizen-auth")}.getOrElse("citizen-auth-not-found")
      override lazy val contactFrontendService: String =  Try{baseUrl("contact-frontend")}.getOrElse("contact-frontend-no-found")
      override lazy val contactFormServiceIdentifier: String = ""

      private val contactHost: String = Play.current.configuration.getString("microservice.services.contact-frontend.host").
                                                        getOrElse("contact-frontend-host-not-found")

      override lazy val assetsPrefix: String = loadConfig("assets.url") + loadConfig("assets.version")
      override lazy val reportAProblemPartialUrl: String = s"$contactFrontendService/contact/problem_reports?secure=true"
      override lazy val betaFeedbackUrl: String = s"$contactHost/contact/beta-feedback"
      override lazy val betaFeedbackUnauthenticatedUrl: String = s"$contactHost/contact/beta-feedback-unauthenticated"
      override lazy val analyticsToken: String = Play.current.configuration.getString("google-analytics.token").getOrElse("n/a")
      override lazy val analyticsHost: String = Play.current.configuration.getString("google-analytics.host").getOrElse("auto")
      override lazy val cyEnabled: Boolean = Play.current.configuration.getBoolean("pbik.enabled.cy").getOrElse(false)
      override lazy val biksNotSupported:List[Int] = Play.current.configuration.getIntList("pbik.unsupported.biks").
                                                        getOrElse(Collections.emptyList[Integer]()).
                                                          toArray(new Array[Integer](0)).toList.map(_.intValue())
      override lazy val biksNotSupportedCY:List[Int] = Play.current.configuration.getIntList("pbik.unsupported.biks.cy").
                                                        getOrElse(Collections.emptyList[Integer]()).
                                                          toArray(new Array[Integer](0)).toList.map(_.intValue())
      override lazy val biksDecommissioned:List[Int] = Play.current.configuration.getIntList("pbik.decommissioned.biks").
                                                        getOrElse(Collections.emptyList[Integer]()).
                                                          toArray(new Array[Integer](0)).toList.map(_.intValue())
      override lazy val maximumExclusions:Int = 300

      override lazy val urBannerLink:String = ""
    override val serviceSignOut: String = ""
  }

  object TestCYEnabledConfig extends AppConfig {
    private def loadConfig(key: String) = ""

    override lazy val contactFrontendService: String =  ""
    override lazy val contactFormServiceIdentifier: String = ""

    override lazy val assetsPrefix: String = ""
    override lazy val reportAProblemPartialUrl: String = ""
    override lazy val betaFeedbackUrl: String =""
    override lazy val betaFeedbackUnauthenticatedUrl: String = ""
    override lazy val analyticsToken: String = ""
    override lazy val analyticsHost: String = ""
    override lazy val cyEnabled: Boolean = true
    override lazy val biksNotSupported: List[Int] = List.empty[Int]
    override lazy val biksNotSupportedCY: List[Int] = List.empty[Int]
    override lazy val biksDecommissioned: List[Int] = List.empty[Int]
    override lazy val maximumExclusions: Int = 300
    override lazy val urBannerLink: String = ""
    override val serviceSignOut: String = ""
  }

  object TestCYDisabledConfig extends AppConfig{
    private def loadConfig(key: String) = ""

    override lazy val contactFrontendService: String =  ""
    override lazy val contactFormServiceIdentifier: String = ""

    override lazy val assetsPrefix: String = ""
    override lazy val reportAProblemPartialUrl: String = ""
    override lazy val betaFeedbackUrl: String =""
    override lazy val betaFeedbackUnauthenticatedUrl: String = ""
    override lazy val analyticsToken: String = ""
    override lazy val analyticsHost: String = ""
    override lazy val cyEnabled: Boolean = false
    override lazy val biksNotSupported:List[Int] = List.empty[Int]
    override lazy val biksNotSupportedCY:List[Int] = List.empty[Int]
    override lazy val biksDecommissioned:List[Int] = List.empty[Int]
    override lazy val maximumExclusions:Int = 300
    override lazy val urBannerLink: String = ""
    override val serviceSignOut: String = ""
  }

  object MockCYEnabledControllersReferenceData extends ControllersReferenceData {
    override def pbikAppConfig: TestCYEnabledConfig.type = TestCYEnabledConfig
  }

  object MockCYDisabledControllersReferenceData extends ControllersReferenceData {
    override def pbikAppConfig: TestCYDisabledConfig.type = TestCYDisabledConfig
  }

  object MockControllersReferenceData extends ControllersReferenceData {
    override def pbikAppConfig: PbikAppConfig.type = PbikAppConfig
  }

  "When instantiating the ControllersReferenceData it " should {
    "not have a null config " in {
      val mockControllerConfig = ControllersReferenceData.pbikAppConfig
      assert(mockControllerConfig != null)
    }
  }

  "When CY mode is disabled the controller " should {
    "display the result passsed to it " in {
      val mockController = MockCYDisabledControllersReferenceData
      implicit val user: AuthContext = createDummyUser("VALID_ID")
      val result = await(mockController.responseCheckCYEnabled(Future{Ok("Passed Test")}(scala.concurrent.ExecutionContext.Implicits.global))(mockrequest, user))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.10003.1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.10003.2"))
    }
  }

  "When CY mode is enabled the controller " should {
    "display the result passsed to it " in {
      val mockController = MockCYEnabledControllersReferenceData
      implicit val user: AuthContext = createDummyUser("VALID_ID")
      val result = await(mockController.responseCheckCYEnabled(Future{Ok("Passed Test")}(scala.concurrent.ExecutionContext.Implicits.global))(mockrequest, user))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include("Passed Test")
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show an error page when the Future completes with a NoSuchElementException " in {
      val mockController = MockControllersReferenceData
      implicit val user: AuthContext = createDummyUser("VALID_ID")
      val p = Promise[Result]()
      p.failure(new NoSuchElementException("NoSuchElement"))
      val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.validationError") )
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show an error page when the Future completes with a InvalidYearURIException " in {
      val mockController = MockControllersReferenceData
      implicit val user: AuthContext = createDummyUser("VALID_ID")
      val p = Promise[Result]()
      p.failure(new InvalidYearURIException)
      val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.invalidYear"))
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show an error page when the Future completes with a InvalidBikTypeURIException " in {
      val mockController = MockControllersReferenceData
      implicit val user: AuthContext = createDummyUser("VALID_ID")
      val p = Promise[Result]()
      p.failure(new InvalidBikTypeURIException)
      val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.invalidBikType"))
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show an error page when the Future completes with a Upstream5xxResponse " in {
      val mockController = MockControllersReferenceData
      implicit val user = createDummyUser("VALID_ID")
      val p = Promise[Result]()
      // TODO - check the NPS error message format used below
      p.failure(Upstream5xxResponse("""{appStatusMessage=10002,}""", 100, 100) )
      val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include ( Messages("ServiceMessage.0.h1") )
      result.body.asInstanceOf[Strict].data.utf8String must include ( Messages("ErrorPage.connectionProblem") )
      result.body.asInstanceOf[Strict].data.utf8String must include ( Messages("ErrorPage.connectionAction") )
      result.body.asInstanceOf[Strict].data.utf8String must include ( Messages("ErrorPage.connectionStatus") )
    }
  }

   "When parsing the response in the responseErrorHandler the controller " should {
     "show the default error page if the Upstream5xxResponse error message is null " in {
       val mockController = MockControllersReferenceData
       implicit val user: AuthContext = createDummyUser("VALID_ID")
       val p = Promise[Result]()
       // TODO - check the NPS error message format used below
       p.failure(Upstream5xxResponse(null, 100, 100) )
       val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
       result.header.status must be(OK) // 200
       result.body.asInstanceOf[Strict].data.utf8String must include (Messages("ServiceMessage.0.h1"))
       result.body.asInstanceOf[Strict].data.utf8String must include (Messages("ErrorPage.connectionProblem"))
       result.body.asInstanceOf[Strict].data.utf8String must include (Messages("ErrorPage.connectionAction"))
       result.body.asInstanceOf[Strict].data.utf8String must include (Messages("ErrorPage.connectionStatus"))
     }
   }

   "When parsing the response in the responseErrorHandler the controller " should {
     "show the default error page if the Upstream5xxResponse error has no number " in {
       val mockController = MockControllersReferenceData
       implicit val user: AuthContext = createDummyUser("VALID_ID")
       val p = Promise[Result]()
       // Note - invalid error message number will not parse
       p.failure(Upstream5xxResponse("NO ERROR NUMBER TO PARSE", 100, 100) )
       val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
       result.header.status must be(OK) // 200
       result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.0.h1"))
       result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionProblem"))
       result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionAction"))
       result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionStatus"))
     }
   }

   "When parsing the response in the responseErrorHandler the controller " should {
     "show the default error page if the Upstream5xxResponse error omits the comma delimeter " in {
       val mockController = MockControllersReferenceData
       implicit val user: AuthContext = createDummyUser("VALID_ID")
       val p = Promise[Result]()
       // Note - missing comma will prevent message being parsed
       p.failure(Upstream5xxResponse("""{appStatusMessage=10002}""", 100, 100) )
       val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
       result.header.status must be(OK) // 200
       result.body.asInstanceOf[Strict].data.utf8String must include ( Messages("ServiceMessage.0.h1") )
       result.body.asInstanceOf[Strict].data.utf8String must include ( Messages("ErrorPage.connectionProblem") )
       result.body.asInstanceOf[Strict].data.utf8String must include ( Messages("ErrorPage.connectionAction") )
       result.body.asInstanceOf[Strict].data.utf8String must include ( Messages("ErrorPage.connectionStatus") )
     }
   }

   "When parsing the response in the responseErrorHandler the controller " should {
     "show an error page when the Future completes with a GenericServerErrorException " in {
       val mockController = MockControllersReferenceData
       implicit val user: AuthContext = createDummyUser("VALID_ID")
       val p = Promise[Result]()
       p.failure( new GenericServerErrorException("10003") )
       val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
       result.header.status must be(OK) // 200
       result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.10003.h1"))
     }
   }

   "When parsing the response in the responseErrorHandler the controller " should {
     "show the default error page if the GenericServerErrorException cannot be parsed" in {
       val mockController = MockControllersReferenceData
       implicit val user: AuthContext = createDummyUser("VALID_ID")
       val p = Promise[Result]()
       p.failure( new GenericServerErrorException("NO JSON") )
       val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
       result.header.status must be(OK) // 200
       result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionProblem"))
       result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionStatus"))
       result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionAction"))
     }
   }

   "When parsing the response in the responseErrorHandler the controller " should {
     "show the default error page when exception is unknown" in {
       val mockController = MockControllersReferenceData
       implicit val user: AuthContext = createDummyUser("VALID_ID")
       val p = Promise[Result]()
       p.failure( new RuntimeException )
       val result = await(mockController.responseErrorHandler(p.future)(mockrequest))
       result.header.status must be(OK) // 200
       result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionProblem"))
     }
   }

}
