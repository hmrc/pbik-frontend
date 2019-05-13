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

import config.{LocalFormPartialRetriever, PbikAppConfig, PbikContext}
import controllers.actions.{AuthAction, NoSessionCheckAction}
import javax.inject.Inject
import models._
import play.api.Mode.Mode
import play.api.Play.current
import play.api.i18n.Lang
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{LegacyI18nSupport, Result, _}
import play.api.{Configuration, Environment, Logger}
import services.BikListService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, UnauthorisedAction}
import utils.{ControllersReferenceData, SplunkLogger, URIInformation, _}

import scala.concurrent.Future
import scala.util.{Success, Try}

class HomePageController @Inject()(bikListService: BikListService,
                                   authenticate: AuthAction,
                                   val noSessionCheck: NoSessionCheckAction,
                                   implicit val pbikAppConfig: PbikAppConfig,
                                   val runModeConfiguration: Configuration,
                                   environment: Environment,
                                   implicit val taxDateUtils: TaxDateUtils,
                                   controllersReferenceData: ControllersReferenceData,
                                   splunkLogger: SplunkLogger,
                                   implicit val context: PbikContext,
                                   implicit val uRIInformation: URIInformation,
                                   implicit val externalURLs: ExternalUrls,
                                   implicit val localFormPartialRetriever: LocalFormPartialRetriever) extends FrontendController with LegacyI18nSupport {

  val mode: Mode = environment.mode

  def notAuthorised: Action[AnyContent] = authenticate {
    implicit request =>
      Ok(views.html.errorPage(controllersReferenceData.AUTHORISATION_ERROR, taxDateUtils.getTaxYearRange(), empRef = Some(request.empRef)))
  }

  def signout: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      Redirect(pbikAppConfig.serviceSignOut).withNewSession
  }

  def setLanguage: Action[AnyContent] = (authenticate andThen noSessionCheck) {
    implicit request =>
      val lang = request.getQueryString("lang").getOrElse("en")
      Logger.info("Language from request query is " + lang)
      val newLang = Lang(lang)
      Logger.info("New language set to " + newLang.code)
      Redirect(routes.HomePageController.onPageLoad()).withLang(newLang)
  }

  def loadCautionPageForCY: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val staticDataRequest: Future[Result] = Future.successful(Ok(views.html.registration.cautionAddCurrentTaxYear(controllersReferenceData.YEAR_RANGE, empRef = request.empRef)))
      controllersReferenceData.responseCheckCYEnabled(staticDataRequest)
  }

  def onPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val taxYearRange = taxDateUtils.getTaxYearRange()
      val pageLoadFuture = for {
        // Get the available count of biks available for each tax year
        biksListOptionCY: List[Bik] <- bikListService.registeredBenefitsList(controllersReferenceData.YEAR_RANGE.cyminus1, EmpRef("", ""))(uRIInformation.getBenefitTypesPath)
        biksListOptionCYP1: List[Bik] <- bikListService.registeredBenefitsList(controllersReferenceData.YEAR_RANGE.cy, EmpRef("", ""))(uRIInformation.getBenefitTypesPath)
        currentYearList: (Map[String, String], List[Bik]) <- bikListService.currentYearList
        nextYearList: (Map[String, String], List[Bik]) <- bikListService.nextYearList
      } yield {
        val fromYTA = if (request.session.get(controllersReferenceData.SESSION_FROM_YTA).isDefined) {
          request.session.get(controllersReferenceData.SESSION_FROM_YTA).get
        }
        else {
          isFromYTA
        }
        auditHomePageView
        Ok(views.html.overview(pbikAppConfig.cyEnabled, taxYearRange, currentYearList._2, nextYearList._2,
          biksListOptionCY.size, biksListOptionCYP1.size, fromYTA.toString, empRef = request.empRef))
          .addingToSession(nextYearList._1.toSeq: _*)
          .addingToSession(controllersReferenceData.SESSION_FROM_YTA -> fromYTA.toString)
      }
      controllersReferenceData.responseErrorHandler(pageLoadFuture)

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
    splunkLogger.logSplunkEvent(splunkLogger.createDataEvent(
      tier = splunkLogger.spTier.FRONTEND,
      action = splunkLogger.spAction.VIEW,
      target = splunkLogger.spTarget.BIK,
      period = splunkLogger.spPeriod.BOTH,
      msg = "Home page view",
      nino = None,
      iabd = None,
      name = Option(request.name),
      empRef = Some(request.empRef)
    ))
  }
}
