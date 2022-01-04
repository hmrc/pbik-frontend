/*
 * Copyright 2022 HM Revenue & Customs
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

import config.{AppConfig, LocalFormPartialRetriever}
import javax.inject.{Inject, Singleton}
import models._
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.http.Upstream5xxResponse
import utils.Exceptions.{GenericServerErrorException, InvalidBikTypeURIException, InvalidYearURIException}
import views.html.{ErrorPage, MaintenancePage}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ControllersReferenceDataCodes extends Logging {
  val CY_RESTRICTED = "ServiceMessage.10003"
  val FEATURE_RESTRICTED = "ServiceMessage.10002"
  val DEFAULT_ERROR = "ServiceMessage.10001"
  val EXCLUSION_ADD_STATUS = 20
  val BIK_REMOVE_STATUS = 40
  val BIK_ADD_STATUS = 30
  val FORM_TYPE_NINO = "nino"
  val FORM_TYPE_NONINO = "no-nino"
  val NEXT_TAX_YEAR: String = FormMappingsConstants.CYP1
  val SESSION_FROM_YTA = "fromYTA"
  val SESSION_LANG = "session_lang"
  val BIK_REMOVE_REASON_LIST = List("software", "guidance", "not-clear", "not-offering", "other")
  val YES = "yes"
  val OTHER = "other"

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
  val INVALID_YEAR_REFERENCE = "ErrorPage.invalidYear"
  val INVALID_BIK_TYPE_REFERENCE = "ErrorPage.invalidBikType"
  val NO_BENEFITS_REGISTERED = "ErrorPage.noBenefitsRegistered"
  val NO_BENEFITS_REGISTERED_VIEW = "ErrorPage.noBenefitsRegisteredView"
  val INVALID_FORM_ERROR = "ErrorPage.invalidForm"
  val EXLCUSIONS_RADIO_BUTTTON_SELECTION_CONFIRMATION_BACK_BUTTON_ERROR = "ErrorPage.backButtonNoCache"
  val AUTHORISATION_ERROR = "ErrorPage.authorisationError"
  val AUTHORISATION_TITLE = "ErrorPage.authorisationTitle"
}

@Singleton
class ControllersReferenceData @Inject()(
  taxDateUtils: TaxDateUtils,
  override val messagesApi: MessagesApi,
  errorPageView: ErrorPage,
  maintenancePageView: MaintenancePage)(
  implicit val pbikAppConfig: AppConfig,
  implicit val localFormPartialRetriever: LocalFormPartialRetriever)
    extends FormMappings(messagesApi) with I18nSupport with Logging {

  def yearRange: TaxYearRange = taxDateUtils.getTaxYearRange()

  def responseCheckCYEnabled(staticDataRequest: Future[Result])(
    implicit request: AuthenticatedRequest[AnyContent]): Future[Result] =
    if (pbikAppConfig.cyEnabled) {
      responseErrorHandler(staticDataRequest)
    } else {
      logger.info("[ControllersReferenceData][responseCheckCYEnabled] Cy is disabled")
      val errorCode = 10003
      Future(
        Forbidden(
          errorPageView(
            ControllersReferenceDataCodes.CY_RESTRICTED,
            yearRange,
            "",
            errorCode,
            empRef = Some(request.empRef))))
    }

  def responseErrorHandler(staticDataRequest: Future[Result])(
    implicit request: AuthenticatedRequest[AnyContent]): Future[Result] =
    staticDataRequest.recover {
      case e0: NoSuchElementException =>
        logger.warn(s"[ControllersReferenceData][responseErrorHandler] A NoSuchElementException was handled : $e0")
        NotFound(
          errorPageView(
            ControllersReferenceDataCodes.VALIDATION_ERROR_REFERENCE,
            yearRange,
            empRef = Some(request.empRef)))

      case e1: InvalidYearURIException =>
        logger.warn(s"[ControllersReferenceData][responseErrorHandler] An InvalidYearURIException was handled : $e1")
        BadRequest(
          errorPageView(ControllersReferenceDataCodes.INVALID_YEAR_REFERENCE, yearRange, empRef = Some(request.empRef)))

      case e2: InvalidBikTypeURIException =>
        logger.warn(s"[ControllersReferenceData][responseErrorHandler] An InvalidBikTypeURIException was handled : $e2")
        BadRequest(
          errorPageView(
            ControllersReferenceDataCodes.INVALID_BIK_TYPE_REFERENCE,
            yearRange,
            empRef = Some(request.empRef)))

      case Upstream5xxResponse(message, code, _, _) =>
        logger.error(
          s"[ControllersReferenceData][responseErrorHandler] An Upstream5xxResponse was handled with code: $code and message:$message")
        InternalServerError(maintenancePageView(empRef = Some(request.empRef)))

      case e4: GenericServerErrorException =>
        try {
          logger.warn(
            s"[ControllersReferenceData][responseErrorHandler] A GenericServerErrorException was handled: ${e4.message}",
            e4)
          val msgValue = e4.message
          if (Messages("ServiceMessage." + msgValue) == ("ServiceMessage." + msgValue)) {
            throw new Exception(msgValue)
          } else {
            InternalServerError(
              errorPageView(
                Messages("ServiceMessage." + msgValue),
                yearRange,
                "",
                msgValue.toInt,
                empRef = Some(request.empRef)))
          }
        } catch {
          case ex: Exception =>
            logger.warn(
              s"[ControllersReferenceData][responseErrorHandler] Could not parse GenericServerError System Error number: ${if (e4.message.nonEmpty) e4.message else "<no error code>"}. Showing default error page instead",
              ex
            )
            InternalServerError(maintenancePageView(empRef = Some(request.empRef)))
        }
      case e5 =>
        logger.warn(
          s"[ControllersReferenceData][responseErrorHandler]. An exception was handled: ${e5.getMessage}. Showing default error page",
          e5)
        InternalServerError(maintenancePageView(empRef = Some(request.empRef)))
    }

}
