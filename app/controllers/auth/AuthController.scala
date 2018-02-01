/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import scala.concurrent.Future
import utils.{ControllersReferenceData, TaxDateUtils}
import play.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}

object AuthController extends AuthController with AuthenticationConnector {
  def pbikAppConfig = PbikAppConfig
}

trait AuthController extends FrontendController with Actions with ControllersReferenceData {

  def notAuthorised:Action[AnyContent] = AuthenticatedBy(PBIKGovernmentGateway, pageVisibility = GGConfidence).async {
    implicit ac => implicit request =>
      notAuthorisedResult
  }

  private[auth] def notAuthorisedResult(implicit request: Request[AnyContent], ac: AuthContext) = {
    Future.successful(Ok(views.html.enrol()))
  }

}
