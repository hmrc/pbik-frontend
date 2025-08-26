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

package utils

import base.FakePBIKApplication
import models.auth.AuthenticatedRequest
import play.api.http.HttpEntity.Strict
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Result, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.Exceptions.{GenericServerErrorException, InvalidBikTypeException, InvalidYearURIException}

import scala.concurrent.{Future, Promise}

class ControllersReferenceDataSpec extends FakePBIKApplication {

  override val configMap: Map[String, Any] = Map(
    "auditing.enabled" -> false,
    "sessionId"        -> "a-session-id"
  )

  private val mockControllersReferenceData: ControllersReferenceData = injected[ControllersReferenceData]
  private val messages: Messages                                     = injected[MessagesApi].preferred(Seq(lang))

  private trait Test {
    implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = createAuthenticatedRequest(mockRequest)
    val p: Promise[Result]                                              = Promise[Result]()
  }

  "ControllersReferenceData" when {
    "CY mode is disabled" should {
      "display the result passed to it" in new Test {
        val result: Result = await(mockControllersReferenceData.responseCheckCYEnabled(Future {
          Results.Ok("Passed Test")
        }(scala.concurrent.ExecutionContext.Implicits.global))(authenticatedRequest))

        result.header.status mustBe FORBIDDEN
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ServiceMessage.10003.1"))
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ServiceMessage.10003.2"))
      }
    }

    "parsing the response in the responseErrorHandler" should {
      "show an error page when the Future completes with a NoSuchElementException" in new Test {
        p.failure(new NoSuchElementException("NoSuchElement"))
        val result: Result = await(mockControllersReferenceData.responseErrorHandler(p.future)(authenticatedRequest))

        result.header.status mustBe NOT_FOUND
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.validationError"))
      }

      "show an error page when the Future completes with a InvalidYearURIException" in new Test {
        p.failure(new InvalidYearURIException)
        val result: Result = await(mockControllersReferenceData.responseErrorHandler(p.future)(authenticatedRequest))

        result.header.status mustBe BAD_REQUEST
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.invalidYear"))
      }

      "show an error page when the Future completes with a InvalidBikTypeException" in new Test {
        p.failure(new InvalidBikTypeException("Invalid Bik Type"))
        val result: Result = await(mockControllersReferenceData.responseErrorHandler(p.future)(authenticatedRequest))

        result.header.status mustBe BAD_REQUEST
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.invalidBikType"))
      }

      "show an error page when the Future completes with a Upstream5xxResponse" in new Test {
        p.failure(UpstreamErrorResponse("""{appStatusMessage=10002,}""", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))
        val result: Result = await(mockControllersReferenceData.responseErrorHandler(p.future)(authenticatedRequest))

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.title"))
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.try.later"))
      }

      "show the default error page if the Upstream5xxResponse error message is null" in new Test {
        p.failure(UpstreamErrorResponse(null, INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))
        val result: Result = await(mockControllersReferenceData.responseErrorHandler(p.future)(authenticatedRequest))

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.title"))
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.try.later"))
      }

      "show the default error page if the Upstream5xxResponse error has no number" in new Test {
        p.failure(UpstreamErrorResponse("NO ERROR NUMBER TO PARSE", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))
        val result: Result = await(mockControllersReferenceData.responseErrorHandler(p.future)(authenticatedRequest))

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.title"))
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.try.later"))
      }

      "show the default error page if the Upstream5xxResponse error omits the comma delimeter" in new Test {
        p.failure(UpstreamErrorResponse("""{appStatusMessage=10002}""", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))
        val result: Result = await(mockControllersReferenceData.responseErrorHandler(p.future)(authenticatedRequest))

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.title"))
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.try.later"))
      }

      "show the default error page when the Future completes with a GenericServerErrorException" in new Test {
        p.failure(new GenericServerErrorException("10003"))
        val result: Result = await(mockControllersReferenceData.responseErrorHandler(p.future)(authenticatedRequest))

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.title"))
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.try.later"))
      }

      "show the default error page when the Future completes with a GenericServerErrorException and empty message" in new Test {
        p.failure(new GenericServerErrorException(""))
        val result: Result = await(mockControllersReferenceData.responseErrorHandler(p.future)(authenticatedRequest))

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.title"))
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.try.later"))
      }

      "show an error page when the Future completes with a GenericServerErrorException" in new Test {
        p.failure(new GenericServerErrorException("63083"))
        val result: Result = await(mockControllersReferenceData.responseErrorHandler(p.future)(authenticatedRequest))

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ServiceMessage.63082.h1"))
      }

      "show the default error page if the GenericServerErrorException cannot be parsed" in new Test {
        p.failure(new GenericServerErrorException("NO JSON"))
        val result: Result = await(mockControllersReferenceData.responseErrorHandler(p.future)(authenticatedRequest))

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.title"))
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.try.later"))
      }

      "show the default error page when exception is unknown" in new Test {
        p.failure(new RuntimeException)
        val result: Result = await(mockControllersReferenceData.responseErrorHandler(p.future)(authenticatedRequest))

        result.header.status mustBe INTERNAL_SERVER_ERROR
        result.body.asInstanceOf[Strict].data.utf8String must include(messages("ErrorPage.title"))
      }
    }
  }
}
