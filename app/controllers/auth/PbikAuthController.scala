/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.{Logger, Play}
import play.api.mvc.{Action, Result, AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.config.RunMode
import utils.TaxDateUtils
import scala.concurrent.Future
import controllers.ExternalUrls
import uk.gov.hmrc.play.http.SessionKeys
import java.util.UUID
import uk.gov.hmrc.play.frontend.auth.{AuthContext, Actions, TaxRegime, GovernmentGateway}

object PBIKGovernmentGateway extends GovernmentGateway {
  override val loginURL = ExternalUrls.signIn
  override val continueURL = ExternalUrls.continue
}

object PBIKEpayeRegime extends TaxRegime {

  override def isAuthorised(accounts: Accounts):Boolean = accounts.epaye.isDefined

  override val unauthorisedLandingPage = Some(routes.AuthController.notAuthorised().url)

  override val authenticationType = PBIKGovernmentGateway
}

trait PbikActions extends Actions {

  self: EpayeUser =>

  def getAuthorisedForPolicy:TaxRegime = PBIKEpayeRegime

  private type AsyncPlayUserRequest = AuthContext => (Request[AnyContent] => Future[Result])

  def AuthorisedForPbik(body: AsyncPlayUserRequest): Action[AnyContent] = AuthorisedFor(getAuthorisedForPolicy, pageVisibility = GGConfidence).async {
    implicit ac => implicit request =>
        noSessionCheck(body)
  }

  def noSessionCheck(body: AsyncPlayUserRequest)(implicit ac: AuthContext, request: Request[AnyContent]):Future[Result]  = {

    val sessionId = request.session.get(SessionKeys.sessionId )
    sessionId match {
      case None => Future.successful(Redirect(controllers.routes.HomePageController.onPageLoad()).withSession(
        request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}")))
      case _ => {
        body(ac)(request)
      }
    }
  }

}
