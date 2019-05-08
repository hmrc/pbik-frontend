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

import _root_.models._
import config.{AppConfig, PbikAppConfig}
import connectors.{HmrcTierConnector, TierConnector}
import controllers.WhatNextPageController
import controllers.actions.{AuthAction, NoSessionCheckAction}
import play.api.Play
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.HtmlFormat
import services.{BikListService, RegistrationService}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionKeys}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ManageRegistrationController extends ManageRegistrationController with TierConnector {
  val pbikAppConfig: AppConfig = PbikAppConfig

  def registrationService: RegistrationService = RegistrationService

  def bikListService: BikListService = BikListService

  val tierConnector = new HmrcTierConnector
  val authenticate: AuthAction = Play.current.injector.instanceOf[AuthAction]
  val noSessionCheck: NoSessionCheckAction = Play.current.injector.instanceOf[NoSessionCheckAction]
}

trait ManageRegistrationController extends FrontendController
  with URIInformation
  with WhatNextPageController
  with ControllersReferenceData
  with SplunkLogger {
  this: TierConnector =>

  def bikListService: BikListService

  def registrationService: RegistrationService

  val authenticate: AuthAction
  val noSessionCheck: NoSessionCheckAction

  def nextTaxYearAddOnPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val staticDataRequest = registrationService.generateViewForBikRegistrationSelection(YEAR_RANGE.cy,
        cachingSuffix = "add", generateViewBasedOnFormItems = views.html.registration.nextTaxYear(_, true, YEAR_RANGE, _, _, _, _, _, empRef = request.empRef))
      responseErrorHandler(staticDataRequest)
  }

  def nextTaxYearRemoveOnPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val staticDataRequest = loadNextTaxYearOnRemoveData
      responseErrorHandler(staticDataRequest)
  }

  def currentTaxYearOnPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        result <- registrationService.generateViewForBikRegistrationSelection(YEAR_RANGE.cyminus1,
          cachingSuffix = "add", generateViewBasedOnFormItems = views.html.registration.currentTaxYear(_, YEAR_RANGE, _, _, _, _, _, empRef = request.empRef))
      } yield {
        result
      }

      responseCheckCYEnabled(resultFuture)
  }

  def loadNextTaxYearOnRemoveData(implicit request: AuthenticatedRequest[AnyContent], hc: HeaderCarrier): Future[Result] = {
    val taxYearRange = TaxDateUtils.getTaxYearRange()
    val loadResultFuture = for {
      registeredListOption <- tierConnector.genericGetCall[List[Bik]](baseUrl, getRegisteredPath,
        request.empRef, YEAR_RANGE.cy)
    } yield {
      val fetchFromCacheMapBiksValue = List.empty[RegistrationItem]
      val fetchFromCacheMapSelectAllValue = ""
      val initialData = RegistrationList(None, registeredListOption.map { x =>
        RegistrationItem(x.iabdType, active = false, enabled = true)
      })
      val sortedData = BikListUtils.sortRegistrationsAlphabeticallyByLabels(initialData)
      if (sortedData.active.isEmpty) {
        Ok(views.html.errorPage(NO_MORE_BENEFITS_TO_REMOVE_CY1,
          taxYearRange,
          FormMappingsConstants.CYP1,
          -1,
          "Registered benefits for tax year starting 6 April " + YEAR_RANGE.cy,
          "manage-registrations",
          empRef = Some(request.empRef)))
      }
      else {
        Ok(views.html.registration.nextTaxYear(objSelectedForm.fill(sortedData),
          additive = false,
          taxYearRange,
          fetchFromCacheMapBiksValue,
          List.empty[Bik],
          List.empty[Int],
          List.empty[Int],
          None,
          empRef = request.empRef
        ))
      }
    }
    responseErrorHandler(loadResultFuture)
  }

  def confirmAddCurrentTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        biksListOption: List[Bik] <- bikListService.registeredBenefitsList(YEAR_RANGE.cyminus1, EmpRef.empty)(getBenefitTypesPath)
        result <- generateConfirmationScreenView(YEAR_RANGE.cyminus1, cachingSuffix = "add", generateViewBasedOnFormItems = views.html.registration.
          confirmAddCurrentTaxYear(_, YEAR_RANGE, empRef = request.empRef), viewToRedirect = formWithErrors =>
          Ok(views.html.registration.currentTaxYear(formWithErrors,
            YEAR_RANGE,
            biksAvailableCount = Some(biksListOption.size),
            empRef = request.empRef))
            .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}")))
      } yield {
        result
      }

      responseCheckCYEnabled(resultFuture)
  }


  def confirmAddNextTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        biksListOption: List[Bik] <- bikListService.registeredBenefitsList(YEAR_RANGE.cy, EmpRef.empty)(getBenefitTypesPath)
        result <- generateConfirmationScreenView(YEAR_RANGE.cy, cachingSuffix = "add", generateViewBasedOnFormItems = views.html.registration.
          confirmUpdateNextTaxYear(_, additive = true, YEAR_RANGE, empRef = request.empRef), viewToRedirect = formWithErrors =>
          Ok(views.html.registration.nextTaxYear(formWithErrors,
            additive = true,
            YEAR_RANGE,
            biksAvailableCount = Some(biksListOption.size),
            empRef = request.empRef
          )))
      } yield {
        result
      }
      responseErrorHandler(resultFuture)
  }

  def confirmRemoveNextTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        result <- generateConfirmationScreenView(YEAR_RANGE.cy, cachingSuffix = "remove", generateViewBasedOnFormItems = views.html.registration.
          confirmUpdateNextTaxYear(_, additive = false, YEAR_RANGE, empRef = request.empRef), viewToRedirect = formWithErrors =>
          Ok(views.html.registration.nextTaxYear(formWithErrors,
            additive = false,
            YEAR_RANGE,
            biksAvailableCount = None,
            empRef = request.empRef
          )))
      } yield {
        result
      }
      responseErrorHandler(resultFuture)
  }

  def confirmRemoveNextTaxYearNoForm(iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val registrationList = RegistrationList(None, List(RegistrationItem(iabdType, active = true, enabled = false)), reason = None)
      val form: Form[RegistrationList] = objSelectedForm.fill(registrationList)
      val resultFuture = Future.successful(
        Ok(views.html.registration.confirmUpdateNextTaxYear(objSelectedForm.fill(form.get),
          additive = false,
          YEAR_RANGE,
          empRef = request.empRef)))
      responseErrorHandler(resultFuture)
  }


  def removeNextYearRegisteredBenefitTypes: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val persistentBiks: List[Bik] = generateListOfBiksBasedOnForm(BIK_REMOVE_STATUS)
      val registeredFuture = updateBiksFutureAction(YEAR_RANGE.cy, persistentBiks, additive = false)
      responseErrorHandler(registeredFuture)
  }

  def generateConfirmationScreenView(year: Int, cachingSuffix: String,
                                     generateViewBasedOnFormItems: Form[RegistrationList] =>
                                       HtmlFormat.Appendable, viewToRedirect: Form[RegistrationList] =>
    Result)(implicit hc: HeaderCarrier,
            request: Request[AnyContent]): Future[Result] = {

    objSelectedForm.bindFromRequest.fold(
      formWithErrors => Future.successful(viewToRedirect(formWithErrors))
      ,
      values => {

        val items: List[RegistrationItem] = values.active.filter(x => x.active)
        Future.successful(
          Ok(generateViewBasedOnFormItems(objSelectedForm.fill(RegistrationList(None, items, None)))))

      }
    )
  }

  def updateRegisteredBenefitTypes: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
      implicit request =>
        val resultFuture = {

          val persistentBiks: List[Bik] = generateListOfBiksBasedOnForm(BIK_ADD_STATUS)
          updateBiksFutureAction(YEAR_RANGE.cyminus1, persistentBiks, additive = true)
        }
        responseCheckCYEnabled(resultFuture)
  }

  def addNextYearRegisteredBenefitTypes: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
      implicit request =>
        val persistentBiks: List[Bik] = generateListOfBiksBasedOnForm(BIK_ADD_STATUS)
        val actionFuture = updateBiksFutureAction(YEAR_RANGE.cy, persistentBiks, additive = true)
        responseErrorHandler(actionFuture)
  }

  def updateBiksFutureAction(year: Int, persistentBiks: List[Bik], additive: Boolean)
                            (implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    tierConnector.genericGetCall[List[Bik]](baseUrl, getRegisteredPath,
      request.empRef, year).flatMap {
      registeredResponse =>
        val form = objSelectedForm.bindFromRequest()

        form.fold(
          formWithErrors => Future.successful(
            Ok(views.html.registration.confirmUpdateNextTaxYear(formWithErrors,
              additive,
              YEAR_RANGE,
              empRef = request.empRef)))
          ,
          values => {
            val changes = BikListUtils.normaliseSelectedBenefits(registeredResponse, persistentBiks)
            additive match {
              case true => {
                // Process registration
                val saveFuture = tierConnector.genericPostCall(baseUrl, updateBenefitTypesPath,
                  request.empRef, year, changes)
                saveFuture.map {
                  saveResponse: HttpResponse =>
                    auditBikUpdate(additive = true, year, persistentBiks)
                    loadWhatNextRegisteredBIK(form, year)
                }
              }
              case _ => {
                // Remove benefit - if there are no errors proceed
                Future(removeBenefitReasonValidation(values, form, year, persistentBiks, changes))
              }
            }
          }
        )
    }
  }

  def removeBenefitReasonValidation(registrationList: RegistrationList, form: Form[RegistrationList], year: Int,
                                    persistentBiks: List[Bik], changes: List[Bik])
                                   (implicit request: AuthenticatedRequest[AnyContent]): Result = {
    registrationList.reason match {
      case Some(reasonValue) if (BIK_REMOVE_REASON_LIST.contains(reasonValue.selectionValue)) => {
        reasonValue.info match {
          case _ if (reasonValue.selectionValue.equals("other") && reasonValue.info.getOrElse("").trim.isEmpty) => {
            Redirect(routes.ManageRegistrationController.confirmRemoveNextTaxYearNoForm(iabdValueURLMapper(persistentBiks.head.iabdType)
            )).flashing("error" -> Messages("RemoveBenefits.reason.other.required"))
          }
          case Some(info) => {
            tierConnector.genericPostCall(baseUrl, updateBenefitTypesPath,
              request.empRef, year, changes)
            auditBikUpdate(false, year, persistentBiks, Some((reasonValue.selectionValue.toUpperCase, Some(info))))
            loadWhatNextRemovedBIK(form, year)
          }
          case _ => {
            tierConnector.genericPostCall(baseUrl, updateBenefitTypesPath,
              request.empRef, year, changes)
            auditBikUpdate(false, year, persistentBiks, Some((reasonValue.selectionValue.toUpperCase, None)))
            loadWhatNextRemovedBIK(form, year)
          }
        }
      }
      case _ => Redirect(routes.ManageRegistrationController.confirmRemoveNextTaxYearNoForm(iabdValueURLMapper(persistentBiks.head.iabdType)
      )).flashing("error" -> Messages("RemoveBenefits.reason.no.selection"))
    }
  }

  private def auditBikUpdate(additive: Boolean, year: Int, persistentBiks: List[Bik], removeReason: Option[(String, Option[String])] = None)
                            (implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]) = {
    val derivedMsg = if (additive) "Benefit added to " + taxYearToSpPeriod(year) else "Benefit removed from " + taxYearToSpPeriod(year)
    for (bik <- persistentBiks) {
      logSplunkEvent(createDataEvent(
        tier = spTier.FRONTEND,
        action = if (additive) spAction.ADD else spAction.REMOVE,
        target = spTarget.BIK,
        period = taxYearToSpPeriod(year),
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
