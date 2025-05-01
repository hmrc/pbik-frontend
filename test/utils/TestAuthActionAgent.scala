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

import controllers.actions.AuthAction
import models.agent.{AccountsOfficeReference, Client}
import models.auth.AuthenticatedRequest
import play.api.mvc.Results._
import play.api.mvc.{BodyParsers, Request, Result}
import uk.gov.hmrc.domain.EmpRef

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestAuthActionAgent @Inject() (val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
    extends AuthAction {

  private val empRef: EmpRef              = EmpRef("123", "AB12345")
  private val agentClient: Option[Client] = Some(
    Client(
      uk.gov.hmrc.domain.EmpRef(empRef.taxOfficeNumber, empRef.taxOfficeReference),
      AccountsOfficeReference("123AB12345678", "123", "A", "12345678"),
      Some("client test name"),
      lpAuthorisation = false,
      None,
      None
    )
  )

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    if (request.session.get("sessionId").getOrElse("").startsWith("session")) {
      implicit val authenticatedRequest: AuthenticatedRequest[A] =
        AuthenticatedRequest(
          empRef,
          Some("tester"),
          request,
          agentClient
        )
      block(authenticatedRequest)
    } else {
      Future(Unauthorized("Request was not authenticated user should be redirected"))
    }

}
