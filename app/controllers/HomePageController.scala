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
import controllers.actions.{AuthAction, NoSessionCheckAction, UnauthorisedAction}
import javax.inject.Inject
import models._
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.{Result, _}
import play.api.{Configuration, Environment, Logger}
import services.BikListService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{ControllersReferenceData, SplunkLogger, URIInformation, _}
import views.html.{ErrorPage, Overview}
import views.html.registration.CautionAddCurrentTaxYear

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Success, Try}

class HomePageController @Inject()(override val messagesApi: MessagesApi,
                                   cc: MessagesControllerComponents,
                                   bikListService: BikListService,
                                   authenticate: AuthAction,
                                   val noSessionCheck: NoSessionCheckAction,
                                   unauthorisedAction: UnauthorisedAction,
                                   controllersReferenceData: ControllersReferenceData,
                                   splunkLogger: SplunkLogger,
                                   taxDateUtils: TaxDateUtils,
                                   pbikAppConfig: PbikAppConfig,
                                   uriInformation: URIInformation,
                                   errorPageView: ErrorPage,
                                   cautionAddCurrentTaxYearView: CautionAddCurrentTaxYear,
                                   overviewView: Overview)(implicit val ec: ExecutionContextExecutor) extends FrontendController(cc) with I18nSupport {

  def notAuthorised: Action[AnyContent] = authenticate {
    implicit request =>
      Ok(errorPageView(ControllersReferenceDataCodes.AUTHORISATION_ERROR, taxDateUtils.getTaxYearRange(), empRef = Some(request.empRef)))
  }

  def signout: Action[AnyContent] = unauthorisedAction {
    implicit request =>
      Redirect(pbikAppConfig.serviceSignOut).withNewSession
  }

  def setLanguage: Action[AnyContent] = (authenticate andThen noSessionCheck) {
    implicit request =>
      val lang = request.getQueryString("lang").getOrElse("en")
      Logger.info("Language from request query is " + lang)
      val newLang = Lang(lang)
      Logger.info("New language set to " + newLang.code)
      Redirect(routes.HomePageController.onPageLoad()).withLang(newLang)(messagesApi)
  }

  def loadCautionPageForCY: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val staticDataRequest: Future[Result] = Future.successful(Ok(cautionAddCurrentTaxYearView(controllersReferenceData.YEAR_RANGE, empRef = request.empRef)))
      controllersReferenceData.responseCheckCYEnabled(staticDataRequest)
  }

  def onPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val taxYearRange = taxDateUtils.getTaxYearRange()
      val pageLoadFuture = for {
        // Get the available count of biks available for each tax year
        biksListOptionCY: List[Bik] <- bikListService.registeredBenefitsList(controllersReferenceData.YEAR_RANGE.cyminus1, EmpRef("", ""))(uriInformation.getBenefitTypesPath)
        biksListOptionCYP1: List[Bik] <- bikListService.registeredBenefitsList(controllersReferenceData.YEAR_RANGE.cy, EmpRef("", ""))(uriInformation.getBenefitTypesPath)
        currentYearList: (Map[String, String], List[Bik]) <- bikListService.currentYearList
        nextYearList: (Map[String, String], List[Bik]) <- bikListService.nextYearList
      } yield {
        val fromYTA = if (request.session.get(ControllersReferenceDataCodes.SESSION_FROM_YTA).isDefined) {
          request.session.get(ControllersReferenceDataCodes.SESSION_FROM_YTA).get
        }
        else {
          isFromYTA
        }
        auditHomePageView()
        Ok(overviewView(pbikAppConfig.cyEnabled, taxYearRange, currentYearList._2, nextYearList._2,
          biksListOptionCY.size, biksListOptionCYP1.size, fromYTA.toString, empRef = request.empRef))
          .addingToSession(nextYearList._1.toSeq: _*)
          .addingToSession(ControllersReferenceDataCodes.SESSION_FROM_YTA -> fromYTA.toString)
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

  def auditHomePageView()(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[AuditResult] = {
    splunkLogger.logSplunkEvent(splunkLogger.createDataEvent(
      tier = splunkLogger.FRONTEND,
      action = splunkLogger.VIEW,
      target = splunkLogger.BIK,
      period = splunkLogger.BOTH,
      msg = "Home page view",
      nino = None,
      iabd = None,
      name = Option(request.name),
      empRef = Some(request.empRef)
    ))
  }
}
