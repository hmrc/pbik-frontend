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

import config.PbikAppConfig
import controllers.actions.{AuthAction, NoSessionCheckAction, UnauthorisedAction}
import models.*
import models.auth.AuthenticatedRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.*
import services.{BikListService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.*
import utils.Exceptions.InvalidURIException
import views.html.{ErrorPage, PayrollingSummaryPageMpbik, PayrollingSummaryPageMpbikPhase2, Summary}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
  summaryPage: Summary,
  payrollingSummaryView: PayrollingSummaryPageMpbik,
  payrollingSummaryMpbikPhase2View: PayrollingSummaryPageMpbikPhase2
)(implicit val ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport
    with Logging {

  private val mpbikToggle: Boolean       = pbikAppConfig.mpbikToggle
  private val mpbikPhase2Toggle: Boolean = pbikAppConfig.mpbikTogglePhase2

  def notAuthorised: Action[AnyContent] = authenticate { implicit request =>
    Unauthorized(
      errorPageView(
        ControllersReferenceDataCodes.AUTHORISATION_ERROR,
        taxDateUtils.getTaxYearRange()
      )
    )
  }

  def signout: Action[AnyContent] = unauthorisedAction {
    Redirect(pbikAppConfig.authSignOut, Map("continue" -> Seq(pbikAppConfig.feedbackUrl)))
  }

  def signOutIndividual: Action[AnyContent] = Action {
    Redirect(
      pbikAppConfig.authSignOut,
      Map("continue" -> Seq(pbikAppConfig.host + routes.SignedOutController.individualSignedOut().url))
    )
  }

  def signOutNoSurvey: Action[AnyContent] = Action {
    Redirect(
      pbikAppConfig.authSignOut,
      Map("continue" -> Seq(routes.SignedOutController.signedOut().url))
    )
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
    if (mpbikPhase2Toggle) {
      val startTaxYear                   = controllersReferenceData.yearRange.cy
      val pageLoadFuture: Future[Result] = for {
        _               <- sessionService.resetAll()
        currentYearList <- bikListService.currentYearList
        nextYearList    <- bikListService.nextYearList
        _               <- auditHomePageView()
      } yield Ok(
        payrollingSummaryMpbikPhase2View(
          startTaxYear,
          currentYearList.getBenefitInKindWithCount,
          nextYearList.getBenefitInKindWithCount
        )
      )

      controllersReferenceData.responseErrorHandler(pageLoadFuture)
    } else { // current code - DO NOT CHANGE
      val startTaxYear                   = controllersReferenceData.yearRange.cy
      val pageLoadFuture: Future[Result] = for {
        _               <- sessionService.resetAll()
        currentYearList <- bikListService.currentYearList
        _               <- auditHomePageView()
      } yield Ok(payrollingSummaryView(startTaxYear, currentYearList.getBenefitInKindWithCount))

      controllersReferenceData.responseErrorHandler(pageLoadFuture)
    }
  }

  private def auditHomePageView()(implicit hc: HeaderCarrier, request: AuthenticatedRequest[?]): Future[AuditResult] =
    splunkLogger.logSplunkEvent(
      splunkLogger.createDataEvent(
        tier = splunkLogger.FRONTEND,
        action = splunkLogger.VIEW,
        target = splunkLogger.BIK,
        period = splunkLogger.BOTH,
        msg = "Home page view",
        nino = None,
        iabd = None,
        name = request.userId,
        empRef = Some(request.empRef)
      )
    )

}
