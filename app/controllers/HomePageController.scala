/*
 * Copyright 2024 HM Revenue & Customs
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
        taxDateUtils.getTaxYearRange()
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

  def onPageLoadCY1: Action[AnyContent] = (authenticate andThen noSessionCheck).async { implicit request =>
    val taxYearRange: TaxYearRange     = taxDateUtils.getTaxYearRange()
    val pageLoadFuture: Future[Result] = for {
      _                  <- sessionService.resetAll()
      // Get the available count of biks available for each tax year
      biksListOptionCYP1 <- bikListService.getAllBenefitsForYear(controllersReferenceData.yearRange.cy)
      nextYearList       <- bikListService.nextYearList
      currentYearList    <- bikListService.currentYearList
    } yield {
      sessionService.storeNYRegisteredBiks(nextYearList.bikList.toList)

      auditHomePageView()

      Ok(
        summaryPage(
          pbikAppConfig.cyEnabled,
          taxYearRange,
          List.empty,
          nextYearList.bikList.toList,
          0,
          biksListOptionCYP1.size,
          currentYearList.bikList.nonEmpty
        )
      ).removingFromSession(HeaderTags.ETAG).addingToSession(nextYearList.headers.toSeq: _*)
    }
    controllersReferenceData.responseErrorHandler(pageLoadFuture)
  }

  def onPageLoadCY: Action[AnyContent] = (authenticate andThen noSessionCheck).async { implicit request =>
    val taxYearRange: TaxYearRange     = taxDateUtils.getTaxYearRange()
    val pageLoadFuture: Future[Result] = for {
      _                <- sessionService.resetAll()
      // Get the available count of biks available for each tax year
      biksListOptionCY <- bikListService.getAllBenefitsForYear(controllersReferenceData.yearRange.cyminus1)
      currentYearList  <- bikListService.currentYearList
    } yield {
      sessionService.storeCYRegisteredBiks(currentYearList.bikList.toList)

      auditHomePageView()

      Ok(
        summaryPage(
          pbikAppConfig.cyEnabled,
          taxYearRange,
          currentYearList.bikList.toList,
          List.empty,
          biksListOptionCY.size,
          0,
          showChangeYearLink = true
        )
      ).removingFromSession(HeaderTags.ETAG).addingToSession(currentYearList.headers.toSeq: _*)
    }
    controllersReferenceData.responseErrorHandler(pageLoadFuture)
  }

  private def auditHomePageView()(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[AuditResult] =
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
