/*
 * Copyright 2026 HM Revenue & Customs
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

import controllers.actions.{AuthAction, NoSessionCheckAction}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.controller.WithUrlEncodedOnlyFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ControllersReferenceData
import views.html.StartPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartPageController @Inject() (
  cc: MessagesControllerComponents,
  authenticate: AuthAction,
  noSessionCheck: NoSessionCheckAction,
  controllersReferenceData: ControllersReferenceData,
  startPageView: StartPage
)(implicit val ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport
    with Logging
    with WithUrlEncodedOnlyFormBinding {

  def onPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck).async { implicit request =>
    val resultFuture: Future[Result] =
      Future.successful(Ok(startPageView()))
    controllersReferenceData.responseErrorHandler(resultFuture)
  }
}
