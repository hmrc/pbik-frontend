/*
 * Copyright 2015 HM Revenue & Customs
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
import services.{EiLListService, BikListService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.controller.{UnauthorisedAction, FrontendController}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils._
import scala.concurrent.Future
import play.api.Play.configuration
import play.api.Play.current

object HomePageController extends HomePageController with TierConnector
with AuthenticationConnector {
  def pbikAppConfig = PbikAppConfig
  def bikListService = BikListService
  val tierConnector = new HmrcTierConnector
}

trait HomePageController extends FrontendController with URIInformation
with ControllersReferenceData with PbikActions with EpayeUser with SplunkLogger {

  this: TierConnector =>
  def bikListService: BikListService

  def notAuthorised:Action[AnyContent] = AuthenticatedBy(PBIKGovernmentGateway, pageVisibility = GGConfidence).async {
    implicit ac => implicit request =>
      Future.successful(Ok(views.html.errorPage(AUTHORISATION_ERROR, TaxDateUtils.getTaxYearRange())))
  }

  val SIGNEDOUT_MDTP_KEY = "mdtpsout"

  def signoutRedirectToDone = UnauthorisedAction {
    implicit request =>
      Redirect(configuration.getString("pbik.survey.url").getOrElse("")).withNewSession
        .discardingCookies(DiscardingCookie(SIGNEDOUT_MDTP_KEY))
  }

  def redirectToDone = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        Future.successful(Redirect(configuration.getString("pbik.survey.url").getOrElse("")))
  }

  def loadCautionPageForCY:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val staticDataRequest: Future[Result] = Future.successful(Ok(views.html.registration.cautionAddCurrentTaxYear(YEAR_RANGE)))
        responseCheckCYEnabled(staticDataRequest)
  }

  def onPageLoad:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
          val taxYearRange = TaxDateUtils.getTaxYearRange()
          val pageLoadFuture = for {
            currentYearList: (Map[String, String], List[Bik]) <- bikListService.currentYearList
            nextYearList: (Map[String, String], List[Bik]) <- bikListService.nextYearList
          } yield {
            auditHomePageView
            Ok(views.html.overview(pbikAppConfig.cyEnabled, taxYearRange, currentYearList._2, nextYearList._2, pbikAppConfig.biksCount))
              .addingToSession(nextYearList._1.toSeq: _*)
          }
          responseErrorHandler(pageLoadFuture)
  }


  def auditHomePageView(implicit hc:HeaderCarrier, ac: AuthContext) = {
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
