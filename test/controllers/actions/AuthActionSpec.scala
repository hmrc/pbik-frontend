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

package controllers.actions

import base.FakePBIKApplication
import connectors.AgentPayeConnector
import controllers.actions.AuthActionSpec.AuthRetrievals
import controllers.actions.AuthConnector
import models.auth.EpayeSessionKeys
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionSpec extends FakePBIKApplication {

  private val enrolment: Enrolment = Enrolment(
    key = "IR-PAYE",
    identifiers = Seq(
      EnrolmentIdentifier("TaxOfficeNumber", "840"),
      EnrolmentIdentifier("TaxOfficeReference", "MODESTE47")
    ),
    state = "activated"
  )

  private val mockAgentPayeConnector: AgentPayeConnector                      = mock(classOf[AgentPayeConnector])
  private val fakeRequestForOrganisation: FakeRequest[AnyContentAsEmpty.type] = mockRequest

  private val fakeRequestForAgent: FakeRequest[AnyContentAsEmpty.type] =
    mockRequest.withSession(EpayeSessionKeys.AGENT_FRONTEND_EMPREF -> "123/AB12345")

  private class Harness(authAction: AuthAction, cc: ControllerComponents = Helpers.stubMessagesControllerComponents())
      extends AbstractController(cc) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Ok)
  }

  private class Test(enrolment: Enrolment) {
    private type RetrievalType = Option[AffinityGroup] ~ Enrolments ~ Option[String] ~ Option[String]

    private val mockAuthConnector: AuthConnector = mock(classOf[AuthConnector])

    def retrievals(
      affinityGroup: Option[AffinityGroup],
      enrolments: Enrolments = Enrolments(Set(enrolment)),
      id: Option[String] = Some("internal_id"),
      agentCode: Option[String] = Some("agentcode1")
    ): Harness = {

      when(mockAuthConnector.authorise[RetrievalType](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(
            affinityGroup composeRetrievals enrolments composeRetrievals id composeRetrievals agentCode
          )
        )

      val authAction = new AuthActionImpl(
        authConnector = mockAuthConnector,
        parser = bodyParsers,
        config = pbikAppConfig,
        agentPayeConnector = mockAgentPayeConnector
      )

      new Harness(authAction)
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockAgentPayeConnector)

    when(mockAgentPayeConnector.getClient(any(), any())(any(), any()))
      .thenReturn(Future.successful(None))
  }

  "AuthAction" when {

    ".authAsEmployer" when {
      "the user is logged in with valid credentials" must {
        "return OK" in new Test(enrolment) {
          val controller: Harness    = retrievals(affinityGroup = Some(AffinityGroup.Organisation))
          val result: Future[Result] = controller.onPageLoad()(fakeRequestForOrganisation)

          status(result) mustBe OK
        }
      }

      "the user with no internal_id is logged in with valid credentials" must {
        "return OK" in new Test(enrolment) {
          val controller: Harness    = retrievals(affinityGroup = Some(AffinityGroup.Organisation), id = None)
          val result: Future[Result] = controller.onPageLoad()(fakeRequestForOrganisation)

          status(result) mustBe OK
        }
      }

      "the user tries to log in with no identifiers" must {
        val enrolmentWithNoIdentifiers = enrolment.copy(identifiers = Seq.empty)

        "redirect the user to home page" in new Test(enrolmentWithNoIdentifiers) {
          val controller: Harness    = retrievals(affinityGroup = Some(AffinityGroup.Organisation))
          val result: Future[Result] = controller.onPageLoad()(fakeRequestForOrganisation)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.AuthController.notAuthorised.url
        }
      }

      "the user tries to log in with an invalid enrolment key" must {
        val enrolmentWithInvalidEnrolmentKey = enrolment.copy(key = "IR")

        "redirect the user to home page" in new Test(enrolmentWithInvalidEnrolmentKey) {
          val controller: Harness    = retrievals(affinityGroup = Some(AffinityGroup.Organisation))
          val result: Future[Result] = controller.onPageLoad()(fakeRequestForOrganisation)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.AuthController.notAuthorised.url
        }
      }

      "the user is not logged in" must {
        "redirect the user to log in" in {
          val authAction = new AuthActionImpl(
            new BrokenAuthConnector(
              new MissingBearerToken,
              mock(classOf[HttpClientV2]),
              pbikAppConfig
            ),
            bodyParsers,
            pbikAppConfig,
            mockAgentPayeConnector
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(fakeRequestForOrganisation)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must endWith(
            "sign-in?continue_url=http%3A%2F%2Flocalhost%3A9233%2Fpayrollbik%2Fstart-payrolling-benefits-expenses&origin=pbik-frontend"
          )

        }
      }

      "the user has an Insufficient Enrolments" must {
        "redirect the user to a page to enroll" in {
          val authAction = new AuthActionImpl(
            new BrokenAuthConnector(
              InsufficientEnrolments("Insufficient enrolments test exception"),
              mock(classOf[HttpClientV2]),
              pbikAppConfig
            ),
            bodyParsers,
            pbikAppConfig,
            mockAgentPayeConnector
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(fakeRequestForOrganisation)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.AuthController.notAuthorised.url
        }
      }
    }

    ".authAsAgent" when {
      "the user is logged in with valid empRef headers" must {
        "return OK" in new Test(enrolment) {
          val controller: Harness    = retrievals(affinityGroup = Some(AffinityGroup.Agent))
          val result: Future[Result] = controller.onPageLoad()(fakeRequestForAgent)

          status(result) mustBe OK
        }
      }

      "the user with no name is logged in with valid credentials" must {
        "return OK" in new Test(enrolment) {
          val controller: Harness    = retrievals(affinityGroup = Some(AffinityGroup.Agent), id = None)
          val result: Future[Result] = controller.onPageLoad()(fakeRequestForAgent)

          status(result) mustBe OK
        }
      }

      "the user tries to log in with no empRef header" must {
        val enrolmentWithNoIdentifiers = enrolment.copy(identifiers = Seq.empty)

        "redirect the user to home page" in new Test(enrolmentWithNoIdentifiers) {
          val controller: Harness    = retrievals(affinityGroup = Some(AffinityGroup.Agent))
          val result: Future[Result] = controller.onPageLoad()(fakeRequestForOrganisation)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe pbikAppConfig.agentClientListUrl
        }
      }

      "the user tries to log in with empty empRef header" must {
        val enrolmentWithNoIdentifiers = enrolment.copy(identifiers = Seq.empty)

        "redirect the user to home page" in new Test(enrolmentWithNoIdentifiers) {
          val controller: Harness    = retrievals(affinityGroup = Some(AffinityGroup.Agent))
          val result: Future[Result] = controller.onPageLoad()(
            mockRequest
              .withSession(
                EpayeSessionKeys.AGENT_FRONTEND_EMPREF -> ""
              )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe pbikAppConfig.agentClientListUrl
        }
      }

      "the user tries to log in with invalid empRef header" must {
        val enrolmentWithNoIdentifiers = enrolment.copy(identifiers = Seq.empty)

        "redirect the user to home page" in new Test(enrolmentWithNoIdentifiers) {
          val controller: Harness    = retrievals(affinityGroup = Some(AffinityGroup.Agent))
          val result: Future[Result] = controller.onPageLoad()(
            mockRequest
              .withSession(
                EpayeSessionKeys.AGENT_FRONTEND_EMPREF -> "123/"
              )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe pbikAppConfig.agentClientListUrl
        }
      }

      "the user is not logged in" must {
        "redirect the user to log in" in {
          val authAction = new AuthActionImpl(
            new BrokenAuthConnector(
              new MissingBearerToken,
              mock(classOf[HttpClientV2]),
              pbikAppConfig
            ),
            bodyParsers,
            pbikAppConfig,
            mockAgentPayeConnector
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(fakeRequestForAgent)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must endWith(
            "sign-in?continue_url=http%3A%2F%2Flocalhost%3A9233%2Fpayrollbik%2Fstart-payrolling-benefits-expenses&origin=pbik-frontend"
          )
        }
      }

      "the user has not enough enrolment" must {
        "redirect the user to not authorised" in {
          val authAction = new AuthActionImpl(
            new BrokenAuthConnector(
              InsufficientEnrolments("Test exception"),
              mock(classOf[HttpClientV2]),
              pbikAppConfig
            ),
            bodyParsers,
            pbikAppConfig,
            mockAgentPayeConnector
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(fakeRequestForAgent)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.AuthController.notAuthorised.url
        }
      }
    }

    ".authAsIndividual" when {
      "the user is Individual" must {
        "redirect the user to individual error page" in new Test(enrolment) {
          val authAction = new AuthActionImpl(
            new BrokenAuthConnector(
              UnsupportedAffinityGroup("Test exception"),
              mock(classOf[HttpClientV2]),
              pbikAppConfig
            ),
            bodyParsers,
            pbikAppConfig,
            mockAgentPayeConnector
          )
          val controller = retrievals(affinityGroup = Some(AffinityGroup.Individual))
          val result     = controller.onPageLoad()(fakeRequestForAgent)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.AuthController.affinityIndividual.url
        }
      }
    }

    "no affinity group" when {
      "the user" must {
        "return exception as it is not supported" in new Test(enrolment) {
          val controller: Harness    = retrievals(affinityGroup = None)
          val result: Future[Result] = controller.onPageLoad()(fakeRequestForAgent)

          await(result.failed).getMessage mustBe "AffinityGroup not found"
        }
      }
    }
  }

}

object AuthActionSpec {

  implicit class AuthRetrievals[A](a: A) {
    def composeRetrievals[B](b: B): ~[A, B] = new ~(a, b)
  }

}
