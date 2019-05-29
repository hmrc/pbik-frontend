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

import config.{AppConfig, LocalFormPartialRetriever, PbikAppConfig, PbikContext}
import controllers.{ExternalUrls, FakePBIKApplication}
import models.{AuthenticatedRequest, EmpRef, UserName}
import org.scalatestplus.play.PlaySpec
import play.api.http.HttpEntity.Strict
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.{TestAuthUser, TestCYDisabledConfig, TestCYEnabledConfig}
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.Upstream5xxResponse
import utils.Exceptions.{GenericServerErrorException, InvalidBikTypeURIException, InvalidYearURIException}

import scala.concurrent.{Future, Promise}

class ControllersReferenceDataSpec extends PlaySpec with FakePBIKApplication
  with TestAuthUser with Results {

  object MockCYEnabledControllersReferenceData extends ControllersReferenceData(
    app.injector.instanceOf[TaxDateUtils],
    app.injector.instanceOf[PbikContext],
    app.injector.instanceOf[AppConfig],
    app.injector.instanceOf[ExternalUrls],
    app.injector.instanceOf[LocalFormPartialRetriever]
  ) {
    override val pbikAppConfig: AppConfig = TestCYEnabledConfig
  }

  object MockCYDisabledControllersReferenceData extends ControllersReferenceData(
    app.injector.instanceOf[TaxDateUtils],
    app.injector.instanceOf[PbikContext],
    app.injector.instanceOf[AppConfig],
    app.injector.instanceOf[ExternalUrls],
    app.injector.instanceOf[LocalFormPartialRetriever]
  ) {
    override val pbikAppConfig: AppConfig = TestCYDisabledConfig
  }

  object MockControllersReferenceData extends ControllersReferenceData(
    app.injector.instanceOf[TaxDateUtils],
    app.injector.instanceOf[PbikContext],
    app.injector.instanceOf[AppConfig],
    app.injector.instanceOf[ExternalUrls],
    app.injector.instanceOf[LocalFormPartialRetriever]
  ) {
    override val pbikAppConfig: AppConfig = app.injector.instanceOf[PbikAppConfig]
  }

  "When CY mode is disabled the controller" should {
    "display the result passsed to it" in {
      val mockController = MockCYDisabledControllersReferenceData
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val result = await(mockController.responseCheckCYEnabled(Future {
        Ok("Passed Test")
      }(scala.concurrent.ExecutionContext.Implicits.global))(authenticatedRequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.10003.1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.10003.2"))
    }
  }

  "When CY mode is enabled the controller" should {
    "display the result passsed to it" in {
      val mockController = MockCYEnabledControllersReferenceData
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val result = await(mockController.responseCheckCYEnabled(Future {
        Ok("Passed Test")
      }(scala.concurrent.ExecutionContext.Implicits.global))(authenticatedRequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include("Passed Test")
    }
  }

  "When parsing the response in the responseErrorHandler the controller" should {
    "show an error page when the Future completes with a NoSuchElementException" in {
      val mockController = MockControllersReferenceData
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val p = Promise[Result]()
      p.failure(new NoSuchElementException("NoSuchElement"))
      val result = await(mockController.responseErrorHandler(p.future)(authenticatedRequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.validationError"))
    }
  }

  "When parsing the response in the responseErrorHandler the controller" should {
    "show an error page when the Future completes with a InvalidYearURIException" in {
      val mockController = MockControllersReferenceData
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val p = Promise[Result]()
      p.failure(new InvalidYearURIException)
      val result = await(mockController.responseErrorHandler(p.future)(authenticatedRequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.invalidYear"))
    }
  }

  "When parsing the response in the responseErrorHandler the controller" should {
    "show an error page when the Future completes with a InvalidBikTypeURIException" in {
      val mockController = MockControllersReferenceData
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val p = Promise[Result]()
      p.failure(new InvalidBikTypeURIException)
      val result = await(mockController.responseErrorHandler(p.future)(authenticatedRequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.invalidBikType"))
    }
  }

  "When parsing the response in the responseErrorHandler the controller" should {
    "show an error page when the Future completes with a Upstream5xxResponse" in {
      val mockController = MockControllersReferenceData
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val p = Promise[Result]()
      // TODO - check the NPS error message format used below
      p.failure(Upstream5xxResponse("""{appStatusMessage=10002,}""", 100, 100))
      val result = await(mockController.responseErrorHandler(p.future)(authenticatedRequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.0.h1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionProblem"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionAction"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionStatus"))
    }
  }

  "When parsing the response in the responseErrorHandler the controller" should {
    "show the default error page if the Upstream5xxResponse error message is null" in {
      val mockController = MockControllersReferenceData
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val p = Promise[Result]()
      // TODO - check the NPS error message format used below
      p.failure(Upstream5xxResponse(null, 100, 100))
      val result = await(mockController.responseErrorHandler(p.future)(authenticatedRequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.0.h1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionProblem"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionAction"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionStatus"))
    }
  }

  "When parsing the response in the responseErrorHandler the controller" should {
    "show the default error page if the Upstream5xxResponse error has no number" in {
      val mockController = MockControllersReferenceData
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val p = Promise[Result]()
      // Note - invalid error message number will not parse
      p.failure(Upstream5xxResponse("NO ERROR NUMBER TO PARSE", 100, 100))
      val result = await(mockController.responseErrorHandler(p.future)(authenticatedRequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.0.h1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionProblem"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionAction"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionStatus"))
    }
  }

  "When parsing the response in the responseErrorHandler the controller" should {
    "show the default error page if the Upstream5xxResponse error omits the comma delimeter" in {
      val mockController = MockControllersReferenceData
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val p = Promise[Result]()
      // Note - missing comma will prevent message being parsed
      p.failure(Upstream5xxResponse("""{appStatusMessage=10002}""", 100, 100))
      val result = await(mockController.responseErrorHandler(p.future)(authenticatedRequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.0.h1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionProblem"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionAction"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionStatus"))
    }
  }

  "When parsing the response in the responseErrorHandler the controller" should {
    "show an error page when the Future completes with a GenericServerErrorException" in {
      val mockController = MockControllersReferenceData
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val p = Promise[Result]()
      p.failure(new GenericServerErrorException("10003"))
      val result = await(mockController.responseErrorHandler(p.future)(authenticatedRequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ServiceMessage.10003.h1"))
    }
  }

  "When parsing the response in the responseErrorHandler the controller" should {
    "show the default error page if the GenericServerErrorException cannot be parsed" in {
      val mockController = MockControllersReferenceData
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val p = Promise[Result]()
      p.failure(new GenericServerErrorException("NO JSON"))
      val result = await(mockController.responseErrorHandler(p.future)(authenticatedRequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionProblem"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionStatus"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionAction"))
    }
  }

  "When parsing the response in the responseErrorHandler the controller" should {
    "show the default error page when exception is unknown" in {
      val mockController = MockControllersReferenceData
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val p = Promise[Result]()
      p.failure(new RuntimeException)
      val result = await(mockController.responseErrorHandler(p.future)(authenticatedRequest))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("ErrorPage.connectionProblem"))
    }
  }

}
