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

package controllers.registration

import config.PbikAppConfig
import connectors.PbikConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models.*
import models.auth.AuthenticatedRequest
import models.form.BinaryRadioButtonWithDesc
import models.v1.IabdType.IabdType
import models.v1.*
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import services.{BikListService, RegistrationService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.*
import utils.Exceptions.GenericServerErrorException
import views.html.registration.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageRegistrationController @Inject() (
  registrationService: RegistrationService,
  formMappings: FormMappings,
  override val messagesApi: MessagesApi,
  cc: MessagesControllerComponents,
  val bikListService: BikListService,
  tierConnector: PbikConnector,
  val authenticate: AuthAction,
  val noSessionCheck: NoSessionCheckAction,
  val sessionService: SessionService,
  controllersReferenceData: ControllersReferenceData,
  splunkLogger: SplunkLogger,
  pbikAppConfig: PbikAppConfig,
  nextTaxYearView: NextTaxYear,
  currentTaxYearView: CurrentTaxYear,
  confirmAddCurrentTaxYearView: ConfirmAddCurrentTaxYear,
  confirmUpdateNextTaxYearView: ConfirmUpdateNextTaxYear,
  removeBenefitNextTaxYearView: RemoveBenefitNextTaxYear,
  removeBenefitOtherReason: RemoveBenefitOtherReason,
  confirmRemoveNextTaxYear: ConfirmRemoveNextTaxYear
)(implicit ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport
    with Logging
    with WithUnsafeDefaultFormBinding {

  def nextTaxYearAddOnPageLoad: Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val staticDataRequest = registrationService.generateViewForBikRegistrationSelection(
        controllersReferenceData.yearRange.cy,
        generateViewBasedOnFormItems = nextTaxYearView(_, additive = true, controllersReferenceData.yearRange, _, _, _)
      )
      controllersReferenceData.responseErrorHandler(staticDataRequest)
    }

  def currentTaxYearOnPageLoad: Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val staticDataRequest = registrationService.generateViewForBikRegistrationSelection(
        controllersReferenceData.yearRange.cyminus1,
        generateViewBasedOnFormItems = currentTaxYearView(_, controllersReferenceData.yearRange, _, _, _)
      )
      controllersReferenceData.responseCheckCYEnabled(staticDataRequest)
    }

  def checkYourAnswersAddCurrentTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        result <- formMappings.objSelectedForm
                    .bindFromRequest()
                    .fold(
                      formWithErrors =>
                        Future.successful(
                          BadRequest(
                            currentTaxYearView(
                              formWithErrors,
                              controllersReferenceData.yearRange,
                              isExhausted = false,
                              nonLegislationBiks = pbikAppConfig.biksNotSupported.map(_.id),
                              decommissionedBiks = pbikAppConfig.biksDecommissioned.map(_.id)
                            )
                          )
                        ),
                      values =>
                        sessionService.storeRegistrationList(values).flatMap { _ =>
                          Future.successful(
                            Redirect(routes.ManageRegistrationController.showCheckYourAnswersAddCurrentTaxYear)
                          )
                        }
                    )
      } yield result
      controllersReferenceData.responseCheckCYEnabled(resultFuture)
  }

  def showCheckYourAnswersAddCurrentTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = sessionService.fetchPbikSession().flatMap { session =>
        val activeReg                    = session.flatMap(_.getActiveRegistrationItems).getOrElse(List.empty[RegistrationItem])
        val registrationList             = RegistrationList(None, activeReg, None)
        val form: Form[RegistrationList] = formMappings.objSelectedForm.fill(registrationList)
        Future.successful(
          Ok(confirmAddCurrentTaxYearView(form, controllersReferenceData.yearRange))
        )
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def checkYourAnswersAddNextTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        result <- formMappings.objSelectedForm
                    .bindFromRequest()
                    .fold(
                      formWithErrors =>
                        Future.successful(
                          BadRequest(
                            nextTaxYearView(
                              form = formWithErrors,
                              additive = true,
                              taxYearRange = controllersReferenceData.yearRange,
                              nonLegislationBiks = pbikAppConfig.biksNotSupported.map(_.id),
                              decommissionedBiks = pbikAppConfig.biksDecommissioned.map(_.id),
                              isExhausted = false
                            )
                          )
                        ),
                      values =>
                        sessionService.storeRegistrationList(values).flatMap { _ =>
                          Future.successful(
                            Redirect(routes.ManageRegistrationController.showCheckYourAnswersAddNextTaxYear)
                          )
                        }
                    )
      } yield result
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def showCheckYourAnswersAddNextTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = sessionService.fetchPbikSession().flatMap { session =>
        val activeReg        = session.flatMap(_.getActiveRegistrationItems).getOrElse(List.empty[RegistrationItem])
        val registrationList = RegistrationList(None, activeReg, None)
        Future.successful(
          Ok(
            confirmUpdateNextTaxYearView(registrationList, controllersReferenceData.yearRange)
          )
        )
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def checkYourAnswersRemoveNextTaxYear(iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      controllersReferenceData.responseErrorHandler(
        showCheckYourAnswersRemoveNextTaxYear(iabdType, formMappings.removalReasonForm)
      )
    }

  private def showCheckYourAnswersRemoveNextTaxYear(iabdType: IabdType, form: Form[BinaryRadioButtonWithDesc])(implicit
    request: AuthenticatedRequest[?]
  ): Future[Result] = {
    val bikToRemove = RegistrationItem(iabdType, active = true, enabled = true)
    sessionService.storeBikRemoved(RegistrationItem(iabdType, active = false, enabled = true))

    Future.successful(
      Ok(
        removeBenefitNextTaxYearView(
          bikToRemove,
          controllersReferenceData.yearRange,
          form
        )
      )
    )
  }

  def showConfirmRemoveNextTaxYear(iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      Future.successful(Ok(confirmRemoveNextTaxYear(iabdType, controllersReferenceData.yearRange)))
    }

  def submitConfirmRemoveNextTaxYear(iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val registeredFuture = sessionService.fetchPbikSession().flatMap { session =>
        val registeredResponseOption = session.get.nyRegisteredBiks
        val reason                   = session.flatMap(_.registrations.filter(_.reason.isDefined)).flatMap(_.reason)
        val bikToRemove              = registeredResponseOption.flatMap(_.getBenefitInKindWithCount.find(_.iabdType == iabdType))

        removeBenefitReasonValidation(
          reason,
          controllersReferenceData.yearRange.cy,
          registeredResponseOption.map(_.currentEmployerOptimisticLock).getOrElse(0),
          bikToRemove.get,
          iabdType
        )
          .map(_ => Redirect(controllers.routes.WhatNextPageController.showWhatNextRemovedBik(iabdType)))
      }

      controllersReferenceData.responseErrorHandler(registeredFuture)
    }

  def removeNextYearRegisteredBenefitTypes(iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val registeredFuture = sessionService.fetchPbikSession().flatMap { session =>
        val bikId       = session.flatMap(_.bikRemoved.map(_.iabdType)).get
        val bikToRemove = BenefitInKindRequest(bikId, PbikAction.RemovePayrolledBenefitInKind, request.isAgent)
        updateBiksFutureAction(controllersReferenceData.yearRange.cy, List(bikToRemove), additive = false)
      }
      controllersReferenceData.responseErrorHandler(registeredFuture)
    }

  def showRemoveBenefitOtherReason(iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      Future.successful(
        Ok(removeBenefitOtherReason(formMappings.removalOtherReasonForm, iabdType))
      )
    }

  def submitRemoveBenefitOtherReason(iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      formMappings.removalOtherReasonForm
        .bindFromRequest()
        .fold(
          formWithErrors => {
            logger.warn("[ManageRegistrationController][submitRemoveBenefitOtherReason] No removal reason entered")
            Future.successful(BadRequest(removeBenefitOtherReason(formWithErrors, iabdType)))
          },
          otherReason => {
            val registeredFuture = sessionService.fetchPbikSession().flatMap { session =>
              val activeReg      = session.flatMap(_.getActiveRegistrationItems).getOrElse(List.empty[RegistrationItem])
              val listWithReason =
                RegistrationList(
                  None,
                  activeReg,
                  reason =
                    Some(BinaryRadioButtonWithDesc(ControllersReferenceDataCodes.OTHER, Some(otherReason.reason)))
                )
              sessionService.storeRegistrationList(listWithReason).map { _ =>
                Redirect(
                  routes.ManageRegistrationController.showConfirmRemoveNextTaxYear(iabdType)
                )
              }
            }
            controllersReferenceData.responseErrorHandler(registeredFuture)
          }
        )
    }

  def updateCurrentYearRegisteredBenefitTypes(): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val actionFuture = sessionService.fetchPbikSession().flatMap { session =>
        val activeReg = session.flatMap(_.getActiveRegistrationItems).getOrElse(List.empty[RegistrationItem])

        val persistentBiks = activeReg
          .filter(biks => biks.active)
          .map(bik => BenefitInKindRequest(bik.iabdType, PbikAction.ReinstatePayrolledBenefitInKind, request.isAgent))
        updateBiksFutureAction(controllersReferenceData.yearRange.cyminus1, persistentBiks, additive = true)
      }
      controllersReferenceData.responseCheckCYEnabled(actionFuture)
  }

  def addNextYearRegisteredBenefitTypes(): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val actionFuture = sessionService.fetchPbikSession().flatMap { session =>
        val activeReg = session.flatMap(_.getActiveRegistrationItems).getOrElse(List.empty[RegistrationItem])

        val persistentBiks = activeReg
          .filter(biks => biks.active)
          .map(bik => BenefitInKindRequest(bik.iabdType, PbikAction.ReinstatePayrolledBenefitInKind, request.isAgent))
        updateBiksFutureAction(controllersReferenceData.yearRange.cy, persistentBiks, additive = true)
      }
      controllersReferenceData.responseErrorHandler(actionFuture)
  }

  def updateBiksFutureAction(year: Int, changes: List[BenefitInKindRequest], additive: Boolean)(implicit
    request: AuthenticatedRequest[AnyContent]
  ): Future[Result] = {
    logger.info(s"[updateBiksFutureAction] Starting - year: $year, additive: $additive, changes count: ${changes.size}")

    bikListService
      .getRegisteredBenefitsForYear(year)
      .flatMap { registeredResponse =>
        logger.info(
          s"[updateBiksFutureAction] Got registered benefits for year $year, lock: ${registeredResponse.currentEmployerOptimisticLock}"
        )

        sessionService.fetchPbikSession().flatMap { session =>
          logger.info(s"[updateBiksFutureAction] Got session, session exists: ${session.isDefined}")

          if (additive) {
            // Process registration
            val payload = BenefitListUpdateRequest(
              changes,
              EmployerOptimisticLockRequest(registeredResponse.currentEmployerOptimisticLock)
            )

            lazy val yearRange  = controllersReferenceData.yearRange
            lazy val yearString = year match {
              case yearRange.cy       => utils.FormMappingsConstants.CYP1
              case yearRange.cyminus1 => utils.FormMappingsConstants.CY
            }

            logger.info(
              s"[ManageRegistrationController][updateBiksFutureAction] Created payload, yearString: $yearString, calling connector..."
            )

            for {
              connectorResult <- tierConnector.updateOrganisationsRegisteredBiks(year, payload)
              _                =
                logger.info(
                  s"[ManageRegistrationController][updateBiksFutureAction] Got connector result, status: ${connectorResult.header.status}"
                )
              finalResult     <- connectorResult.header.status match {
                                   case OK       =>
                                     logger.info(
                                       s"[ManageRegistrationController][updateBiksFutureAction] Success case - performing audit and redirecting to $yearString"
                                     )
                                     auditBikUpdate(additive = true, year, changes.map(_.iabdType))
                                     val redirectResult = Redirect(
                                       controllers.routes.WhatNextPageController.showWhatNextRegisteredBik(yearString)
                                     )
                                     logger.info(
                                       s"[ManageRegistrationController][updateBiksFutureAction] Created redirect result: ${redirectResult.header.status}"
                                     )
                                     Future.successful(redirectResult)
                                   case CONFLICT =>
                                     logger.warn(
                                       "[ManageRegistrationController][updateBiksFutureAction] Optimistic lock conflict detected"
                                     )
                                     logger.warn(s"Connector Result: $connectorResult")
                                     Future.successful(connectorResult)
                                   case _        =>
                                     logger.warn(
                                       s"[ManageRegistrationController][updateBiksFutureAction] Non-OK status: ${connectorResult.header.status}"
                                     )
                                     Future.successful(connectorResult)
                                 }
            } yield {
              logger.info(
                s"[ManageRegistrationController][updateBiksFutureAction] Yielding final result with status: ${finalResult.header.status}"
              )
              finalResult
            }
          } else {
            logger.info("[ManageRegistrationController][updateBiksFutureAction] Processing removal (additive=false)")
            formMappings.removalReasonForm
              .bindFromRequest()
              .fold(
                formWithErrors => {
                  logger.warn("[ManageRegistrationController][updateBiksFutureAction] No removal reason selected")
                  showCheckYourAnswersRemoveNextTaxYear(
                    changes.head.iabdType,
                    formWithErrors
                  )
                },
                values =>
                  values.selectionValue match {
                    case ControllersReferenceDataCodes.OTHER =>
                      logger
                        .info("[ManageRegistrationController][updateBiksFutureAction] Redirecting to other reason page")
                      Future.successful(
                        Redirect(
                          routes.ManageRegistrationController.showRemoveBenefitOtherReason(
                            changes.head.iabdType
                          )
                        )
                      )
                    case _                                   =>
                      logger
                        .info("[ManageRegistrationController][updateBiksFutureAction] Processing removal with reason")
                      val activeReg =
                        session.flatMap(_.getActiveRegistrationItems).getOrElse(List.empty[RegistrationItem])

                      val listWithReason =
                        RegistrationList(None, activeReg, reason = Some(values))
                      sessionService.storeRegistrationList(listWithReason).flatMap { _ =>
                        logger.info(
                          "[ManageRegistrationController][updateBiksFutureAction] Stored registration list, redirecting to confirm"
                        )
                        Future.successful(
                          Redirect(
                            routes.ManageRegistrationController.showConfirmRemoveNextTaxYear(
                              changes.head.iabdType
                            )
                          )
                        )
                      }
                  }
              )
          }
        }
      }
      .recover { case ex: Exception =>
        logger.error(s"[updateBiksFutureAction] Exception caught: ${ex.getClass.getSimpleName}: ${ex.getMessage}", ex)
        InternalServerError(s"An error occurred: ${ex.getMessage}")
      }
  }

  def removeBenefitReasonValidation(
    reasonOption: Option[BinaryRadioButtonWithDesc],
    year: Int,
    employerLock: Int,
    benefitWithCount: BenefitInKindWithCount,
    iabdType: IabdType
  )(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val employerOptimisticLockRequest = EmployerOptimisticLockRequest(employerLock)
    val payload                       = BenefitListUpdateRequest(
      List(
        BenefitInKindRequest(
          benefitWithCount.iabdType,
          PbikAction.RemovePayrolledBenefitInKind,
          request.isAgent
        )
      ),
      employerOptimisticLockRequest
    )

    reasonOption match {
      case Some(reasonValue)
          if ControllersReferenceDataCodes.BIK_REMOVE_REASON_LIST.contains(reasonValue.selectionValue) =>
        val reasonUserInfo = reasonValue.info match {
          case Some(info) => Some((reasonValue.selectionValue.toUpperCase, Some(info)))
          case _          => Some((reasonValue.selectionValue.toUpperCase, None))
        }
        for {
          connectorResult <- tierConnector.updateOrganisationsRegisteredBiks(year, payload)
          finalResult     <- connectorResult.header.status match {
                               case OK       =>
                                 auditBikUpdate(
                                   additive = false,
                                   year,
                                   List(benefitWithCount.iabdType),
                                   reasonUserInfo
                                 )
                                 Future.successful(
                                   Redirect(controllers.routes.WhatNextPageController.showWhatNextRemovedBik(iabdType))
                                 )
                               case CONFLICT =>
                                 logger.warn("Optimistic lock conflict in benefit removal")
                                 Future.successful(connectorResult)
                               case _        =>
                                 Future.successful(connectorResult)
                             }
        } yield finalResult
      case _ =>
        logger.warn(
          s"[ManageRegistrationController][removeBenefitReasonValidation] Couldn't find reason from request form"
        )
        showCheckYourAnswersRemoveNextTaxYear(
          iabdType,
          formMappings.removalReasonForm.withError("missingInfo", Messages("RemoveBenefits.reason.no.selection"))
        )
    }
  }

  private def auditBikUpdate(
    additive: Boolean,
    year: Int,
    iabdTypes: List[IabdType],
    removeReason: Option[(String, Option[String])] = None
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[?]): Unit = {
    val derivedMsg =
      if (additive) { "Benefit added to " + splunkLogger.taxYearToSpPeriod(year) }
      else {
        "Benefit removed from " + splunkLogger.taxYearToSpPeriod(year)
      }
    for (iabd <- iabdTypes)
      splunkLogger.logSplunkEvent(
        splunkLogger.createDataEvent(
          tier = splunkLogger.FRONTEND,
          action = if (additive) splunkLogger.ADD else splunkLogger.REMOVE,
          target = splunkLogger.BIK,
          period = splunkLogger.taxYearToSpPeriod(year),
          msg = derivedMsg + " : " + iabd.toString,
          nino = None,
          iabd = Some(iabd.toString),
          removeReason = if (additive) None else Some(removeReason.get._1),
          removeReasonDesc = if (additive) None else Some(removeReason.get._2.getOrElse("")),
          name = request.userId,
          empRef = Some(request.empRef)
        )
      )
  }

}
