/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.HmrcTierConnector

import javax.inject.{Inject, Singleton}
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{ControllersReferenceData, _}
import controllers.actions.AuthAction
import views.html.registration.{AddBenefitConfirmationNextTaxYear, RemoveBenefitConfirmationNextTaxYear}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class WhatNextPageController @Inject()(
  override val messagesApi: MessagesApi,
  val cachingService: SessionService,
  authenticate: AuthAction,
  val tierConnector: HmrcTierConnector,
  taxDateUtils: TaxDateUtils,
  controllersReferenceData: ControllersReferenceData,
  cc: MessagesControllerComponents,
  addBenefitConfirmationNextTaxYearView: AddBenefitConfirmationNextTaxYear,
  removeBenefitConfirmationNextTaxYearView: RemoveBenefitConfirmationNextTaxYear
) extends FrontendController(cc) with I18nSupport {

  def calculateTaxYear(isCurrentTaxYear: Boolean): (Int, Int) =
    if (isCurrentTaxYear) {
      (controllersReferenceData.yearRange.cyminus1, controllersReferenceData.yearRange.cy)
    } else {
      (controllersReferenceData.yearRange.cy, controllersReferenceData.yearRange.cyplus1)
    }

  def showWhatNextRegisteredBik(year: String): Action[AnyContent] =
    (authenticate).async { implicit request =>
      val yearInt = year match {
        case "cy1" => controllersReferenceData.yearRange.cy
        case "cy"  => controllersReferenceData.yearRange.cyminus1
      }
      val resultFuture = cachingService.fetchPbikSession().map { session =>
        val addedBiksAsList: RegistrationList =
          RegistrationList(active = session.get.registrations.get.active.filter(item => item.active))
        Ok(
          addBenefitConfirmationNextTaxYearView(
            taxDateUtils.isCurrentTaxYear(yearInt),
            controllersReferenceData.yearRange,
            addedBiksAsList,
            empRef = request.empRef
          ))
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def showWhatNextRemovedBik: Action[AnyContent] =
    (authenticate).async { implicit request =>
      val resultFuture = cachingService.fetchPbikSession().map { session =>
        val removedBikAsList: RegistrationList =
          RegistrationList(active = List(session.get.bikRemoved.get))
        Ok(
          removeBenefitConfirmationNextTaxYearView(
            taxDateUtils.isCurrentTaxYear(controllersReferenceData.yearRange.cyplus1),
            controllersReferenceData.yearRange,
            removedBikAsList,
            empRef = request.empRef
          ))
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
    }
}
