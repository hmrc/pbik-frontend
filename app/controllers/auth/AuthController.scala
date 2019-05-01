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

package controllers.auth

import config.PbikAppConfig
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models.AuthenticatedRequest
import play.api.Play
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.ControllersReferenceData

import scala.concurrent.Future

object AuthController extends AuthController with AuthenticationConnector {
  def pbikAppConfig: PbikAppConfig.type = PbikAppConfig
  val authenticate: AuthAction = Play.current.injector.instanceOf[AuthAction]
  val noSessionCheck: NoSessionCheckAction = Play.current.injector.instanceOf[NoSessionCheckAction]
}

trait AuthController extends FrontendController with Actions with ControllersReferenceData {

  val authenticate: AuthAction
  val noSessionCheck: NoSessionCheckAction

  def notAuthorised:Action[AnyContent] = authenticate.async {
    implicit request =>
      notAuthorisedResult
  }

  private[auth] def notAuthorisedResult(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    Future.successful(Ok(views.html.enrol(Some(request.empRef))))
  }
}
