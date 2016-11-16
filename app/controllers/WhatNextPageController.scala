/*
 * Copyright 2016 HM Revenue & Customs
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
import _root_.models._
import config._
import connectors.{HmrcTierConnector, TierConnector}
import controllers.auth._
import play.api.data.Form
import play.api.i18n.Lang
import play.api.mvc.{Result, _}
import services.BikListService
import uk.gov.hmrc.play.http.SessionKeys
import utils._
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.frontend.auth.AuthContext
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object WhatNextPageController extends WhatNextPageController with TierConnector
with AuthenticationConnector {
  def pbikAppConfig = PbikAppConfig
  def bikListService = BikListService
  val tierConnector = new HmrcTierConnector
}

trait WhatNextPageController extends FrontendController with URIInformation
with ControllersReferenceData with PbikActions with EpayeUser with SplunkLogger {

  this: TierConnector =>
  def bikListService: BikListService

  def calculateTaxYear(isCurrentTaxYear: Boolean): (Int, Int) = {
    val isCurrentYear = isCurrentTaxYear match {
      case true => FormMappingsConstants.CY
      case false => FormMappingsConstants.CYP1
    }
    isCurrentYear match {
      case FormMappingsConstants.CY => (YEAR_RANGE.cyminus1, + YEAR_RANGE.cy)
      case FormMappingsConstants.CYP1 => (YEAR_RANGE.cy, YEAR_RANGE.cyplus1)
    }
  }

  def loadWhatNextRegisteredBIK(formRegisteredList: Form[RegistrationList], year: Int)(implicit request: Request[_], ac: AuthContext, context: PbikContext):Result = {
    val yearCalculated = calculateTaxYear(TaxDateUtils.isCurrentTaxYear(year))

    Ok(views.html.registration.whatNextAddRemove.render(
      TaxDateUtils.isCurrentTaxYear(year), YEAR_RANGE, true, formRegisteredList, request, ac, context, applicationMessages))
      .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
  }

  def loadWhatNextRemovedBIK(formRegisteredList: Form[RegistrationList], year: Int)(implicit request: Request[_], ac: AuthContext, context: PbikContext):Result = {
    val yearCalculated = calculateTaxYear(TaxDateUtils.isCurrentTaxYear(year))

    Ok(views.html.registration.whatNextAddRemove.render(
      TaxDateUtils.isCurrentTaxYear(year), YEAR_RANGE, false, formRegisteredList, request, ac, context, applicationMessages))
      .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
  }

}
