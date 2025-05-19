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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.SignedOut
import views.html.IndividualSignedOut
import repositories.DefaultSessionRepository
import services.SessionService
import play.api.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignedOutController @Inject() (
  signedOutView: SignedOut,
  individualSignedOutView: IndividualSignedOut,
  val mcc: MessagesControllerComponents,
  sessionRepository: DefaultSessionRepository,
  sessionService: SessionService,
  implicit val ec: ExecutionContext
) extends FrontendController(mcc)
    with Logging {

  def signedOut: Action[AnyContent] = Action { implicit request =>
    Ok(signedOutView())
  }

  def individualSignedOut: Action[AnyContent] = Action { implicit request =>
    Ok(individualSignedOutView())
  }

  def keepAlive: Action[AnyContent] = Action.async { implicit request =>
    sessionService.fetchPbikSession().flatMap {
      case Some(session) =>
        sessionRepository
          .upsert(session)
          .map { _ =>
            Ok("Session kept alive").withSession(request.session)
          }
          .recover { case ex =>
            logger.error("Session upsert failed", ex)
            InternalServerError("Could not extend session due to a server error")
          }
      case None          =>
        Future.successful(Unauthorized("Invalid or expired session"))
    }
  }
}
