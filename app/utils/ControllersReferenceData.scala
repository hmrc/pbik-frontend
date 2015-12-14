/*
 * Copyright 2015 HM Revenue & Customs
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

package utils

import config.AppConfig
import models._
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request, Result}
import utils.BikListUtils.MandatoryRadioButton
import utils.Exceptions.{GenericServerErrorException, InvalidBikTypeURIException, InvalidYearURIException}
import uk.gov.hmrc.play.http._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future}
import play.api.{Play, Logger}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import config.PbikAppConfig
import play.api.Play.current

object ControllersReferenceData extends ControllersReferenceData {
  def pbikAppConfig = PbikAppConfig
}

trait ControllersReferenceData extends FormMappings {

  implicit val bikFormats = Json.format[Bik]
  implicit val eilFormats = Json.format[EiLPerson]
  implicit val eilListFormats = Json.format[EiLPersonList]
  implicit val mandatoryDecisionFormats = Json.format[MandatoryRadioButton]
  implicit val addRemoveDecisionFormats = Json.format[BinaryRadioButton]
  implicit val registrationItemsFormats = Json.format[RegistrationItem]

  def YEAR_RANGE:TaxYearRange = TaxDateUtils.getTaxYearRange()
  def pbikAppConfig: AppConfig

  val CY_RESTRICTED = "ServiceMessage.10003"
  val FEATURE_RESTRICTED = "ServiceMessage.10002"
  val DEFAULT_ERROR = "ServiceMessage.10001"
  val BIK_REMOVE_STATUS = 40
  val BIK_ADD_STATUS = 30
  val FORM_TYPE_NINO = "nino"
  val FORM_TYPE_NONINO = "no-nino"
  val NEXT_TAX_YEAR = FormMappingsConstants.CYP1
  val SESSION_FROM_YTA = "fromYTA"


  val EXCLUSION_TRACE_AND_MATCH_LIST_OF_PEOPLE = "trace-and-match-list-of-people"
  val EXCLUSION_TRACE_AND_MATCH_RADIO = "trace-and-match-radio"
  val EXCLUSION_SEARCHFORM_PERSON = "person-search-exclusion"
  val EXCLUSION_FORMTYPE_DECISION_FORMID = "formtype-exclusion-decision"
  val EXCLUSION_CHOOSE_BENEFIT_FOR_EXCLUSION = "choose-benefit-decision"
  val SELECT_ALL_REGISTRATION = "select-all-registration"
  val REGISTRATION_VIEW_CHOOSE_YEAR = "registration-view-year-decision"
  val EXCLUSION_VIEW_CHOOSE_YEAR = "exclusion-view-year-decision"
  val EXCLUSION_MANAGE_CHOOSE_YEAR = "exclusion-manage-year-decision"
  val MANAGE_REGISTRATION_DECISION = "registration-add-remove-decision"
  val REGISTRATION_FORMTYPE_DECISION_FORMID = "add-remove-registration-decision"
  val REGISTRATION_LIST_BIKS = "registration-list-biks"

  val INVALID_YEAR_TITLE = "ErrorPage.heading.invalidYear"
  val VALIDATION_ERROR_REFERENCE = "ErrorPage.validationError"
  val CONNECTION_ERROR_REFERENCE = "ErrorPage.connectionProblem"
  val SERVICE_NOT_LAUNCHED_ERROR = "ErrorPage.serviceNotLaunched"
  val NO_MORE_BENEFITS_TO_ADD_HEADING = "AddBenefits.Heading"
  val NO_MORE_BENEFITS_TO_ADD = "ErrorPage.noBenefitsToAdd"
  val NO_MORE_BENEFITS_TO_REMOVE_CY1 = "ErrorPage.noCY1BenefitsToRemove"
  val INVALID_YEAR_REFERENCE="ErrorPage.invalidYear"
  val INVALID_BIK_TYPE_REFERENCE="ErrorPage.invalidBikType"
  val NO_BENEFITS_REGISTERED="ErrorPage.noBenefitsRegistered"
  val NO_BENEFITS_REGISTERED_VIEW="ErrorPage.noBenefitsRegisteredView"
  val INVALID_FORM_ERROR = "ErrorPage.invalidForm"
  val EXLCUSIONS_RADIO_BUTTTON_SELECTION_CONFIRMATION_BACK_BUTTON_ERROR = "ErrorPage.backButtonNoCache"
  val AUTHORISATION_ERROR = "ErrorPage.authorisationError"
  val AUTHORISATION_TITLE = "ErrorPage.authorisationTitle"

  def generateListOfBiksBasedOnForm(bikStatus: Int)(implicit request: Request[AnyContent]): List[Bik] = {
    val persistentBiks: List[Bik] = objSelectedForm.bindFromRequest.fold(
      formWithErrors => List[Bik](),
      values => {
        values.active.filter(x => x.active).map(x => Bik(x.id, bikStatus))
      }
    )
    persistentBiks
  }

  def responseCheckCYEnabled(staticDataRequest: Future[Result])(implicit request: Request[AnyContent], ac: AuthContext): Future[Result] = {
    if(pbikAppConfig.cyEnabled) {
      responseErrorHandler(staticDataRequest)
    } else {
      Logger.info("Cy is disabled")
      Future(Ok(views.html.errorPage(CY_RESTRICTED, YEAR_RANGE, "")))
    }
  }

  def responseErrorHandler(staticDataRequest: Future[Result])(implicit request: Request[AnyContent], ac: AuthContext): Future[Result] = {
    staticDataRequest.recover {
      case e0: NoSuchElementException => {
        Logger.warn("ResponseErrorHandler. A NoSuchElementException was handled :  " + e0)
        Ok(views.html.errorPage(VALIDATION_ERROR_REFERENCE, YEAR_RANGE, ""))
      }
      case e1: InvalidYearURIException => {
        Logger.warn("ResponseErrorHandler. An InvalidYearURIException was handled :  " + e1)
        Ok(views.html.errorPage(INVALID_YEAR_REFERENCE, YEAR_RANGE, ""))
      }
      case e2: InvalidBikTypeURIException => {
        Logger.warn("ResponseErrorHandler. An InvalidBikTypeURIException was handled :  " + e2)
        Ok(views.html.errorPage(INVALID_BIK_TYPE_REFERENCE, YEAR_RANGE, ""))
      }
      case e3:Upstream5xxResponse => {
          Logger.warn("ResponseErrorHandler. An Upstream5xxResponse was handled :  " + e3.message)
          Ok(views.html.maintenancePage())
      }
      case e4: GenericServerErrorException => {
        try {
          Logger.warn("ResponseErrorHandler. A GenericServerErrorException was handled :  " + e4.message)
          val msgValue = e4.message
          if((Messages("ServiceMessage." + (msgValue))) == ("ServiceMessage." + (msgValue))) throw new Exception(msgValue)
          else Ok(views.html.errorPage(Messages("ServiceMessage." + (msgValue)), YEAR_RANGE, "",msgValue.toInt))
        } catch {
          case e: Exception => {
            Logger.warn("Could not parse GenericServerError System Error number: " + e4.message + " .Showing default error page instead")
            Ok(views.html.maintenancePage())
          }
        }
      }
      case e5 => {
        Logger.warn("ResponseErrorHandler. An exception was handled : " + e5.getMessage)
        Ok(views.html.maintenancePage())
      }
    }
  }

}
