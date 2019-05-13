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

package controllers.registration

import java.util.UUID

import config.{LocalFormPartialRetriever, PbikAppConfig, PbikContext}
import connectors.HmrcTierConnector
import controllers.{ExternalUrls, WhatNextPageController}
import controllers.actions.{AuthAction, NoSessionCheckAction}
import javax.inject.Inject
import models._
import play.api.Mode.Mode
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.{Configuration, Environment}
import play.twirl.api.HtmlFormat
import services.{BikListService, RegistrationService}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionKeys}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{ControllersReferenceData, URIInformation, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ManageRegistrationController @Inject()(implicit val pbikAppConfig: PbikAppConfig,
                                             registrationService: RegistrationService,
                                             val bikListService: BikListService,
                                             tierConnector: HmrcTierConnector,
                                             val authenticate: AuthAction,
                                             val noSessionCheck: NoSessionCheckAction,
                                             val runModeConfiguration: Configuration,
                                             environment: Environment,
                                             taxDateUtils: TaxDateUtils,
                                             implicit val context: PbikContext,
                                             whatNextPageController: WhatNextPageController,
                                             controllersReferenceData: ControllersReferenceData,
                                             splunkLogger: SplunkLogger,
                                             implicit val uriInformation: URIInformation,
                                             implicit val externalURLs: ExternalUrls,
                                             implicit val localFormPartialRetriever: LocalFormPartialRetriever
                                            ) extends FrontendController {

  val mode: Mode = environment.mode

  def nextTaxYearAddOnPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val staticDataRequest = registrationService.generateViewForBikRegistrationSelection(controllersReferenceData.YEAR_RANGE.cy,
        cachingSuffix = "add", generateViewBasedOnFormItems = views.html.registration.nextTaxYear(_, true, controllersReferenceData.YEAR_RANGE, _, _, _, _, _, empRef = request.empRef))
      controllersReferenceData.responseErrorHandler(staticDataRequest)
  }

  def nextTaxYearRemoveOnPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val staticDataRequest = loadNextTaxYearOnRemoveData
      controllersReferenceData.responseErrorHandler(staticDataRequest)
  }

  def currentTaxYearOnPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        result <- registrationService.generateViewForBikRegistrationSelection(controllersReferenceData.YEAR_RANGE.cyminus1,
          cachingSuffix = "add", generateViewBasedOnFormItems = views.html.registration.currentTaxYear(_, controllersReferenceData.YEAR_RANGE, _, _, _, _, _, empRef = request.empRef))
      } yield {
        result
      }

      controllersReferenceData.responseCheckCYEnabled(resultFuture)
  }

  def loadNextTaxYearOnRemoveData(implicit request: AuthenticatedRequest[AnyContent], hc: HeaderCarrier): Future[Result] = {
    val taxYearRange = taxDateUtils.getTaxYearRange()
    val loadResultFuture = for {
      registeredListOption <- tierConnector.genericGetCall[List[Bik]](uriInformation.baseUrl, uriInformation.getRegisteredPath,
        request.empRef, controllersReferenceData.YEAR_RANGE.cy)
    } yield {
      val fetchFromCacheMapBiksValue = List.empty[RegistrationItem]
      val fetchFromCacheMapSelectAllValue = ""
      val initialData = RegistrationList(None, registeredListOption.map { x =>
        RegistrationItem(x.iabdType, active = false, enabled = true)
      })
      val sortedData = BikListUtils.sortRegistrationsAlphabeticallyByLabels(initialData)
      if (sortedData.active.isEmpty) {
        Ok(views.html.errorPage(controllersReferenceData.NO_MORE_BENEFITS_TO_REMOVE_CY1,
          taxYearRange,
          FormMappingsConstants.CYP1,
          -1,
          "Registered benefits for tax year starting 6 April " + controllersReferenceData.YEAR_RANGE.cy,
          "manage-registrations",
          empRef = Some(request.empRef)))
      }
      else {
        Ok(views.html.registration.nextTaxYear(
          bikForm = uriInformation.objSelectedForm.fill(sortedData),
          additive = false,
          taxYearRange = taxYearRange,
          previouslySelectedBenefits = fetchFromCacheMapBiksValue,
          registeredBiks = List.empty[Bik],
          nonLegislationBiks = List.empty[Int],
          decommissionedBiks = List.empty[Int],
          biksAvailableCount = None,
          empRef = request.empRef
        ))
      }
    }
    controllersReferenceData.responseErrorHandler(loadResultFuture)
  }

  def confirmAddCurrentTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        biksListOption: List[Bik] <- bikListService.registeredBenefitsList(controllersReferenceData.YEAR_RANGE.cyminus1, EmpRef.empty)(uriInformation.getBenefitTypesPath)
        result <- generateConfirmationScreenView(controllersReferenceData.YEAR_RANGE.cyminus1, cachingSuffix = "add", generateViewBasedOnFormItems = views.html.registration.
          confirmAddCurrentTaxYear(_, controllersReferenceData.YEAR_RANGE, empRef = request.empRef), viewToRedirect = formWithErrors =>
          Ok(views.html.registration.currentTaxYear(formWithErrors,
            controllersReferenceData.YEAR_RANGE,
            registeredBiks = List.empty[Bik],
            nonLegislationBiks=pbikAppConfig.biksNotSupportedCY,
            decommissionedBiks=pbikAppConfig.biksDecommissioned,
            biksAvailableCount = Some(biksListOption.size),
            empRef = request.empRef))
            .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}")))
      } yield {
        result
      }

      controllersReferenceData.responseCheckCYEnabled(resultFuture)
  }


  def confirmAddNextTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        biksListOption: List[Bik] <- bikListService.registeredBenefitsList(controllersReferenceData.YEAR_RANGE.cy, EmpRef.empty)(uriInformation.getBenefitTypesPath)
        result <- generateConfirmationScreenView(controllersReferenceData.YEAR_RANGE.cy, cachingSuffix = "add", generateViewBasedOnFormItems = views.html.registration.
          confirmUpdateNextTaxYear(_, additive = true, controllersReferenceData.YEAR_RANGE, empRef = request.empRef), viewToRedirect = formWithErrors =>
          Ok(views.html.registration.nextTaxYear(
            bikForm = formWithErrors,
            additive = true,
            taxYearRange = controllersReferenceData.YEAR_RANGE,
            nonLegislationBiks = pbikAppConfig.biksNotSupported,
            decommissionedBiks = pbikAppConfig.biksDecommissioned,
            biksAvailableCount = Some(biksListOption.size),
            empRef = request.empRef
          )))
      } yield {
        result
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def confirmRemoveNextTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        result <- generateConfirmationScreenView(controllersReferenceData.YEAR_RANGE.cy, cachingSuffix = "remove", generateViewBasedOnFormItems = views.html.registration.
          confirmUpdateNextTaxYear(_, additive = false, controllersReferenceData.YEAR_RANGE, empRef = request.empRef), viewToRedirect = formWithErrors =>
          Ok(views.html.registration.nextTaxYear(
            bikForm = formWithErrors,
            additive = false,
            taxYearRange = controllersReferenceData.YEAR_RANGE,
            nonLegislationBiks = pbikAppConfig.biksNotSupported,
            decommissionedBiks = pbikAppConfig.biksDecommissioned,
            biksAvailableCount = None,
            empRef = request.empRef
          )))
      } yield {
        result
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def confirmRemoveNextTaxYearNoForm(iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val registrationList = RegistrationList(None, List(RegistrationItem(iabdType, active = true, enabled = false)), reason = None)
      val form: Form[RegistrationList] = uriInformation.objSelectedForm.fill(registrationList)
      val resultFuture = Future.successful(
        Ok(views.html.registration.confirmUpdateNextTaxYear(uriInformation.objSelectedForm.fill(form.get),
          additive = false,
          controllersReferenceData.YEAR_RANGE,
          empRef = request.empRef)))
      controllersReferenceData.responseErrorHandler(resultFuture)
  }


  def removeNextYearRegisteredBenefitTypes: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val persistentBiks: List[Bik] = controllersReferenceData.generateListOfBiksBasedOnForm(controllersReferenceData.BIK_REMOVE_STATUS)
      val registeredFuture = updateBiksFutureAction(controllersReferenceData.YEAR_RANGE.cy, persistentBiks, additive = false)
      controllersReferenceData.responseErrorHandler(registeredFuture)
  }

  def generateConfirmationScreenView(year: Int, cachingSuffix: String,
                                     generateViewBasedOnFormItems: Form[RegistrationList] =>
                                       HtmlFormat.Appendable, viewToRedirect: Form[RegistrationList] =>
    Result)(implicit hc: HeaderCarrier,
            request: Request[AnyContent]): Future[Result] = {

    uriInformation.objSelectedForm.bindFromRequest.fold(
      formWithErrors => Future.successful(viewToRedirect(formWithErrors))
      ,
      values => {

        val items: List[RegistrationItem] = values.active.filter(x => x.active)
        Future.successful(
          Ok(generateViewBasedOnFormItems(uriInformation.objSelectedForm.fill(RegistrationList(None, items, None)))))

      }
    )
  }

  def updateRegisteredBenefitTypes: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = {

        val persistentBiks: List[Bik] = controllersReferenceData.generateListOfBiksBasedOnForm(controllersReferenceData.BIK_ADD_STATUS)
        updateBiksFutureAction(controllersReferenceData.YEAR_RANGE.cyminus1, persistentBiks, additive = true)
      }
      controllersReferenceData.responseCheckCYEnabled(resultFuture)
  }

  def addNextYearRegisteredBenefitTypes: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val persistentBiks: List[Bik] = controllersReferenceData.generateListOfBiksBasedOnForm(controllersReferenceData.BIK_ADD_STATUS)
      val actionFuture = updateBiksFutureAction(controllersReferenceData.YEAR_RANGE.cy, persistentBiks, additive = true)
      controllersReferenceData.responseErrorHandler(actionFuture)
  }

  def updateBiksFutureAction(year: Int, persistentBiks: List[Bik], additive: Boolean)
                            (implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    tierConnector.genericGetCall[List[Bik]](uriInformation.baseUrl, uriInformation.getRegisteredPath,
      request.empRef, year).flatMap {
      registeredResponse =>
        val form = uriInformation.objSelectedForm.bindFromRequest()

        form.fold(
          formWithErrors => Future.successful(
            Ok(views.html.registration.confirmUpdateNextTaxYear(formWithErrors,
              additive,
              controllersReferenceData.YEAR_RANGE,
              empRef = request.empRef)))
          ,
          values => {
            val changes = BikListUtils.normaliseSelectedBenefits(registeredResponse, persistentBiks)
            if (additive) {
              // Process registration
              val saveFuture = tierConnector.genericPostCall(uriInformation.baseUrl, uriInformation.updateBenefitTypesPath,
                request.empRef, year, changes)
              saveFuture.map {
                saveResponse: HttpResponse =>
                  auditBikUpdate(additive = true, year, persistentBiks)
                  whatNextPageController.loadWhatNextRegisteredBIK(form, year)
              }
            } else {
              // Remove benefit - if there are no errors proceed
              Future(removeBenefitReasonValidation(values, form, year, persistentBiks, changes))
            }
          }
        )
    }
  }

  def removeBenefitReasonValidation(registrationList: RegistrationList, form: Form[RegistrationList], year: Int,
                                    persistentBiks: List[Bik], changes: List[Bik])
                                   (implicit request: AuthenticatedRequest[AnyContent]): Result = {
    registrationList.reason match {
      case Some(reasonValue) if controllersReferenceData.BIK_REMOVE_REASON_LIST.contains(reasonValue.selectionValue) => {
        reasonValue.info match {
          case _ if reasonValue.selectionValue.equals("other") && reasonValue.info.getOrElse("").trim.isEmpty => {
            Redirect(routes.ManageRegistrationController.confirmRemoveNextTaxYearNoForm(uriInformation.iabdValueURLMapper(persistentBiks.head.iabdType)
            )).flashing("error" -> Messages("RemoveBenefits.reason.other.required"))
          }
          case Some(info) => {
            tierConnector.genericPostCall(uriInformation.baseUrl, uriInformation.updateBenefitTypesPath,
              request.empRef, year, changes)
            auditBikUpdate(additive = false, year, persistentBiks, Some((reasonValue.selectionValue.toUpperCase, Some(info))))
            whatNextPageController.loadWhatNextRemovedBIK(form, year)
          }
          case _ => {
            tierConnector.genericPostCall(uriInformation.baseUrl, uriInformation.updateBenefitTypesPath,
              request.empRef, year, changes)
            auditBikUpdate(additive = false, year, persistentBiks, Some((reasonValue.selectionValue.toUpperCase, None)))
            whatNextPageController.loadWhatNextRemovedBIK(form, year)
          }
        }
      }
      case _ => Redirect(routes.ManageRegistrationController.confirmRemoveNextTaxYearNoForm(uriInformation.iabdValueURLMapper(persistentBiks.head.iabdType)
      )).flashing("error" -> Messages("RemoveBenefits.reason.no.selection"))
    }
  }

  private def auditBikUpdate(additive: Boolean, year: Int, persistentBiks: List[Bik], removeReason: Option[(String, Option[String])] = None)
                            (implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Unit = {
    val derivedMsg = if (additive) "Benefit added to " + splunkLogger.taxYearToSpPeriod(year) else "Benefit removed from " + splunkLogger.taxYearToSpPeriod(year)
    for (bik <- persistentBiks) {
      splunkLogger.logSplunkEvent(splunkLogger.createDataEvent(
        tier = splunkLogger.spTier.FRONTEND,
        action = if (additive) splunkLogger.spAction.ADD else splunkLogger.spAction.REMOVE,
        target = splunkLogger.spTarget.BIK,
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
