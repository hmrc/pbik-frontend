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

package controllers

import base.FakePBIKApplication
import config.PbikAppConfig
import models.v1.IabdType
import models.v1.IabdType.IabdType
import models.{PbikSession, RegistrationItem, RegistrationList}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.DefaultSessionRepository
import services.SessionService
import uk.gov.hmrc.mongo.MongoComponent
import views.html.{IndividualSignedOut, SignedOut}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class SignedOutControllerSpec extends FakePBIKApplication {

  implicit val ec: ExecutionContextExecutor                    = ExecutionContext.global
  private val messagesActionBuilder: MessagesActionBuilder     =
    new DefaultMessagesActionBuilderImpl(stubBodyParser[AnyContent](), stubMessagesApi())
  private val cc: ControllerComponents                         = stubControllerComponents()
  private val mockMCC: MessagesControllerComponents            = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication.injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ec
  )
  protected def mongoComponent: MongoComponent                 = injected[MongoComponent]
  implicit val appConfig: PbikAppConfig                        = injected[PbikAppConfig]
  private val mockSessionService: SessionService               = mock(classOf[SessionService])
  private val mockSessionRepository: DefaultSessionRepository  = mock(classOf[DefaultSessionRepository])
  private val signedOutView: SignedOut                         = injected[SignedOut]
  private val individualSignedOutView: IndividualSignedOut     = injected[IndividualSignedOut]
  private val signedOutController                              = new SignedOutController(
    signedOutView,
    individualSignedOutView,
    mockMCC,
    mockSessionRepository,
    mockSessionService,
    ec
  )
  private val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/")
  private val iabdType: IabdType                               = IabdType.MedicalInsurance
  private val pbikSession                                      = PbikSession(
    sessionId,
    Some(RegistrationList(active = List(RegistrationItem(iabdType, active = true, enabled = true)))),
    None,
    None,
    None,
    None,
    None,
    None
  )

  "SignedOutController" when {
    "keepAlive" must {
      "return 200 OK and keep session alive when session exists" in {
        when(mockSessionService.fetchPbikSession()(any())).thenReturn(Future.successful(Some(pbikSession)))
        when(mockSessionRepository.upsert(any[PbikSession])).thenReturn(Future.successful(pbikSession))

        val result = signedOutController.keepAlive()(fakeRequest)
        status(result) mustEqual OK
        contentAsString(result) mustEqual "Session kept alive"
      }

      "return InternalServerError when upsert fails" in {
        when(mockSessionService.fetchPbikSession()(any())).thenReturn(Future.successful(Some(pbikSession)))
        when(mockSessionRepository.upsert(any[PbikSession]))
          .thenReturn(Future.failed(new RuntimeException("Mongo error")))

        val result = signedOutController.keepAlive.apply(FakeRequest())
        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Could not extend session due to a server error")
      }

      "return 401 Unauthorized when session is invalid or expired" in {
        when(mockSessionService.fetchPbikSession()(any()))
          .thenReturn(Future.successful(None))
        val result = signedOutController.keepAlive()(fakeRequest)
        status(result) mustEqual UNAUTHORIZED
        contentAsString(result) mustEqual "Invalid or expired session"
      }
    }

    "signedOut" must {
      "return OK" in {
        status(signedOutController.signedOut().apply(fakeRequest)) mustBe OK
      }
    }

    "individualSignedOut" must {
      "return OK" in {
        status(signedOutController.individualSignedOut.apply(fakeRequest)) mustBe OK
      }
    }
  }
}
