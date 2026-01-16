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

package utils

import config.PbikAppConfig
import models.*
import models.auth.AuthenticatedRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.*
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.Exceptions.{GenericServerErrorException, InvalidBikTypeException, InvalidYearURIException, OptimisticLockConflictException}
import views.html.{ErrorPage, MaintenancePage, OptimisticLockErrorPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object ControllersReferenceDataCodes {
  val CY_RESTRICTED: String                = "ServiceMessage.10003"
  val FEATURE_RESTRICTED: String           = "ServiceMessage.10002"
  val DEFAULT_ERROR: String                = "ServiceMessage.10001"
  val FORM_TYPE_NINO: String               = "nino"
  val FORM_TYPE_NONINO: String             = "no-nino"
  val BIK_REMOVE_REASON_LIST: List[String] = List("software", "guidance", "not-clear", "not-offering", "other")
  val YES: String                          = "yes"
  val NO: String                           = "no"
  val OTHER: String                        = "other"

  val VALIDATION_ERROR_REFERENCE: String      = "ErrorPage.validationError"
  val NO_MORE_BENEFITS_TO_ADD_HEADING: String = "AddBenefits.Heading"
  val NO_MORE_BENEFITS_TO_ADD: String         = "ErrorPage.noBenefitsToAdd"
  val INVALID_YEAR_REFERENCE: String          = "ErrorPage.invalidYear"
  val INVALID_BIK_TYPE_REFERENCE: String      = "ErrorPage.invalidBikType"
  val INVALID_FORM_ERROR: String              = "ErrorPage.invalidForm"
  val AUTHORISATION_ERROR: String             = "ErrorPage.authorisationError"
}

@Singleton
class ControllersReferenceData @Inject() (
  taxDateUtils: TaxDateUtils,
  override val messagesApi: MessagesApi,
  errorPageView: ErrorPage,
  optimisticLockErrorPage: OptimisticLockErrorPage,
  maintenancePageView: MaintenancePage
)(implicit
  ec: ExecutionContext,
  val pbikAppConfig: PbikAppConfig
) extends FormMappings(messagesApi)
    with I18nSupport
    with Logging {

  def yearRange: TaxYearRange = taxDateUtils.getTaxYearRange()

  def responseCheckCYEnabled(
    staticDataRequest: Future[Result]
  )(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val errorCode = 10003
    Future(
      Forbidden(
        errorPageView(
          ControllersReferenceDataCodes.CY_RESTRICTED,
          yearRange,
          "",
          errorCode
        )
      )
    )
  }

  def responseErrorHandler(
    staticDataRequest: Future[Result]
  )(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] =
    staticDataRequest.recover {
      case e0: NoSuchElementException  =>
        logger.error(s"[ControllersReferenceData][responseErrorHandler] A NoSuchElementException was handled: $e0")
        NotFound(
          errorPageView(
            ControllersReferenceDataCodes.VALIDATION_ERROR_REFERENCE,
            yearRange
          )
        )
      case e1: InvalidYearURIException =>
        logger.warn(s"[ControllersReferenceData][responseErrorHandler] An InvalidYearURIException was handled : $e1")
        BadRequest(
          errorPageView(ControllersReferenceDataCodes.INVALID_YEAR_REFERENCE, yearRange)
        )
      case e2: InvalidBikTypeException =>
        logger.warn(s"[ControllersReferenceData][responseErrorHandler] An InvalidBikTypeException was handled : $e2")
        BadRequest(
          errorPageView(
            ControllersReferenceDataCodes.INVALID_BIK_TYPE_REFERENCE,
            yearRange
          )
        )

      case UpstreamErrorResponse(message, code, _, _) =>
        logger.error(
          s"[ControllersReferenceData][responseErrorHandler] An Upstream5xxResponse was handled with code: " +
            s"$code and message:$message"
        )
        InternalServerError(maintenancePageView())

      case e4: GenericServerErrorException     =>
        try {
          logger.warn(
            s"[ControllersReferenceData][responseErrorHandler] A GenericServerErrorException was handled: " +
              s"${e4.message}",
            e4
          )
          val msgValue = e4.message
          if (Messages("ServiceMessage." + msgValue) == ("ServiceMessage." + msgValue)) {
            throw new Exception(msgValue)
          } else {
            InternalServerError(
              errorPageView(
                Messages("ServiceMessage." + msgValue),
                yearRange,
                "",
                msgValue.toInt
              )
            )
          }
        } catch {
          case ex: Exception =>
            logger.warn(
              s"[ControllersReferenceData][responseErrorHandler] Could not parse GenericServerError System Error number:" +
                s" ${if (e4.message.nonEmpty) e4.message else "<no error code>"}. Showing default error page instead",
              ex
            )
            InternalServerError(maintenancePageView())
        }
      case e5: OptimisticLockConflictException =>
        logger.warn(
          s"[ControllersReferenceData][responseErrorHandler] An OptimisticLockConflictException was handled : $e5"
        )
        Conflict(
          optimisticLockErrorPage(taxDateUtils.isCurrentTaxYear(e5.year))
        )
      case e6                                  =>
        logger.warn(
          s"[ControllersReferenceData][responseErrorHandler]. An exception was handled: ${e6.getMessage}. " +
            s"Showing default error page",
          e6
        )
        InternalServerError(maintenancePageView())
    }

}
