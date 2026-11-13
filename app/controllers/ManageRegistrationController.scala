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

package controllers

import config.PbikAppConfig
import connectors.PbikConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models.*
import models.auth.AuthenticatedRequest
import models.v1.*
import models.v1.IabdType.IabdType
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import play.api.mvc.Results.NotFound
import services.{BikListService, RegistrationService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.WithUrlEncodedOnlyFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.*
import utils.Exceptions.InvalidURIException
import views.html.page_not_found_template
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
  nextTaxYearBenefitView: NextTaxYearBenefit,
  confirmUpdateNextTaxYearView: ConfirmUpdateNextTaxYear,
  confirmRemoveNextTaxYear: ConfirmRemoveNextTaxYear,
  pageNotFound: page_not_found_template
)(implicit ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport
    with Logging
    with WithUrlEncodedOnlyFormBinding {

  def nextTaxYearAddOnPageLoad: Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val staticDataRequest =
        registrationService.generateViewForBikRegistrationSelection(
          controllersReferenceData.yearRange.cy,
          generateViewBasedOnFormItems =
            nextTaxYearBenefitView(_, additive = true, controllersReferenceData.yearRange, _, _, _)
        )
      controllersReferenceData.responseErrorHandler(staticDataRequest)
    }

  def checkYourAnswersAddNextTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture =
        for {
          result <- formMappings.objSelectedForm
                      .bindFromRequest()
                      .fold(
                        formWithErrors =>
                          Future.successful(
                            BadRequest(
                              nextTaxYearBenefitView(
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
      val resultFuture =
        sessionService.fetchPbikSession().flatMap { session =>
          val activeReg        = session.flatMap(_.getActiveRegistrationItems).getOrElse(List.empty[RegistrationItem])
          val registrationList = RegistrationList(None, activeReg)
          if (registrationList.active.isEmpty) {
            Future.successful(
              NotFound(
                pageNotFound()
              )
            )
          } else {
            Future.successful(
              Ok(
                confirmUpdateNextTaxYearView(registrationList, controllersReferenceData.yearRange)
              )
            )
          }
        }
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def checkYourAnswersRemoveNextTaxYear(iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      controllersReferenceData.responseErrorHandler(Future.failed(new InvalidURIException()))
    }

  def showConfirmRemoveNextTaxYear(iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val response = {
        sessionService.storeBikRemoved(RegistrationItem(iabdType, active = false, enabled = true))

        Future.successful(Ok(confirmRemoveNextTaxYear(iabdType, controllersReferenceData.yearRange)))
      }
      controllersReferenceData.responseErrorHandler(response)
    }

  def submitConfirmRemoveNextTaxYear(iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val registeredFuture =
        sessionService.fetchPbikSession().flatMap { session =>
          val registeredResponseOption = session.get.nyRegisteredBiks
          val bikToRemove              = registeredResponseOption.flatMap(_.getBenefitInKindWithCount.find(_.iabdType == iabdType))

          removeBenefitValidation(
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
      val registeredFuture =
        sessionService.fetchPbikSession().flatMap { session =>
          val bikId       = session.flatMap(_.bikRemoved.map(_.iabdType)).get
          val bikToRemove = BenefitInKindRequest(bikId, PbikAction.RemovePayrolledBenefitInKind, request.isAgent)
          updateBiksFutureAction(controllersReferenceData.yearRange.cy, List(bikToRemove), additive = false)
        }
      controllersReferenceData.responseErrorHandler(registeredFuture)
    }

  def addNextYearRegisteredBenefitTypes(): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val actionFuture =
        sessionService.fetchPbikSession().flatMap { session =>
          val activeReg = session.flatMap(_.getActiveRegistrationItems).getOrElse(List.empty[RegistrationItem])

          val newBiksToAdd = activeReg
            .filter(biks => biks.active)
            .map(bik => BenefitInKindRequest(bik.iabdType, PbikAction.ReinstatePayrolledBenefitInKind, request.isAgent))
          updateBiksFutureAction(controllersReferenceData.yearRange.cy, newBiksToAdd, additive = true)
        }
      controllersReferenceData.responseErrorHandler(actionFuture)
  }

  def updateBiksFutureAction(year: Int, changes: List[BenefitInKindRequest], additive: Boolean)(implicit
    request: AuthenticatedRequest[AnyContent]
  ): Future[Result] = {
    val actionFuture =
      bikListService
        .getRegisteredBenefitsForYear(year)
        .flatMap { registeredResponse =>
          sessionService.fetchPbikSession().flatMap { session =>
            if (additive) { // benefit addition
              val payload         = BenefitListUpdateRequest(
                changes,
                EmployerOptimisticLockRequest(registeredResponse.currentEmployerOptimisticLock)
              )
              lazy val yearRange  = controllersReferenceData.yearRange
              lazy val yearString = year match {
                case yearRange.cy       => utils.FormMappingsConstants.CYP1
                case yearRange.cyminus1 => utils.FormMappingsConstants.CY
              }

              for {
                _ <- tierConnector.updateOrganisationsRegisteredBiks(year, payload)
                _  = auditBikUpdate(additive = true, year, changes.map(_.iabdType))
              } yield Redirect(controllers.routes.WhatNextPageController.showWhatNextRegisteredBik(yearString))
            } else { // benefit removal
              val activeReg =
                session.flatMap(_.getActiveRegistrationItems).getOrElse(List.empty[RegistrationItem])

              val regList =
                RegistrationList(None, activeReg)
              sessionService.storeRegistrationList(regList).flatMap { _ =>
                Future.successful(
                  Redirect(
                    routes.ManageRegistrationController.showConfirmRemoveNextTaxYear(
                      changes.head.iabdType
                    )
                  )
                )
              }
            }
          }
        }
    controllersReferenceData.responseErrorHandler(actionFuture)
  }

  def removeBenefitValidation(
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

    val eventualResult = for {
      _ <- tierConnector.updateOrganisationsRegisteredBiks(year, payload)
      _  = auditBikUpdate(additive = false, year, List(benefitWithCount.iabdType))
    } yield Redirect(controllers.routes.WhatNextPageController.showWhatNextRemovedBik(iabdType))
    controllersReferenceData.responseErrorHandler(eventualResult)
  }

  private def auditBikUpdate(
    additive: Boolean,
    year: Int,
    iabdTypes: List[IabdType]
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
          name = request.userId,
          empRef = Some(request.empRef)
        )
      )
  }

}
