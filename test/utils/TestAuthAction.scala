/*
 * Copyright 2024 HM Revenue & Customs
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
import models.{AuthenticatedRequest, EmpRef, UserName}
import play.api.mvc.Results._
import play.api.mvc.{BodyParsers, Request, Result}
import uk.gov.hmrc.auth.core.retrieve.Name

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestAuthAction @Inject() (val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
    extends AuthAction {
  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    if (request.session.get("sessionId").getOrElse("").startsWith("session")) {
      implicit val authenticatedRequest: AuthenticatedRequest[A] =
        AuthenticatedRequest(
          EmpRef("taxOfficeNumber", "taxOfficeReference"),
          UserName(Name(None, None)),
          request
        )
      block(authenticatedRequest)
    } else {
      Future(Unauthorized("Request was not authenticated user should be redirected"))
    }

}
