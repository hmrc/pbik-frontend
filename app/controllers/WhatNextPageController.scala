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

import java.util.UUID

import config._
import connectors.HmrcTierConnector
import javax.inject.Inject
import models._
import play.api.Mode.Mode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Result
import play.api.{Configuration, Environment}
import services.BikListService
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{ControllersReferenceData, _}

class WhatNextPageController @Inject()(val messagesApi: MessagesApi,
                                       bikListService: BikListService,
                                       val tierConnector: HmrcTierConnector,
                                       val runModeConfiguration: Configuration,
                                       environment: Environment,
                                       taxDateUtils: TaxDateUtils,
                                       controllersReferenceData: ControllersReferenceData)
                                       (implicit val pbikAppConfig: PbikAppConfig,
                                       implicit val externalURLs: ExternalUrls,
                                       implicit val localFormPartialRetriever: LocalFormPartialRetriever) extends FrontendController with I18nSupport {

  val mode: Mode = environment.mode

  def calculateTaxYear(isCurrentTaxYear: Boolean): (Int, Int) = {
    val isCurrentYear = if (isCurrentTaxYear) FormMappingsConstants.CY else FormMappingsConstants.CYP1
    isCurrentYear match {
      case FormMappingsConstants.CY => (controllersReferenceData.YEAR_RANGE.cyminus1,
        controllersReferenceData.YEAR_RANGE.cy)
      case FormMappingsConstants.CYP1 => (controllersReferenceData.YEAR_RANGE.cy,
        controllersReferenceData.YEAR_RANGE.cyplus1)
    }
  }

  def loadWhatNextRegisteredBIK(formRegisteredList: Form[RegistrationList], year: Int)(implicit request: AuthenticatedRequest[_], context: PbikContext): Result = {
    val yearCalculated = calculateTaxYear(taxDateUtils.isCurrentTaxYear(year))

    Ok(views.html.registration.whatNextAddRemove(
      taxDateUtils.isCurrentTaxYear(year),
      controllersReferenceData.YEAR_RANGE,
      additive = true,
      formRegisteredList, empRef = request.empRef)
    ).withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
  }

  def loadWhatNextRemovedBIK(formRegisteredList: Form[RegistrationList], year: Int)(implicit request: AuthenticatedRequest[_], context: PbikContext): Result = {
    val yearCalculated = calculateTaxYear(taxDateUtils.isCurrentTaxYear(year))

    Ok(views.html.registration.whatNextAddRemove(
      taxDateUtils.isCurrentTaxYear(year),
      controllersReferenceData.YEAR_RANGE,
      additive = false,
      formRegisteredList,
      empRef = request.empRef)
    ).withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
  }
}
