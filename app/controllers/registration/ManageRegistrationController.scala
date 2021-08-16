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

package controllers.registration

import config.PbikAppConfig
import connectors.HmrcTierConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models._
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import services.{BikListService, RegistrationService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{ControllersReferenceData, URIInformation, _}
import views.html.registration._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Logging

@Singleton
class ManageRegistrationController @Inject()(
  bikListUtils: BikListUtils,
  registrationService: RegistrationService,
  formMappings: FormMappings,
  override val messagesApi: MessagesApi,
  cc: MessagesControllerComponents,
  val bikListService: BikListService,
  tierConnector: HmrcTierConnector,
  val authenticate: AuthAction,
  val noSessionCheck: NoSessionCheckAction,
  val cachingService: SessionService,
  controllersReferenceData: ControllersReferenceData,
  splunkLogger: SplunkLogger,
  pbikAppConfig: PbikAppConfig,
  uriInformation: URIInformation,
  nextTaxYearView: NextTaxYear,
  currentTaxYearView: CurrentTaxYear,
  confirmAddCurrentTaxYearView: ConfirmAddCurrentTaxYear,
  confirmUpdateNextTaxYearView: ConfirmUpdateNextTaxYear,
  removeBenefitNextTaxYearView: RemoveBenefitNextTaxYear)
    extends FrontendController(cc) with I18nSupport with Logging {

  def nextTaxYearAddOnPageLoad: Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val staticDataRequest = registrationService.generateViewForBikRegistrationSelection(
        controllersReferenceData.yearRange.cy,
        cachingSuffix = "add",
        generateViewBasedOnFormItems =
          nextTaxYearView(_, true, controllersReferenceData.yearRange, _, _, _, _, _, empRef = request.empRef)
      )
      controllersReferenceData.responseErrorHandler(staticDataRequest)
    }

  def currentTaxYearOnPageLoad: Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val staticDataRequest = registrationService.generateViewForBikRegistrationSelection(
        controllersReferenceData.yearRange.cyminus1,
        cachingSuffix = "add",
        generateViewBasedOnFormItems =
          currentTaxYearView(_, controllersReferenceData.yearRange, _, _, _, _, _, empRef = request.empRef)
      )
      controllersReferenceData.responseCheckCYEnabled(staticDataRequest)
    }

  def checkYourAnswersAddCurrentTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        biksListOption: List[Bik] <- bikListService.registeredBenefitsList(
                                      controllersReferenceData.yearRange.cyminus1,
                                      EmpRef.empty)(uriInformation.getBenefitTypesPath)
        result <- formMappings.objSelectedForm.bindFromRequest.fold(
                   formWithErrors =>
                     Future.successful(
                       BadRequest(currentTaxYearView(
                         formWithErrors,
                         controllersReferenceData.yearRange,
                         registeredBiks = List.empty[Bik],
                         nonLegislationBiks = pbikAppConfig.biksNotSupportedCY,
                         decommissionedBiks = pbikAppConfig.biksDecommissioned,
                         biksAvailableCount = Some(biksListOption.size),
                         empRef = request.empRef
                       ))),
                   values => {
                     cachingService.cacheRegistrationList(values).flatMap { _ =>
                       Future.successful(
                         Redirect(routes.ManageRegistrationController.showCheckYourAnswersAddCurrentTaxYear))
                     }
                   }
                 )
      } yield result
      controllersReferenceData.responseCheckCYEnabled(resultFuture)
  }

  def showCheckYourAnswersAddCurrentTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = cachingService.fetchPbikSession().flatMap { session =>
        val registrationList = RegistrationList(None, session.get.registrations.get.active.filter(_.active), None)
        val form: Form[RegistrationList] = formMappings.objSelectedForm.fill(registrationList)
        Future.successful(
          Ok(confirmAddCurrentTaxYearView(form, controllersReferenceData.yearRange, empRef = request.empRef)))
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def checkYourAnswersAddNextTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        biksListOption: List[Bik] <- bikListService.registeredBenefitsList(
                                      controllersReferenceData.yearRange.cy,
                                      EmpRef.empty)(uriInformation.getBenefitTypesPath)
        result <- formMappings.objSelectedForm.bindFromRequest.fold(
                   formWithErrors =>
                     Future.successful(
                       BadRequest(nextTaxYearView(
                         form = formWithErrors,
                         additive = true,
                         taxYearRange = controllersReferenceData.yearRange,
                         nonLegislationBiks = pbikAppConfig.biksNotSupported,
                         decommissionedBiks = pbikAppConfig.biksDecommissioned,
                         biksAvailableCount = Some(biksListOption.size),
                         empRef = request.empRef
                       ))),
                   values => {
                     cachingService.cacheRegistrationList(values).flatMap { _ =>
                       Future.successful(
                         Redirect(routes.ManageRegistrationController.showCheckYourAnswersAddNextTaxYear))
                     }
                   }
                 )
      } yield result
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def showCheckYourAnswersAddNextTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = cachingService.fetchPbikSession().flatMap { session =>
        val registrationList = RegistrationList(None, session.get.registrations.get.active.filter(_.active), None)
        Future.successful(Ok(
          confirmUpdateNextTaxYearView(registrationList, controllersReferenceData.yearRange, empRef = request.empRef)))
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def checkYourAnswersRemoveNextTaxYear(iabdType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      controllersReferenceData.responseErrorHandler(
        showCheckYourAnswersRemoveNextTaxYear(iabdType, formMappings.removalReasonForm))
    }

  def showCheckYourAnswersRemoveNextTaxYear(iabdType: String, form: Form[BinaryRadioButtonWithDesc])(
    implicit request: AuthenticatedRequest[_]): Future[Result] = {
    val bikToRemove = RegistrationItem(iabdType, active = true, enabled = true)
    val registrationList =
      RegistrationList(None, List(bikToRemove), reason = None)
    cachingService.cacheBikRemoved(
      RegistrationItem(uriInformation.iabdValueURLDeMapper(iabdType), active = false, enabled = true))
    Future.successful(
      Ok(
        removeBenefitNextTaxYearView(
          registrationList,
          Some(bikToRemove),
          controllersReferenceData.yearRange,
          form,
          empRef = request.empRef
        )))
  }

  //TODO iabdType is not used
  def removeNextYearRegisteredBenefitTypes(iabdType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val registeredFuture = cachingService.fetchPbikSession().flatMap { session =>
        val bikToRemove = Bik(session.get.bikRemoved.get.id, ControllersReferenceDataCodes.BIK_REMOVE_STATUS)
        updateBiksFutureAction(controllersReferenceData.yearRange.cy, List(bikToRemove), additive = false)
      }
      controllersReferenceData.responseErrorHandler(registeredFuture)
    }

  def updateCurrentYearRegisteredBenefitTypes(): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val actionFuture = cachingService.fetchPbikSession().flatMap { session =>
        val persistentBiks = session.get.registrations.get.active
          .filter(biks => biks.active)
          .map(bik => Bik(bik.id, ControllersReferenceDataCodes.BIK_ADD_STATUS))
        updateBiksFutureAction(controllersReferenceData.yearRange.cyminus1, persistentBiks, additive = true)
      }
      controllersReferenceData.responseCheckCYEnabled(actionFuture)
  }

  def addNextYearRegisteredBenefitTypes(): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val actionFuture = cachingService.fetchPbikSession().flatMap { session =>
        val persistentBiks = session.get.registrations.get.active
          .filter(biks => biks.active)
          .map(bik => Bik(bik.id, ControllersReferenceDataCodes.BIK_ADD_STATUS))
        updateBiksFutureAction(controllersReferenceData.yearRange.cy, persistentBiks, additive = true)
      }
      controllersReferenceData.responseErrorHandler(actionFuture)
  }

  def updateBiksFutureAction(year: Int, persistentBiks: List[Bik], additive: Boolean)(
    implicit request: AuthenticatedRequest[AnyContent]): Future[Result] =
    tierConnector
      .genericGetCall[List[Bik]](uriInformation.baseUrl, uriInformation.getRegisteredPath, request.empRef, year)
      .flatMap { registeredResponse =>
        cachingService.fetchPbikSession().flatMap { session =>
          val changes = bikListUtils.normaliseSelectedBenefits(registeredResponse, persistentBiks)
          if (additive) {
            // Process registration
            val saveFuture = tierConnector.genericPostCall(
              uriInformation.baseUrl,
              uriInformation.updateBenefitTypesPath,
              request.empRef,
              year,
              changes)
            saveFuture.map { _ =>
              auditBikUpdate(additive = true, year, persistentBiks)
              lazy val yearRange = controllersReferenceData.yearRange
              lazy val yearString = year match {
                case yearRange.cy       => "cy1"
                case yearRange.cyminus1 => "cy"
              }
              Redirect(controllers.routes.WhatNextPageController.showWhatNextRegisteredBik(yearString))
            }
          } else {
            // Remove benefit - if there are no errors proceed
            formMappings.removalReasonForm
              .bindFromRequest()
              .fold(
                formWithErrors => {
                  logger.warn("[ManageRegistrationController][updateBiksFutureAction] No removal reason selected")
                  showCheckYourAnswersRemoveNextTaxYear(
                    uriInformation.iabdValueURLMapper(persistentBiks.head.iabdType),
                    formWithErrors)
                },
                values => {
                  val listWithReason =
                    RegistrationList(None, session.get.registrations.get.active, reason = Some(values))
                  cachingService.cacheRegistrationList(listWithReason).flatMap { _ =>
                    removeBenefitReasonValidation(listWithReason, year, persistentBiks, changes)
                  }
                }
              )
          }
        }
      }

  def removeBenefitReasonValidation(
    registrationList: RegistrationList,
    year: Int,
    persistentBiks: List[Bik],
    changes: List[Bik])(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] =
    registrationList.reason match {
      case Some(reasonValue)
          if ControllersReferenceDataCodes.BIK_REMOVE_REASON_LIST.contains(reasonValue.selectionValue) =>
        reasonValue.info match {
          case _ if reasonValue.selectionValue.equals("other") && reasonValue.info.getOrElse("").trim.isEmpty =>
            showCheckYourAnswersRemoveNextTaxYear(
              uriInformation.iabdValueURLMapper(persistentBiks.head.iabdType),
              formMappings.removalReasonForm.withError("missingInfo", Messages("RemoveBenefits.reason.other.required"))
            )
          case Some(info) =>
            tierConnector.genericPostCall(
              uriInformation.baseUrl,
              uriInformation.updateBenefitTypesPath,
              request.empRef,
              year,
              changes)
            auditBikUpdate(
              additive = false,
              year,
              persistentBiks,
              Some((reasonValue.selectionValue.toUpperCase, Some(info))))
            Future.successful(Redirect(controllers.routes.WhatNextPageController.showWhatNextRemovedBik))
          case _ =>
            tierConnector.genericPostCall(
              uriInformation.baseUrl,
              uriInformation.updateBenefitTypesPath,
              request.empRef,
              year,
              changes)
            auditBikUpdate(additive = false, year, persistentBiks, Some((reasonValue.selectionValue.toUpperCase, None)))
            Future.successful(Redirect(controllers.routes.WhatNextPageController.showWhatNextRemovedBik))
        }
      case _ =>
        logger.warn(
          s"[ManageRegistrationController][removeBenefitReasonValidation] Couldn't find reason from request form")
        showCheckYourAnswersRemoveNextTaxYear(
          uriInformation.iabdValueURLMapper(persistentBiks.head.iabdType),
          formMappings.removalReasonForm.withError("missingInfo", Messages("RemoveBenefits.reason.no.selection"))
        )
    }

  private def auditBikUpdate(
    additive: Boolean,
    year: Int,
    persistentBiks: List[Bik],
    removeReason: Option[(String, Option[String])] = None)(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_]): Unit = {
    val derivedMsg =
      if (additive) { "Benefit added to " + splunkLogger.taxYearToSpPeriod(year) } else {
        "Benefit removed from " + splunkLogger.taxYearToSpPeriod(year)
      }
    for (bik <- persistentBiks) {
      splunkLogger.logSplunkEvent(
        splunkLogger.createDataEvent(
          tier = splunkLogger.FRONTEND,
          action = if (additive) splunkLogger.ADD else splunkLogger.REMOVE,
          target = splunkLogger.BIK,
          period = splunkLogger.taxYearToSpPeriod(year),
          msg = derivedMsg + " : " + bik.iabdType,
          nino = None,
          iabd = Some(bik.iabdType),
          removeReason = if (additive) None else Some(removeReason.get._1),
          removeReasonDesc = if (additive) None else Some(removeReason.get._2.getOrElse("")),
          name = Some(request.name),
          empRef = Some(request.empRef)
        ))
    }
  }

}
