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

import config.PbikAppConfig
import connectors.{HmrcTierConnector, TierConnector}
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models._
import play.api.Play.current
import play.api.i18n.Lang
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{LegacyI18nSupport, Result, _}
import play.api.{Logger, Play}
import services.BikListService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import utils._

import scala.concurrent.Future
import scala.util.{Success, Try}

object HomePageController extends HomePageController with TierConnector {
  def pbikAppConfig = PbikAppConfig

  def bikListService = BikListService

  val tierConnector = new HmrcTierConnector
  val authenticate: AuthAction = Play.current.injector.instanceOf[AuthAction]
  val noSessionCheck: NoSessionCheckAction = Play.current.injector.instanceOf[NoSessionCheckAction]
}

trait HomePageController extends FrontendController
  with URIInformation
  with ControllersReferenceData
  with SplunkLogger
  with LegacyI18nSupport {
  this: TierConnector =>

  def bikListService: BikListService
  val authenticate: AuthAction
  val noSessionCheck: NoSessionCheckAction

  def notAuthorised: Action[AnyContent] = authenticate {
    implicit request =>
      Ok(views.html.errorPage(AUTHORISATION_ERROR, TaxDateUtils.getTaxYearRange(), empRef = Some(request.empRef)))
  }

  def signout: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      Redirect(PbikAppConfig.serviceSignOut).withNewSession
  }

  def setLanguage: Action[AnyContent] = (authenticate andThen noSessionCheck) {
    implicit request =>
      val lang = request.getQueryString("lang").getOrElse("en")
      Logger.info("Language from request query is " + lang)
      val newLang = Lang(lang)
      Logger.info("New language set to " + newLang.code)
      Redirect(routes.HomePageController.onPageLoad).withLang(newLang)
  }

  def loadCautionPageForCY: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val staticDataRequest: Future[Result] = Future.successful(Ok(views.html.registration.cautionAddCurrentTaxYear(YEAR_RANGE, empRef = request.empRef)))
      responseCheckCYEnabled(staticDataRequest)
  }

  def onPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val taxYearRange = TaxDateUtils.getTaxYearRange()
      val pageLoadFuture = for {
        // Get the available count of biks available for each tax year
        biksListOptionCY: List[Bik] <- bikListService.registeredBenefitsList(YEAR_RANGE.cyminus1, EmpRef("", ""))(getBenefitTypesPath)
        biksListOptionCYP1: List[Bik] <- bikListService.registeredBenefitsList(YEAR_RANGE.cy, EmpRef("", ""))(getBenefitTypesPath)
        currentYearList: (Map[String, String], List[Bik]) <- bikListService.currentYearList
        nextYearList: (Map[String, String], List[Bik]) <- bikListService.nextYearList

      } yield {
        val fromYTA = if (request.session.get(SESSION_FROM_YTA).isDefined) {
          request.session.get(SESSION_FROM_YTA).get
        }
        else {
          isFromYTA
        }
        auditHomePageView
        Ok(views.html.overview(pbikAppConfig.cyEnabled, taxYearRange, currentYearList._2, nextYearList._2,
          biksListOptionCY.size, biksListOptionCYP1.size, fromYTA.toString, empRef = request.empRef))
          .addingToSession(nextYearList._1.toSeq: _*)
          .addingToSession(SESSION_FROM_YTA -> fromYTA.toString)
      }
      responseErrorHandler(pageLoadFuture)

  }

  def isFromYTA(implicit request: Request[_]): Boolean = {
    val refererUrl = Try(request.headers("referer"))
    refererUrl match {
      case Success(url) if url.endsWith("/business-account") => true
      case Success(url) if url.endsWith("/account") => true
      case _ => false
    }
  }

  def auditHomePageView(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[AuditResult] = {
    logSplunkEvent(createDataEvent(
      tier = spTier.FRONTEND,
      action = spAction.VIEW,
      target = spTarget.BIK,
      period = spPeriod.BOTH,
      msg = "Home page view",
      nino = None,
      iabd = None,
      name = Option(request.name),
      empRef = Some(request.empRef)
    ))
  }
}
