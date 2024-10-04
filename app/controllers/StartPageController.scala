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

import controllers.actions.{AuthAction, NoSessionCheckAction}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.BikListService
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Exceptions.InvalidYearURIException
import utils.{ControllersReferenceData, FormMappings, TaxDateUtils}
import views.html.{SelectYearPage, StartPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartPageController @Inject() (
  cc: MessagesControllerComponents,
  authenticate: AuthAction,
  noSessionCheck: NoSessionCheckAction,
  bikListService: BikListService,
  formMappings: FormMappings,
  controllersReferenceData: ControllersReferenceData,
  taxDateUtils: TaxDateUtils,
  startPageView: StartPage,
  selectYearPageView: SelectYearPage
)(implicit val ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport
    with Logging
    with WithUnsafeDefaultFormBinding {

  def onPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck) { implicit request =>
    Ok(startPageView())
  }

  def selectYearPage: Action[AnyContent] = (authenticate andThen noSessionCheck).async { implicit request =>
    val taxYearRange                 = controllersReferenceData.yearRange
    val resultFuture: Future[Result] = for {
      currentYearList <- bikListService.currentYearList
    } yield
      if (currentYearList.getBenefitInKindWithCount.isEmpty) {
        Redirect(routes.HomePageController.onPageLoadCY1)
      } else {
        Ok(selectYearPageView(taxYearRange, formMappings.selectYearForm))
      }

    controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def submitSelectYearPage: Action[AnyContent] = (authenticate andThen noSessionCheck).async { implicit request =>
    val taxYearRange                 = controllersReferenceData.yearRange
    val resultFuture: Future[Result] = formMappings.selectYearForm
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(selectYearPageView(taxYearRange, formWithErrors))),
        values => {
          val selectedValue = values.year
          taxDateUtils.mapYearStringToInt(selectedValue, controllersReferenceData.yearRange)
          selectedValue match {
            case utils.FormMappingsConstants.CY   =>
              Future.successful(Redirect(routes.HomePageController.onPageLoadCY))
            case utils.FormMappingsConstants.CYP1 =>
              Future.successful(Redirect(routes.HomePageController.onPageLoadCY1))
            case _                                =>
              Future.failed(throw new InvalidYearURIException())
          }
        }
      )

    controllersReferenceData.responseErrorHandler(resultFuture)
  }

}
