/*
 * Copyright 2024 HM Revenue & Customs
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
import models._
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import services.{BikListService, RegistrationService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._
import views.html.registration._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageRegistrationController @Inject() (
  bikListUtils: BikListUtils,
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
        cachingSuffix = "add",
        generateViewBasedOnFormItems =
          nextTaxYearView(_, additive = true, controllersReferenceData.yearRange, _, _, _, _, _)
      )
      controllersReferenceData.responseErrorHandler(staticDataRequest)
    }

  def currentTaxYearOnPageLoad: Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val staticDataRequest = registrationService.generateViewForBikRegistrationSelection(
        controllersReferenceData.yearRange.cyminus1,
        cachingSuffix = "add",
        generateViewBasedOnFormItems = currentTaxYearView(_, controllersReferenceData.yearRange, _, _, _, _, _)
      )
      controllersReferenceData.responseCheckCYEnabled(staticDataRequest)
    }

  def checkYourAnswersAddCurrentTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        biksListOption <- bikListService.getAllBenefitsForYear(controllersReferenceData.yearRange.cyminus1)
        result         <- formMappings.objSelectedForm
                            .bindFromRequest()
                            .fold(
                              formWithErrors =>
                                Future.successful(
                                  BadRequest(
                                    currentTaxYearView(
                                      formWithErrors,
                                      controllersReferenceData.yearRange,
                                      registeredBiks = List.empty[Bik],
                                      nonLegislationBiks = pbikAppConfig.biksNotSupportedCY.map(_.id).toList, //TODO List to Set
                                      decommissionedBiks = pbikAppConfig.biksDecommissioned.map(_.id).toList, //TODO List to Set
                                      biksAvailableCount = Some(biksListOption.size)
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
        val activeReg                    = session.flatMap(_.getActiveRegistrationItems()).getOrElse(List.empty[RegistrationItem])
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
        biksListOption <- bikListService.getAllBenefitsForYear(controllersReferenceData.yearRange.cy)
        result         <- formMappings.objSelectedForm
                            .bindFromRequest()
                            .fold(
                              formWithErrors =>
                                Future.successful(
                                  BadRequest(
                                    nextTaxYearView(
                                      form = formWithErrors,
                                      additive = true,
                                      taxYearRange = controllersReferenceData.yearRange,
                                      nonLegislationBiks = pbikAppConfig.biksNotSupported.map(_.id).toList, //TODO List to Set
                                      decommissionedBiks = pbikAppConfig.biksDecommissioned.map(_.id).toList, //TODO List to Set
                                      biksAvailableCount = Some(biksListOption.size)
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
        val activeReg        = session.flatMap(_.getActiveRegistrationItems()).getOrElse(List.empty[RegistrationItem])
        val registrationList = RegistrationList(None, activeReg, None)
        Future.successful(
          Ok(
            confirmUpdateNextTaxYearView(registrationList, controllersReferenceData.yearRange)
          )
        )
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def checkYourAnswersRemoveNextTaxYear(iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      controllersReferenceData.responseErrorHandler(
        showCheckYourAnswersRemoveNextTaxYear(iabdString, formMappings.removalReasonForm)
      )
    }

  private def showCheckYourAnswersRemoveNextTaxYear(iabdString: String, form: Form[BinaryRadioButtonWithDesc])(implicit
    request: AuthenticatedRequest[_]
  ): Future[Result] = {
    val bikToRemove      = RegistrationItem(iabdString, active = true, enabled = true)
    val registrationList =
      RegistrationList(None, List(bikToRemove), reason = None)
    sessionService.storeBikRemoved(
      RegistrationItem(Bik.asNPSTypeValue(iabdString), active = false, enabled = true)
    )
    Future.successful(
      Ok(
        removeBenefitNextTaxYearView(
          registrationList,
          Some(bikToRemove),
          controllersReferenceData.yearRange,
          form
        )
      )
    )
  }

  def showConfirmRemoveNextTaxYear(iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      Future.successful(Ok(confirmRemoveNextTaxYear(iabdString, controllersReferenceData.yearRange)))
    }

  def submitConfirmRemoveNextTaxYear(iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val registeredFuture = sessionService.fetchPbikSession().flatMap { session =>
        tierConnector
          .getRegisteredBiks(request.empRef, controllersReferenceData.yearRange.cy)
          .flatMap { registeredResponse =>
            val bikId          = session.flatMap(_.bikRemoved.map(_.id)).getOrElse("")
            val bikToRemove    = Bik(bikId, ControllersReferenceDataCodes.BIK_REMOVE_STATUS)
            val listWithReason = session
              .flatMap(_.registrations.filter(_.reason.isDefined))
              .getOrElse(RegistrationList(None, List.empty[RegistrationItem], None))
            val persistentBiks = List(bikToRemove)
            val changes        = bikListUtils.normaliseSelectedBenefits(registeredResponse.bikList, persistentBiks)

            removeBenefitReasonValidation(
              listWithReason,
              controllersReferenceData.yearRange.cy,
              persistentBiks,
              changes,
              iabdString
            )
              .map(_ => Redirect(controllers.routes.WhatNextPageController.showWhatNextRemovedBik(iabdString)))
          }
      }
      controllersReferenceData.responseErrorHandler(registeredFuture)
    }

  def removeNextYearRegisteredBenefitTypes(iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val registeredFuture = sessionService.fetchPbikSession().flatMap { session =>
        val bikId       = session.flatMap(_.bikRemoved.map(_.id)).getOrElse("")
        val bikToRemove = Bik(bikId, ControllersReferenceDataCodes.BIK_REMOVE_STATUS)
        updateBiksFutureAction(controllersReferenceData.yearRange.cy, List(bikToRemove), iabdString, additive = false)
      }
      controllersReferenceData.responseErrorHandler(registeredFuture)
    }

  def showRemoveBenefitOtherReason(iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      Future.successful(
        Ok(removeBenefitOtherReason(formMappings.removalOtherReasonForm, iabdString))
      )
    }

  def submitRemoveBenefitOtherReason(iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      formMappings.removalOtherReasonForm
        .bindFromRequest()
        .fold(
          formWithErrors => {
            logger.warn("[ManageRegistrationController][submitRemoveBenefitOtherReason] No removal reason entered")
            Future.successful(BadRequest(removeBenefitOtherReason(formWithErrors, iabdString)))
          },
          values => {
            val registeredFuture = sessionService.fetchPbikSession().flatMap { session =>
              val bikId       = session.flatMap(_.bikRemoved.map(_.id)).getOrElse("")
              val bikToRemove = Bik(bikId, ControllersReferenceDataCodes.BIK_REMOVE_STATUS)
              updateRemoveBenefitsOther(bikToRemove, values)
            }
            controllersReferenceData.responseErrorHandler(registeredFuture)
          }
        )
    }

  def updateCurrentYearRegisteredBenefitTypes(): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val actionFuture = sessionService.fetchPbikSession().flatMap { session =>
        val activeReg = session.flatMap(_.getActiveRegistrationItems()).getOrElse(List.empty[RegistrationItem])

        val persistentBiks = activeReg
          .filter(biks => biks.active)
          .map(bik => Bik(bik.id, ControllersReferenceDataCodes.BIK_ADD_STATUS))
        updateBiksFutureAction(controllersReferenceData.yearRange.cyminus1, persistentBiks, additive = true)
      }
      controllersReferenceData.responseCheckCYEnabled(actionFuture)
  }

  def addNextYearRegisteredBenefitTypes(): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val actionFuture = sessionService.fetchPbikSession().flatMap { session =>
        val activeReg = session.flatMap(_.getActiveRegistrationItems()).getOrElse(List.empty[RegistrationItem])

        val persistentBiks = activeReg
          .filter(biks => biks.active)
          .map(bik => Bik(bik.id, ControllersReferenceDataCodes.BIK_ADD_STATUS))
        updateBiksFutureAction(controllersReferenceData.yearRange.cy, persistentBiks, additive = true)
      }
      controllersReferenceData.responseErrorHandler(actionFuture)
  }

  def updateBiksFutureAction(year: Int, persistentBiks: List[Bik], iabdString: String = "", additive: Boolean)(implicit
    request: AuthenticatedRequest[AnyContent]
  ): Future[Result] =
    tierConnector
      .getRegisteredBiks(request.empRef, year)
      .flatMap { registeredResponse =>
        sessionService.fetchPbikSession().flatMap { session =>
          val changes = bikListUtils.normaliseSelectedBenefits(registeredResponse.bikList, persistentBiks)
          if (additive) {
            // Process registration
            tierConnector.updateOrganisationsRegisteredBiks(year, changes).map { _ =>
              auditBikUpdate(additive = true, year, persistentBiks)
              lazy val yearRange  = controllersReferenceData.yearRange
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
                    persistentBiks.head.asBenefitString,
                    formWithErrors
                  )
                },
                values =>
                  values.selectionValue match {
                    case ControllersReferenceDataCodes.OTHER =>
                      Future.successful(
                        Redirect(
                          routes.ManageRegistrationController.showRemoveBenefitOtherReason(
                            persistentBiks.head.asBenefitString
                          )
                        )
                      )
                    case _                                   =>
                      val activeReg =
                        session.flatMap(_.getActiveRegistrationItems()).getOrElse(List.empty[RegistrationItem])

                      val listWithReason =
                        RegistrationList(None, activeReg, reason = Some(values))
                      sessionService.storeRegistrationList(listWithReason).flatMap { _ =>
                        Future.successful(
                          Redirect(
                            routes.ManageRegistrationController.showConfirmRemoveNextTaxYear(
                              persistentBiks.head.asBenefitString
                            )
                          )
                        )
                      }
                  }
              )
          }
        }
      }

  private def updateRemoveBenefitsOther(
    persistentBik: Bik,
    otherReason: OtherReason
  )(implicit
    request: AuthenticatedRequest[AnyContent]
  ): Future[Result] =
    sessionService.fetchPbikSession().flatMap { session =>
      val activeReg      = session.flatMap(_.getActiveRegistrationItems()).getOrElse(List.empty[RegistrationItem])
      val listWithReason =
        RegistrationList(
          None,
          activeReg,
          reason = Some(BinaryRadioButtonWithDesc(ControllersReferenceDataCodes.OTHER, Some(otherReason.reason)))
        )
      sessionService.storeRegistrationList(listWithReason).flatMap { _ =>
        Future.successful(
          Redirect(
            routes.ManageRegistrationController.showConfirmRemoveNextTaxYear(
              persistentBik.asBenefitString
            )
          )
        )
      }
    }

  def removeBenefitReasonValidation(
    registrationList: RegistrationList,
    year: Int,
    persistentBiks: List[Bik],
    changes: List[Bik],
    iabdString: String
  )(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] =
    registrationList.reason match {
      case Some(reasonValue)
          if ControllersReferenceDataCodes.BIK_REMOVE_REASON_LIST.contains(reasonValue.selectionValue) =>
        reasonValue.info match {
          case Some(info) =>
            tierConnector.updateOrganisationsRegisteredBiks(year, changes)
            auditBikUpdate(
              additive = false,
              year,
              persistentBiks,
              Some((reasonValue.selectionValue.toUpperCase, Some(info)))
            )
            Future.successful(Redirect(controllers.routes.WhatNextPageController.showWhatNextRemovedBik(iabdString)))
          case _          =>
            tierConnector.updateOrganisationsRegisteredBiks(year, changes)
            auditBikUpdate(additive = false, year, persistentBiks, Some((reasonValue.selectionValue.toUpperCase, None)))
            Future.successful(Redirect(controllers.routes.WhatNextPageController.showWhatNextRemovedBik(iabdString)))
        }
      case _ =>
        logger.warn(
          s"[ManageRegistrationController][removeBenefitReasonValidation] Couldn't find reason from request form"
        )
        showCheckYourAnswersRemoveNextTaxYear(
          persistentBiks.head.asBenefitString,
          formMappings.removalReasonForm.withError("missingInfo", Messages("RemoveBenefits.reason.no.selection"))
        )
    }

  private def auditBikUpdate(
    additive: Boolean,
    year: Int,
    persistentBiks: List[Bik],
    removeReason: Option[(String, Option[String])] = None
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Unit = {
    val derivedMsg =
      if (additive) { "Benefit added to " + splunkLogger.taxYearToSpPeriod(year) }
      else {
        "Benefit removed from " + splunkLogger.taxYearToSpPeriod(year)
      }
    for (bik <- persistentBiks)
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
        )
      )
  }

}
