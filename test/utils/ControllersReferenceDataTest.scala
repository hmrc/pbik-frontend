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

package utils

import java.util.Collections

import config.{AppConfig, PbikAppConfig}
import controllers.FakePBIKApplication
import org.scalatest.Matchers
import play.api.Play._
import play.api.i18n.Messages
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import support.TestAuthUser
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.Upstream5xxResponse
import uk.gov.hmrc.play.test.UnitSpec
import utils.Exceptions.{GenericServerErrorException, InvalidBikTypeURIException, InvalidYearURIException}

import scala.concurrent.Promise
import scala.util.Try

class ControllersReferenceDataTest extends UnitSpec with FakePBIKApplication
                                                    with Matchers with TestAuthUser with Results {


  object StubPbikConfig extends AppConfig with ServicesConfig {

      private def loadConfig(key: String) = configuration.getString(key).
                                                            getOrElse(throw new Exception(s"Missing key: $key"))
      private val citizenAuthHost = Try{baseUrl("citizen-auth")}.getOrElse("citizen-auth-not-found")
      private val contactFrontendService =  Try{baseUrl("contact-frontend")}.getOrElse("contact-frontend-no-found")
      private val contactHost = configuration.getString("microservice.services.contact-frontend.host").
                                                        getOrElse("contact-frontend-host-not-found")

      override lazy val assetsPrefix = loadConfig("assets.url") +
                                                                            loadConfig("assets.version")
      override lazy val reportAProblemPartialUrl = s"$contactFrontendService/contact/problem_reports?secure=true"
      override lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback"
      override lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
      override lazy val analyticsToken: String = configuration.getString("google-analytics.token").getOrElse("n/a")
      override lazy val analyticsHost: String = configuration.getString("google-analytics.host").getOrElse("auto")
      override lazy val cyEnabled = configuration.getBoolean("pbik.enabled.cy").getOrElse(false)
      override lazy val biksNotSupported:List[Int] = (configuration.getIntList("pbik.unsupported.biks").
                                                        getOrElse(Collections.emptyList[Integer]())).
                                                          toArray(new Array[Integer](0)).toList.map(_.intValue())
      override lazy val biksCount: Int = 17

  }

  object MockControllersReferenceData extends ControllersReferenceData {
    override def pbikAppConfig = PbikAppConfig
  }

  object TestCYEnabledConfig extends AppConfig {
    private def loadConfig(key: String) = ""

    private val contactFrontendService = ""
    private val contactHost = ""

    override lazy val assetsPrefix = ""
    override lazy val reportAProblemPartialUrl = ""
    override lazy val betaFeedbackUrl =""
    override lazy val betaFeedbackUnauthenticatedUrl = ""
    override lazy val analyticsToken: String = ""
    override lazy val analyticsHost: String = ""
    override lazy val cyEnabled = true
    override lazy val biksNotSupported:List[Int] = List.empty[Int]
    override lazy val biksCount:Int = 17
  }

  object MockCYEnabledControllersReferenceData extends ControllersReferenceData {
    override def pbikAppConfig = TestCYEnabledConfig
  }

  "When instantiating the ControllersReferenceData it " should {
    "not have a null config " in {
      running(fakeApplication) {
        val mockControllerConfig = ControllersReferenceData.pbikAppConfig
        assert(mockControllerConfig != null)
      }
    }
  }


  "When CY mode is disabled the controller " should {
    "display the result passsed to it " in {
      running(fakeApplication) {
        val mockController = MockControllersReferenceData
        implicit val user = createDummyUser("VALID_ID")
        val result = await(mockController.responseCheckCYEnabled( Ok("Passed Test") )(mockrequest, user))
        status (result) shouldBe 200
        bodyOf (result) should include ( Messages("ServiceMessage.10003"))
      }
    }
  }

  "When CY mode is enabled the controller " should {
    "display the result passsed to it " in {
      running(fakeApplication) {
        val mockController = MockCYEnabledControllersReferenceData
        implicit val user = createDummyUser("VALID_ID")
        val result = await(mockController.responseCheckCYEnabled( Ok("Passed Test") )(mockrequest, user))
        status (result) shouldBe 200
        bodyOf (result) should include ("Passed Test")
      }
    }
  }

  "When a system message is not identified the controller " should {
    "return a default " in {
      running(fakeApplication) {
        val test = new {
          val name = "ExtractTest"
        } with ControllersReferenceData {
          override def pbikAppConfig: AppConfig = ???

          assert( extractUpstreamError("TEST_NOT_VALID_ERROR_MSG") == DEFAULT_ERROR )

        }
      }
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show an error page when the Future completes with a NoSuchElementException " in {
      running(fakeApplication) {
        val mockController = MockControllersReferenceData
        implicit val user = createDummyUser("VALID_ID")
        val p = Promise[Result]()
        p.failure(new NoSuchElementException("NoSuchElement"))
        val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
        status (result) shouldBe 200
        bodyOf (result) should include ( Messages("ErrorPage.validationError") )
      }
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show an error page when the Future completes with a InvalidYearURIException " in {
      running(fakeApplication) {
        val mockController = MockControllersReferenceData
        implicit val user = createDummyUser("VALID_ID")
        val p = Promise[Result]()
        p.failure(new InvalidYearURIException)
        val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
        status (result) shouldBe 200
        bodyOf (result) should include ( Messages("ErrorPage.invalidYear") )
      }
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show an error page when the Future completes with a InvalidBikTypeURIException " in {
      running(fakeApplication) {
        val mockController = MockControllersReferenceData
        implicit val user = createDummyUser("VALID_ID")
        val p = Promise[Result]()
        p.failure(new InvalidBikTypeURIException)
        val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
        status (result) shouldBe 200
        bodyOf (result) should include ( Messages("ErrorPage.invalidBikType") )
      }
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show an error page when the Future completes with a Upstream5xxResponse " in {
      running(fakeApplication) {
        val mockController = MockControllersReferenceData
        implicit val user = createDummyUser("VALID_ID")
        val p = Promise[Result]()
        // TODO - check the NPS error message format used below
        p.failure( new Upstream5xxResponse("""{appStatusMessage=10002,}""", 100, 100) )
        val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
        status (result) shouldBe 200
        bodyOf (result) should include ( Messages("ServiceMessage.0.h1") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionProblem") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionAction") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionStatus") )
      }
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show the default error page if the Upstream5xxResponse error message is null " in {
      running(fakeApplication) {
        val mockController = MockControllersReferenceData
        implicit val user = createDummyUser("VALID_ID")
        val p = Promise[Result]()
        // TODO - check the NPS error message format used below
        p.failure( new Upstream5xxResponse(null, 100, 100) )
        val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
        status (result) shouldBe 200
        bodyOf (result) should include ( Messages("ServiceMessage.0.h1") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionProblem") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionAction") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionStatus") )
      }
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show the default error page if the Upstream5xxResponse error has no number " in {
      running(fakeApplication) {
        val mockController = MockControllersReferenceData
        implicit val user = createDummyUser("VALID_ID")
        val p = Promise[Result]()
        // Note - invalid error message number will not parse
        p.failure( new Upstream5xxResponse("NO ERROR NUMBER TO PARSE", 100, 100) )
        val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
        status (result) shouldBe 200
        bodyOf (result) should include ( Messages("ServiceMessage.0.h1") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionProblem") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionAction") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionStatus") )
      }
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show the default error page if the Upstream5xxResponse error omits the comma delimeter " in {
      running(fakeApplication) {
        val mockController = MockControllersReferenceData
        implicit val user = createDummyUser("VALID_ID")
        val p = Promise[Result]()
        // Note - missing comma will prevent message being parsed
        p.failure( new Upstream5xxResponse("""{appStatusMessage=10002}""", 100, 100) )
        val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
        status (result) shouldBe 200
        bodyOf (result) should include ( Messages("ServiceMessage.0.h1") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionProblem") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionAction") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionStatus") )

      }
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show an error page when the Future completes with a GenericServerErrorException " in {
      running(fakeApplication) {
        val mockController = MockControllersReferenceData
        implicit val user = createDummyUser("VALID_ID")
        val p = Promise[Result]()
        p.failure( new GenericServerErrorException("10003") )
        val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
        status (result) shouldBe 200
        bodyOf (result) should include ( Messages("ServiceMessage.10003") )
      }
    }
  }

  "When parsing the response in the responseErrorHandler the controller " should {
    "show the default error page if the GenericServerErrorException cannot be parsed" in {
      running(fakeApplication) {
        val mockController = MockControllersReferenceData
        implicit val user = createDummyUser("VALID_ID")
        val p = Promise[Result]()
        p.failure( new GenericServerErrorException("NO JSON") )
        val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
        status (result) shouldBe 200
        bodyOf (result) should include ( Messages("ErrorPage.connectionProblem") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionStatus") )
        bodyOf (result) should include ( Messages("ErrorPage.connectionAction") )
      }
    }
  }


  "When parsing the response in the responseErrorHandler the controller " should {
    "show the default error page when exception is unknown" in {
      running(fakeApplication) {
        val mockController = MockControllersReferenceData
        implicit val user = createDummyUser("VALID_ID")
        val p = Promise[Result]()
        p.failure( new RuntimeException )
        val result = await(mockController.responseErrorHandler(p.future)(mockrequest, user))
        status (result) shouldBe 200
        bodyOf (result) should include ( Messages("ErrorPage.connectionProblem") )
      }
    }
  }

}
