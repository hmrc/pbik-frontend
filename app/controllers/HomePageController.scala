/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.actions.{AuthAction, NoSessionCheckAction, UnauthorisedAction}
import models._
import play.api.Logging
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc._
import services.{BikListService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._
import views.html.{ErrorPage, Summary}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class HomePageController @Inject() (
  override val messagesApi: MessagesApi,
  cc: MessagesControllerComponents,
  bikListService: BikListService,
  sessionService: SessionService,
  authenticate: AuthAction,
  val noSessionCheck: NoSessionCheckAction,
  unauthorisedAction: UnauthorisedAction,
  controllersReferenceData: ControllersReferenceData,
  splunkLogger: SplunkLogger,
  taxDateUtils: TaxDateUtils,
  pbikAppConfig: PbikAppConfig,
  errorPageView: ErrorPage,
  summaryPage: Summary
)(implicit val ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport
    with Logging {

  def notAuthorised: Action[AnyContent] = authenticate { implicit request =>
    Unauthorized(
      errorPageView(
        ControllersReferenceDataCodes.AUTHORISATION_ERROR,
        taxDateUtils.getTaxYearRange(),
        empRef = Some(request.empRef)
      )
    )
  }

  def signout: Action[AnyContent] = unauthorisedAction {
    Redirect(pbikAppConfig.signOut)
  }

  def setLanguage(): Action[AnyContent] = Action { implicit request =>
    val lang    = request.getQueryString("lang").getOrElse("en")
    logger.info(s"[HomePageController][setLanguage] Request received: set language to $lang")
    val newLang = Lang(lang)
    Redirect(
      request.headers.toMap
        .getOrElse("Referer", List("https://www.tax.service.gov.uk/payrollbik/payrolled-benefits-expenses"))
        .asInstanceOf[List[String]]
        .head
    )
      .withLang(newLang)(messagesApi)
  }

  def onPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck).async { implicit request =>
    val taxYearRange: TaxYearRange     = taxDateUtils.getTaxYearRange()
    val pageLoadFuture: Future[Result] = for {
      _                             <- sessionService.resetAll()
      // Get the available count of biks available for each tax year
      biksListOptionCY: List[Bik]   <-
        bikListService.registeredBenefitsList(controllersReferenceData.yearRange.cyminus1, EmpRef.empty)
      biksListOptionCYP1: List[Bik] <-
        bikListService.registeredBenefitsList(controllersReferenceData.yearRange.cy, EmpRef.empty)
      currentYearList               <- bikListService.currentYearList
      nextYearList                  <- bikListService.nextYearList
    } yield {
      sessionService.storeCYRegisteredBiks(currentYearList.bikList)
      sessionService.storeNYRegisteredBiks(nextYearList.bikList)
      val fromYTA = if (request.session.get(ControllersReferenceDataCodes.SESSION_FROM_YTA).isDefined) {
        request.session.get(ControllersReferenceDataCodes.SESSION_FROM_YTA).get
      } else {
        isFromYTA
      }
      auditHomePageView()
      Ok(
        summaryPage(
          pbikAppConfig.cyEnabled,
          taxYearRange,
          currentYearList.bikList,
          nextYearList.bikList,
          biksListOptionCY.size,
          biksListOptionCYP1.size,
          fromYTA.toString,
          empRef = request.empRef
        )
      )
        .addingToSession(nextYearList.headers.toSeq: _*)
        .addingToSession(ControllersReferenceDataCodes.SESSION_FROM_YTA -> fromYTA.toString)
    }
    controllersReferenceData.responseErrorHandler(pageLoadFuture)
  }

  def isFromYTA(implicit request: Request[_]): Boolean = {
    val refererUrl = Try(request.headers("referer"))
    refererUrl match {
      case Success(url) if url.endsWith("/business-account") => true
      case Success(url) if url.endsWith("/account")          => true
      case _                                                 => false
    }
  }

  def auditHomePageView()(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[AuditResult] =
    splunkLogger.logSplunkEvent(
      splunkLogger.createDataEvent(
        tier = splunkLogger.FRONTEND,
        action = splunkLogger.VIEW,
        target = splunkLogger.BIK,
        period = splunkLogger.BOTH,
        msg = "Home page view",
        nino = None,
        iabd = None,
        name = Option(request.name),
        empRef = Some(request.empRef)
      )
    )

}
