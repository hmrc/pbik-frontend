/*
 * Copyright 2025 HM Revenue & Customs
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

import controllers.actions.AuthAction
import models._
import models.v1.IabdType.IabdType
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._
import views.html.registration.{AddBenefitConfirmationNextTaxYear, RemoveBenefitConfirmationNextTaxYear}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class WhatNextPageController @Inject() (
  override val messagesApi: MessagesApi,
  sessionService: SessionService,
  authenticate: AuthAction,
  taxDateUtils: TaxDateUtils,
  controllersReferenceData: ControllersReferenceData,
  cc: MessagesControllerComponents,
  addBenefitConfirmationNextTaxYearView: AddBenefitConfirmationNextTaxYear,
  removeBenefitConfirmationNextTaxYearView: RemoveBenefitConfirmationNextTaxYear
)(implicit ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport {

  def showWhatNextRegisteredBik(year: String): Action[AnyContent] =
    authenticate.async { implicit request =>
      val yearInt = taxDateUtils.mapYearStringToInt(year)

      val resultFuture = sessionService.fetchPbikSession().map { session =>
        val addedBiksAsList: RegistrationList =
          RegistrationList(active = session.get.registrations.get.active.filter(item => item.active))
        Ok(
          addBenefitConfirmationNextTaxYearView(
            taxDateUtils.isCurrentTaxYear(yearInt),
            controllersReferenceData.yearRange,
            addedBiksAsList
          )
        )
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def showWhatNextRemovedBik(iabdType: IabdType): Action[AnyContent] =
    authenticate.async { implicit request =>
      val resultFuture = sessionService.fetchPbikSession().map { session =>
        val removedBikAsList: RegistrationList =
          RegistrationList(active = List(session.get.bikRemoved.get))
        Ok(
          removeBenefitConfirmationNextTaxYearView(
            taxDateUtils.isCurrentTaxYear(controllersReferenceData.yearRange.cyplus1),
            controllersReferenceData.yearRange,
            removedBikAsList,
            iabdType
          )
        )
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
    }
}
