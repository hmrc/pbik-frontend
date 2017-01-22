/*
 * Copyright 2017 HM Revenue & Customs
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

import audit.Auditable
import config.{PbikAppConfig, PbikFrontendAuditConnector}
import connectors.{HmrcTierConnector, TierConnector}
import controllers.auth.AuthenticationConnector
import models.Questionnaire
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent}
import services.BikListService
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.ControllersReferenceData

object QuestionnaireController extends QuestionnaireController with TierConnector
  with AuthenticationConnector {
  def pbikAppConfig = PbikAppConfig
  def bikListService = BikListService
  val tierConnector = new HmrcTierConnector

  val audit: Audit = new Audit(s"PBIK:${AppName.appName}-Questionnaire", PbikFrontendAuditConnector)
  val appName: String = AppName.appName
}

trait QuestionnaireController extends FrontendController with Auditable with ControllersReferenceData {

  def showQuestionnaire: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      Ok(views.html.questionnaire.feedbackQuestionnaire(Questionnaire.form))
  }

  def submitQuestionnaire: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      Questionnaire.form.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(views.html.questionnaire.feedbackQuestionnaire(formWithErrors))
        },
        value => {
          auditQuestionnaire(value)
          Redirect(routes.QuestionnaireController.feedbackThankYou())
        }
      )
  }

  private def auditQuestionnaire(value: Questionnaire)(implicit hc: HeaderCarrier) = {
    sendDataEvent("pbik-exit-survey", detail = Map(
      "satisfactionLevel" -> value.satisfactionLevel.mkString,
      "howCanWeImprove" -> value.howCanWeImprove.mkString
    ))
  }

  def feedbackThankYou: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      Ok(views.html.questionnaire.feedbackThankYou())
  }

  def setLanguage: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      val lang = request.getQueryString("lang").getOrElse("en")
      Logger.info("Language from request query is " + lang)
      implicit val newLang = Lang(lang)
      Logger.info("New language set to " + newLang.code)
      Redirect(routes.QuestionnaireController.showQuestionnaire()).withLang(newLang)
  }
}