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

import _root_.models._
import config.PbikAppConfig
import connectors.{HmrcTierConnector, TierConnector}
import controllers.auth._
import play.api.Logger
import play.api.mvc.{Result, _}
import services.{BikListService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.controller.{UnauthorisedAction, FrontendController}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils._
import scala.concurrent.Future
import play.api.Play.configuration
import play.api.Play.current
import play.api.i18n.Lang

import scala.util.{Success, Try}

object HomePageController extends HomePageController with TierConnector
with AuthenticationConnector {
  def pbikAppConfig = PbikAppConfig
  def bikListService = BikListService
  val tierConnector = new HmrcTierConnector
}

trait HomePageController extends FrontendController with URIInformation
with ControllersReferenceData with PbikActions with EpayeUser with SplunkLogger  {

  this: TierConnector =>
  def bikListService: BikListService

  def notAuthorised:Action[AnyContent] = AuthenticatedBy(PBIKGovernmentGateway, pageVisibility = GGConfidence).async {
    implicit ac => implicit request =>
      Future.successful(Ok(views.html.errorPage(AUTHORISATION_ERROR, TaxDateUtils.getTaxYearRange())))
  }

  def signout: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      Redirect(controllers.routes.QuestionnaireController.showQuestionnaire).withNewSession
  }

  def setLanguage:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val lang = request.getQueryString("lang").getOrElse("en")
        Logger.info("Language from request query is " + lang)
        implicit val newLang = Lang(lang)
        Logger.info("New language set to " + newLang.code)
        Future.successful(Redirect(routes.HomePageController.onPageLoad).withLang(newLang))
  }

  def loadCautionPageForCY:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val staticDataRequest: Future[Result] = Future.successful(Ok(views.html.registration.cautionAddCurrentTaxYear(YEAR_RANGE)))
        responseCheckCYEnabled(staticDataRequest)
  }

//  def setLanguage(body: Request): Action[AnyContent] = AuthorisedFor(getAuthorisedForPolicy, pageVisibility = GGConfidence).async {
//    implicit ac => implicit request =>
//     request.add
//  }

  def onPageLoad:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>

        val taxYearRange = TaxDateUtils.getTaxYearRange()
        val pageLoadFuture = for {
          // Get the available count of biks available for each tax year
          biksListOptionCY:List[Bik] <- bikListService.registeredBenefitsList(YEAR_RANGE.cyminus1, "")(getBenefitTypesPath)
          biksListOptionCYP1:List[Bik] <- bikListService.registeredBenefitsList(YEAR_RANGE.cy, "")(getBenefitTypesPath)
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
            biksListOptionCY.size, biksListOptionCYP1.size, fromYTA.toString))
            .addingToSession(nextYearList._1.toSeq: _*).addingToSession(SESSION_FROM_YTA -> fromYTA.toString)
        }
        responseErrorHandler(pageLoadFuture)

  }

  def isFromYTA(implicit request: Request[_]): Boolean = {
    val refererUrl = Try(request.headers("referer"))
    refererUrl match {
      case Success(url) if(url.endsWith("/business-account"))=> true
      case Success(url) if(url.endsWith("/account"))=> true
      case _ => false
    }
  }

  def auditHomePageView(implicit hc:HeaderCarrier, ac: AuthContext): Future[AuditResult] = {
      logSplunkEvent(createDataEvent(
        tier=spTier.FRONTEND,
        action=spAction.VIEW,
        target=spTarget.BIK,
        period=spPeriod.BOTH,
        msg="Home page view",
        nino=None,
        iabd=None))
  }
}
