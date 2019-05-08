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
import connectors.{HmrcTierConnector, TierConnector}
import models._
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Result
import services.BikListService
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils._

object WhatNextPageController extends WhatNextPageController with TierConnector {
  val pbikAppConfig: AppConfig = PbikAppConfig
  def bikListService: BikListService = BikListService
  val tierConnector = new HmrcTierConnector
}

trait WhatNextPageController extends FrontendController
  with URIInformation
  with ControllersReferenceData
  with SplunkLogger {
  this: TierConnector =>

  def bikListService: BikListService

  def calculateTaxYear(isCurrentTaxYear: Boolean): (Int, Int) = {
    val isCurrentYear = if (isCurrentTaxYear) FormMappingsConstants.CY else FormMappingsConstants.CYP1
    isCurrentYear match {
      case FormMappingsConstants.CY => (YEAR_RANGE.cyminus1, YEAR_RANGE.cy)
      case FormMappingsConstants.CYP1 => (YEAR_RANGE.cy, YEAR_RANGE.cyplus1)
    }
  }

  def loadWhatNextRegisteredBIK(formRegisteredList: Form[RegistrationList], year: Int)(implicit request: AuthenticatedRequest[_], context: PbikContext): Result = {
    val yearCalculated = calculateTaxYear(TaxDateUtils.isCurrentTaxYear(year))

    Ok(views.html.registration.whatNextAddRemove(
      TaxDateUtils.isCurrentTaxYear(year), YEAR_RANGE, additive = true, formRegisteredList, empRef = request.empRef))
      .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
  }

  def loadWhatNextRemovedBIK(formRegisteredList: Form[RegistrationList], year: Int)(implicit request: AuthenticatedRequest[_], context: PbikContext): Result = {
    val yearCalculated = calculateTaxYear(TaxDateUtils.isCurrentTaxYear(year))

    Ok(views.html.registration.whatNextAddRemove(
      TaxDateUtils.isCurrentTaxYear(year), YEAR_RANGE, additive = false, formRegisteredList, empRef = request.empRef))
      .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
  }
}
