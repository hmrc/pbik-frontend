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

package controllers

import config.{PbikAppConfig, PbikContext}
import controllers.actions.MinimalAuthAction
import javax.inject.Inject
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class AuthController @Inject()(val pbikAppConfig: PbikAppConfig,
                               val authenticate: MinimalAuthAction,
                               implicit val context: PbikContext,
                               implicit val externalUrls: ExternalUrls) extends FrontendController {

  def notAuthorised:Action[AnyContent] = authenticate.async {
    implicit request =>
      notAuthorisedResult
  }

  private def notAuthorisedResult(implicit request: Request[AnyContent]): Future[Result] = {
    Future.successful(Ok(views.html.enrol(None)))
  }
}
