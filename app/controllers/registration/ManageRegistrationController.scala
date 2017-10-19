/*
 * Copyright 2017 HM Revenue & Customs
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
import config.PbikAppConfig
import connectors.{HmrcTierConnector, TierConnector}
import controllers.WhatNextPageController
import controllers.auth.{AuthenticationConnector, EpayeUser, PbikActions}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc._
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.HtmlFormat
import services.{BikListService, RegistrationService}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.BikListUtils.MandatoryRadioButton
import utils.FormMappingsConstants._
import utils._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.play.frontend.auth.AuthContext
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, SessionKeys }

object ManageRegistrationController extends ManageRegistrationController with TierConnector with AuthenticationConnector {
  def pbikAppConfig = PbikAppConfig
  def registrationService = RegistrationService
  def bikListService = BikListService
  val tierConnector = new HmrcTierConnector

}

trait ManageRegistrationController extends FrontendController with URIInformation
                                                    with WhatNextPageController with ControllersReferenceData with PbikActions
                                                    with EpayeUser with SplunkLogger {

  this: TierConnector =>

  def bikListService: BikListService
  def registrationService: RegistrationService

  def nextTaxYearAddOnPageLoad:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val staticDataRequest = registrationService.generateViewForBikRegistrationSelection(YEAR_RANGE.cy,
          "add", views.html.registration.nextTaxYear(_, true, YEAR_RANGE, _, _, _, _, _))
          responseErrorHandler(staticDataRequest)
  }

  def nextTaxYearRemoveOnPageLoad:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val staticDataRequest = loadNextTaxYearOnRemoveData
        responseErrorHandler(staticDataRequest)
  }

  def currentTaxYearOnPageLoad:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>

        val resultFuture = for {
          result <- registrationService.generateViewForBikRegistrationSelection(YEAR_RANGE.cyminus1,
            "add", views.html.registration.currentTaxYear(_, YEAR_RANGE,  _, _, _, _, _))
        } yield {
          result
        }

        responseCheckCYEnabled(resultFuture)
  }


  def loadNextTaxYearOnRemoveData(implicit ac: AuthContext, request: Request[AnyContent], hc:HeaderCarrier): Future[Result] = {
    val taxYearRange = TaxDateUtils.getTaxYearRange()
    val loadResultFuture = for {
      registeredListOption <- tierConnector.genericGetCall[List[Bik]](baseUrl, getRegisteredPath,
        ac.principal.accounts.epaye.get.empRef.toString, YEAR_RANGE.cy)
    } yield {
      val fetchFromCacheMapBiksValue = List.empty[RegistrationItem]
      val fetchFromCacheMapSelectAllValue = ""
      val initialData = RegistrationList(None, registeredListOption.map { x =>
        RegistrationItem(x.iabdType, false, true)})
      val sortedData = BikListUtils.sortRegistrationsAlphabeticallyByLabels(initialData)
      if (sortedData.active.size == 0) {
        Ok(views.html.errorPage(NO_MORE_BENEFITS_TO_REMOVE_CY1, taxYearRange, FormMappingsConstants.CYP1, -1,
          "Registered benefits for tax year starting 6 April " + YEAR_RANGE.cy, "manage-registrations"))
      }
      else {
        Ok(views.html.registration.nextTaxYear(objSelectedForm.fill(sortedData),
          false, taxYearRange, fetchFromCacheMapBiksValue, List.empty[Bik], List.empty[Int], List.empty[Int], None))
      }
    }
    responseErrorHandler(loadResultFuture)
  }

  def confirmAddCurrentTaxYear:Action[AnyContent] = AuthorisedForPbik {
    implicit ac => implicit request =>
      val resultFuture = for {
        biksListOption:List[Bik] <- bikListService.registeredBenefitsList(YEAR_RANGE.cyminus1, "")(getBenefitTypesPath)
        result <- generateConfirmationScreenView(YEAR_RANGE.cyminus1, "add", views.html.registration.
          confirmAddCurrentTaxYear(_, YEAR_RANGE), (formWithErrors =>
          Ok(views.html.registration.currentTaxYear(formWithErrors, YEAR_RANGE, biksAvailableCount=Some(biksListOption.size)))
            .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))))
      } yield {
        result
      }

      responseCheckCYEnabled(resultFuture)
  }


  def confirmAddNextTaxYear:Action[AnyContent] = AuthorisedForPbik {
    implicit ac => implicit request =>
      val resultFuture = for {
        biksListOption:List[Bik] <- bikListService.registeredBenefitsList(YEAR_RANGE.cy, "")(getBenefitTypesPath)
        result <- generateConfirmationScreenView(YEAR_RANGE.cy, "add", views.html.registration.
          confirmUpdateNextTaxYear(_, true, YEAR_RANGE), (formWithErrors =>
          Ok(views.html.registration.nextTaxYear(formWithErrors, true, YEAR_RANGE, biksAvailableCount=Some(biksListOption.size)))))
      } yield {
        result
      }
      responseErrorHandler(resultFuture)
  }

  def confirmRemoveNextTaxYear:Action[AnyContent] = AuthorisedForPbik {
    implicit ac => implicit request =>
      val resultFuture = for {
        result <- generateConfirmationScreenView(YEAR_RANGE.cy, "remove", views.html.registration.
          confirmUpdateNextTaxYear(_, false, YEAR_RANGE), (formWithErrors =>
          Ok(views.html.registration.nextTaxYear(formWithErrors, false, YEAR_RANGE, biksAvailableCount=None))))
      } yield {
          result
        }
      responseErrorHandler(resultFuture)
  }

  def confirmRemoveNextTaxYearNoForm(iabdType: String):Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
      val registrationList = RegistrationList(None, List(RegistrationItem(iabdType, true, false)), None)
      val form: Form[RegistrationList] = objSelectedForm.fill(registrationList)
      val resultFuture = Future.successful(
        Ok(views.html.registration.confirmUpdateNextTaxYear(objSelectedForm.fill(form.get), false, YEAR_RANGE)))
      responseErrorHandler(resultFuture)
  }


  def removeNextYearRegisteredBenefitTypes:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val persistentBiks: List[Bik] = generateListOfBiksBasedOnForm(BIK_REMOVE_STATUS)
        val registeredFuture = updateBiksFutureAction(YEAR_RANGE.cy, persistentBiks, false)
        responseErrorHandler(registeredFuture)
  }

  def generateConfirmationScreenView(year: Int, cachingSuffix: String,
                                     generateViewBasedOnFormItems: Form[RegistrationList] =>
                                       HtmlFormat.Appendable,viewToRedirect: Form[RegistrationList] =>
    Result)(implicit hc:HeaderCarrier,
            request: Request[AnyContent], ac: AuthContext): Future[Result] = {

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

  def updateRegisteredBenefitTypes:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val resultFuture = {

          val persistentBiks: List[Bik] = generateListOfBiksBasedOnForm(BIK_ADD_STATUS)
          updateBiksFutureAction(YEAR_RANGE.cyminus1, persistentBiks, true)
        }
        responseCheckCYEnabled(resultFuture)
  }

  def addNextYearRegisteredBenefitTypes:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val persistentBiks: List[Bik] = generateListOfBiksBasedOnForm(BIK_ADD_STATUS)
        val actionFuture = updateBiksFutureAction(YEAR_RANGE.cy, persistentBiks, true)
        responseErrorHandler(actionFuture)
  }

  def updateBiksFutureAction(year: Int, persistentBiks: List[Bik], additive: Boolean)
                            (implicit request: Request[AnyContent], ac: AuthContext): Future[Result] = {
    tierConnector.genericGetCall[List[Bik]](baseUrl, getRegisteredPath,
      ac.principal.accounts.epaye.get.empRef.toString, year).flatMap {
      registeredResponse =>
        val form = objSelectedForm.bindFromRequest()

        form.fold(
          formWithErrors => Future.successful(
            Ok(views.html.registration.confirmUpdateNextTaxYear(formWithErrors, additive, YEAR_RANGE)))
          ,
          values => {
            val changes = BikListUtils.normaliseSelectedBenefits(registeredResponse, persistentBiks)
            additive match {
              case true => {
                // Process registration
                val saveFuture = tierConnector.genericPostCall(baseUrl, updateBenefitTypesPath,
                  ac.principal.accounts.epaye.get.empRef.toString, year, changes)
                saveFuture.map {
                  saveResponse: HttpResponse =>
                      auditBikUpdate(true, year, persistentBiks)
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
                                           (implicit request: Request[AnyContent], ac: AuthContext): Result = {
    registrationList.reason match {
      case Some(reasonValue) if(BIK_REMOVE_REASON_LIST.contains(reasonValue.selectionValue)) => {
        reasonValue.info match {
          case _ if(reasonValue.selectionValue.equals("other") && reasonValue.info.getOrElse("").trim.isEmpty) => {
            Redirect(routes.ManageRegistrationController.confirmRemoveNextTaxYearNoForm(iabdValueURLMapper(persistentBiks.head.iabdType)
            )).flashing("error" -> Messages("RemoveBenefits.reason.other.required"))
          }
          case Some(info)=> {
            tierConnector.genericPostCall(baseUrl, updateBenefitTypesPath,
              ac.principal.accounts.epaye.get.empRef.toString, year, changes)
            auditBikUpdate(false, year, persistentBiks, Some((reasonValue.selectionValue.toUpperCase, Some(info))))
            loadWhatNextRemovedBIK(form, year)
          }
          case _ => {
            tierConnector.genericPostCall(baseUrl, updateBenefitTypesPath,
              ac.principal.accounts.epaye.get.empRef.toString, year, changes)
            auditBikUpdate(false, year, persistentBiks, Some((reasonValue.selectionValue.toUpperCase, None)))
            loadWhatNextRemovedBIK(form, year)
          }
        }
      }
      case _ => Redirect(routes.ManageRegistrationController.confirmRemoveNextTaxYearNoForm(iabdValueURLMapper(persistentBiks.head.iabdType)
        )).flashing("error" -> Messages("RemoveBenefits.reason.no.selection"))
    }
  }

  private def auditBikUpdate(additive: Boolean, year:Int, persistentBiks: List[Bik], removeReason: Option[(String, Option[String])] = None)
                            (implicit hc:HeaderCarrier, ac: AuthContext) = {
    val derivedMsg = if(additive) "Benefit added to " + taxYearToSpPeriod(year) else "Benefit removed from " + taxYearToSpPeriod(year)
    for (bik <- persistentBiks ) {
      logSplunkEvent(createDataEvent(
        tier=spTier.FRONTEND,
        action= if(additive) spAction.ADD else spAction.REMOVE,
        target=spTarget.BIK,
        period=taxYearToSpPeriod(year),
        msg= derivedMsg + " : " + bik.iabdType,
        nino=None,
        iabd=Some(bik.iabdType),
        removeReason=if(additive) None else Some(removeReason.get._1),
        removeReasonDesc=if(additive) None else Some(removeReason.get._2.getOrElse("")))
      )
    }
  }
}
