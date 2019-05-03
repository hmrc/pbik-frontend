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

package controllers.actions

import java.util.UUID

import com.google.inject.ImplementedBy
import models.AuthenticatedRequest
import play.api.mvc._
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class NoSessionCheckActionImpl extends NoSessionCheckAction {
  override def invokeBlock[A](request: AuthenticatedRequest[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    val sessionId = request.session.get(SessionKeys.sessionId)
    sessionId match {
      case None => Future.successful(
        Results.Redirect(controllers.routes.HomePageController.onPageLoad()).withSession(
                          request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
      )
      case _ => {
        block(request)
      }
    }
  }
}

@ImplementedBy(classOf[NoSessionCheckActionImpl])
trait NoSessionCheckAction extends ActionFunction[AuthenticatedRequest, AuthenticatedRequest]
