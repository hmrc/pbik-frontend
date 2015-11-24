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

package controllers

import java.util.UUID

import config.PbikAppConfig
import controllers.auth.{AuthenticationConnector, EpayeUser, PbikActions}
import models._
import connectors.{HmrcTierConnector, TierConnector}
import play.i18n.Messages
import play.api.data.{FormError, Form}
import play.api.{Play, Logger}
import play.api.mvc._
import services.{BikListService, EiLListService}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.{HttpResponse, SessionKeys}
import utils.BikListUtils.MandatoryRadioButton
import utils.Exceptions.{InvalidBikTypeURIException, InvalidYearURIException}
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.AuthContext

trait ExclusionListConfiguration extends RunMode {

  import play.api.Play.current

  lazy val exclusionsAllowed = Play.configuration.getBoolean("eil.functionality.enabled").getOrElse(false)

}

object ExclusionListController extends ExclusionListController with TierConnector
                                with AuthenticationConnector {
  def pbikAppConfig = PbikAppConfig
  def eiLListService = EiLListService
  def bikListService = BikListService
  val tierConnector = new HmrcTierConnector
}

trait ExclusionListController extends FrontendController with URIInformation
                              with ControllersReferenceData with PbikActions with EpayeUser
                              with SplunkLogger with ExclusionListConfiguration {
  this: TierConnector =>

  def eiLListService: EiLListService
  def bikListService: BikListService

  def mapYearStringToInt(URIYearString: String): Future[Int] = {
    URIYearString match {
      case utils.FormMappingsConstants.CY => Future{YEAR_RANGE.cyminus1}
      case utils.FormMappingsConstants.CYP1 => Future{YEAR_RANGE.cy}
      case (_) => Future{throw new InvalidYearURIException()}
    }
  }

  def validateRequest(isCurrentYear: String, iabdType: String)(implicit ac: AuthContext, request: Request[_]):Future[Int] = {
   for {
      year <- mapYearStringToInt(isCurrentYear)
      registeredBenefits: List[Bik] <- bikListService.registeredBenefitsList(year, ac.principal.accounts.epaye.get.empRef.toString)(getRegisteredPath)
    } yield {
      if (registeredBenefits.filter(x => x.iabdType.equals(iabdValueURLDeMapper(iabdType))).length > 0) {
        year
      } else {
        throw new InvalidBikTypeURIException()
      }
    }
  }

  def performPageLoad(isCurrentTaxYear: String, iabdType: String):Action[AnyContent] = AuthorisedForPbik {
    implicit user =>
      implicit request =>
        if ( exclusionsAllowed) {
            val iabdTypeValue = iabdValueURLDeMapper(iabdType)
            val staticDataRequest = for {
              year <- validateRequest(isCurrentTaxYear, iabdType)
              nextYearList: (Map[String, String], List[Bik]) <- bikListService.nextYearList
              currentYearEIL: List[EiLPerson] <- eiLListService.currentYearEiL(iabdTypeValue, year)
            } yield {
                  Ok(views.html.exclusion.exclusionOverview(YEAR_RANGE, isCurrentTaxYear, iabdTypeValue, currentYearEIL))
                    .addingToSession(bikListService.pbikHeaders.toSeq: _*)
            }
            responseErrorHandler(staticDataRequest)

        } else {
          Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED,
                                                    TaxDateUtils.getTaxYearRange())))
        }

  }

  def withOrWithoutNinoOnPageLoad(isCurrentTaxYear: String, iabdType: String):Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val iabdTypeValue = iabdValueURLDeMapper(iabdType)
        if ( exclusionsAllowed ) {
          val resultFuture = for {
            _ <- validateRequest(isCurrentTaxYear, iabdType)
          } yield {
              Ok(views.html.exclusion.exclusionNinoOrNoNinoForm(YEAR_RANGE, isCurrentTaxYear, iabdTypeValue))
          }
          responseErrorHandler(resultFuture)
        } else {
          Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED,
                                                    TaxDateUtils.getTaxYearRange())))
        }
  }

  def withOrWithoutNinoDecision(isCurrentTaxYear: String, iabdType: String):Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
      val iabdTypeValue = iabdValueURLDeMapper(iabdType)
      if ( exclusionsAllowed ) {
        val taxYearRange = YEAR_RANGE
        val resultFuture = binaryRadioButton.bindFromRequest().fold(
          formWithErrors => Future(Redirect(routes.ExclusionListController.withOrWithoutNinoOnPageLoad(
            isCurrentTaxYear, iabdType)).flashing("error" -> Messages.get("ExclusionDecision.noselection.error")))
          ,
          values => {
            val selectedValue = values.selectionValue
            for {
              _ <- validateRequest(isCurrentTaxYear, iabdType)
            } yield {
              selectedValue match {
                case FORM_TYPE_NINO => Ok(views.html.exclusion.ninoExclusionSearchForm(taxYearRange,
                    isCurrentTaxYear, iabdTypeValue, exclusionSearchFormWithNino))
                  case FORM_TYPE_NONINO => Ok(views.html.exclusion.noNinoExclusionSearchForm(taxYearRange,
                    isCurrentTaxYear, iabdTypeValue, exclusionSearchFormWithoutNino))
                case "" => Redirect(routes.ExclusionListController.withOrWithoutNinoOnPageLoad(
                  isCurrentTaxYear, iabdTypeValue)).flashing("error" -> Messages.get("ExclusionDecision.noselection.error"))
              }
            }
          }
        )
        responseErrorHandler(resultFuture)
      } else {
        Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED, TaxDateUtils.getTaxYearRange())))
      }
  }

  def searchResults(isCurrentTaxYear: String, iabdType: String, formType: String):Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val iabdTypeValue = iabdValueURLDeMapper(iabdType)
        if ( exclusionsAllowed ) {
          val form = formType match {
            case FORM_TYPE_NINO => exclusionSearchFormWithNino
            case FORM_TYPE_NONINO => exclusionSearchFormWithoutNino
          }
          val futureResult = form.bindFromRequest().fold(
            formWithErrors =>
              searchResultsHandleFormErrors(isCurrentTaxYear, formType, iabdTypeValue, formWithErrors),
            validModel => {
              for {
                year <- validateRequest(isCurrentTaxYear, iabdType)
                result <- tierConnector.genericPostCall(baseUrl, exclusionPostUpdatePath(iabdTypeValue),
                  ac.principal.accounts.epaye.get.empRef.toString, year, validModel)
              } yield {
                val listOfMatches: List[EiLPerson] = result.json.validate[List[EiLPerson]].asOpt.get
                searchResultsHandleValidResult(listOfMatches, isCurrentTaxYear, formType,
                  iabdTypeValue, form, None)
              }
            }
         )
         responseErrorHandler(futureResult)
        } else {
          Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED, TaxDateUtils.getTaxYearRange())))
        }
  }

  /*
  * Handles valid List[EiLPerson] on search results page
  * If list is 0 size will return employee not found message
  */
  def searchResultsHandleValidResult(listOfMatches: List[EiLPerson], isCurrentTaxYear: String, formType: String,
                                     iabdTypeValue: String, form: Form[EiLPerson], individualSelectionOption: Option[String])
                                    (implicit request: Request[_], ac: AuthContext): Result = {
    listOfMatches.size match {
      case 0 =>
        Logger.error("Matches are zero size")
        formType match {
          case FORM_TYPE_NINO => Ok(views.html.exclusion.ninoExclusionSearchForm(YEAR_RANGE, isCurrentTaxYear,
            iabdTypeValue, form.bindFromRequest().withError("status", Messages.get("ExclusionSearch.Fail.P"))))
          case _ => Ok(views.html.exclusion.noNinoExclusionSearchForm(YEAR_RANGE, isCurrentTaxYear,
            iabdTypeValue, form.bindFromRequest().withError("status", Messages.get("ExclusionSearch.Fail.P"))))
        }

      case _ =>
        val filledListOfMatchesForm = individualsFormWithRadio.fill((individualSelectionOption.getOrElse(""),
          EiLPersonList(listOfMatches)))
        Ok(views.html.exclusion.searchResults(YEAR_RANGE, isCurrentTaxYear, iabdTypeValue,
          filledListOfMatchesForm, formType))

    }
  }

  /*
  * Handles form errors on search results page
  * Will display relevant form page
  */
  def searchResultsHandleFormErrors(isCurrentTaxYear: String, formType: String, iabdTypeValue: String,
                                    formWithErrors: Form[EiLPerson])
                                   (implicit request: Request[_], ac: AuthContext): Future[Result] = {
    Future {
      formType match {
        case FORM_TYPE_NINO => Ok(views.html.exclusion.ninoExclusionSearchForm(YEAR_RANGE, isCurrentTaxYear,
          iabdTypeValue, formWithErrors))
        case FORM_TYPE_NONINO => Ok(views.html.exclusion.noNinoExclusionSearchForm(YEAR_RANGE, isCurrentTaxYear,
          iabdTypeValue, formWithErrors))
      }
    }
  }

  def showExclusionWhatNext(year: String, iabdType: String, name:String) = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val iabdTypeValue = iabdValueURLDeMapper(iabdType)
        Future.successful(Ok(views.html.exclusion.whatNextExclusion(
            TaxDateUtils.getTaxYearRange(), year, iabdTypeValue, name))
            .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
            .addingToSession(bikListService.pbikHeaders.toSeq: _*))
  }

  def showRescindWhatNext(year: String, iabdType: String, name:String) = AuthorisedForPbik {
    implicit user =>
      implicit request =>
        Future.successful(Ok(views.html.exclusion.whatNextRescind(
            TaxDateUtils.getTaxYearRange(), year, iabdType, name))
            .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
            .addingToSession(bikListService.pbikHeaders.toSeq: _*))
  }

  def updateExclusions(year: String, iabdType: String):Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val iabdTypeValue = iabdValueURLDeMapper(iabdType)
        if ( exclusionsAllowed ) {
            val taxYearRange = YEAR_RANGE

            val futureResult = processExclusionForm(individualsForm.bindFromRequest, year, iabdTypeValue, taxYearRange)
            responseErrorHandler(futureResult)

        } else {
          Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED, TaxDateUtils.getTaxYearRange())))
        }
  }

  def updateMultipleExclusions(year: String, iabdType: String):Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val iabdTypeValue = iabdValueURLDeMapper(iabdType)
        if ( exclusionsAllowed ) {
          val taxYearRange = YEAR_RANGE
          val form = individualsFormWithRadio.bindFromRequest
          val futureResult = processIndividualExclusionForm(form, year, iabdTypeValue, taxYearRange)
          responseErrorHandler(futureResult)

        } else {
          Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED, TaxDateUtils.getTaxYearRange())))
        }
  }

  def processIndividualExclusionForm (form: Form[(String,EiLPersonList)], isCurrentTaxYear: String,
                                      iabdType: String, taxYearRange: TaxYearRange)
                                     (implicit hc:HeaderCarrier, request: Request[AnyContent], ac: AuthContext) : Future[Result] = {
    val iabdTypeValue = iabdValueURLMapper(iabdType)
    form.fold(
      formWithErrors =>
        for {
          _ <- validateRequest(isCurrentTaxYear, iabdTypeValue)
        } yield {
          Ok(views.html.exclusion.searchResults(taxYearRange, isCurrentTaxYear, iabdType,
            formWithErrors, FORM_TYPE_NONINO))
        },
      values => {

          validateRequest(isCurrentTaxYear, iabdTypeValue)
          val excludedIndividual = extractExcludedIndividual(values._1, values._2)
          commitExclusion(isCurrentTaxYear, iabdType, taxYearRange, excludedIndividual)
      }
    )
  }

  def processExclusionForm(form: Form[(EiLPersonList)], year: String,
                           iabdType: String, taxYearRange: TaxYearRange)
                          (implicit hc:HeaderCarrier, request: Request[AnyContent], ac: AuthContext) : Future[Result] = {

    form.fold(
      formWithErrors => Future {
        Ok(views.html.errorPage(INVALID_FORM_ERROR, taxYearRange, year))
      },
      values => {

        val excludedIndividual = extractExcludedIndividual("",values)
        if ( excludedIndividual.isEmpty ) {
          Future.successful(Ok(views.html.errorPage(INVALID_FORM_ERROR, taxYearRange, year)))
        } else {
          commitExclusion(year, iabdType, taxYearRange,excludedIndividual)
        }
      }
    )
  }

  def commitExclusion(year:String, iabdType:String, taxYearRange:TaxYearRange, excludedIndividual:Option[EiLPerson])
                     (implicit hc:HeaderCarrier, request: Request[AnyContent], ac: AuthContext) : Future[Result] = {
    val yearInt = if (year.equals(utils.FormMappingsConstants.CY)) taxYearRange.cyminus1 else taxYearRange.cy
    val spYear = if (TaxDateUtils.isCurrentTaxYear(yearInt)) spPeriod.CY else spPeriod.CYP1
    val registrationList: RegistrationList = new RegistrationList(None, List(new RegistrationItem(iabdType, false, false)))
    tierConnector.genericPostCall(baseUrl, exclusionPostUpdatePath(iabdType),
      ac.principal.accounts.epaye.get.empRef.toString, yearInt, excludedIndividual.get).map {
      response =>
        response.status match {
          case OK => {
            auditExclusion(true, yearInt,excludedIndividual.get.nino, iabdType)
            Ok(views.html.exclusion.whatNextExclusion.render(TaxDateUtils.getTaxYearRange(), year,
              iabdType, "Exclusion commited", request, ac))

          }
          case _ => Ok(views.html.errorPage("Could not perform update operation", YEAR_RANGE, "")).
            withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
        }
    }
  }

  /*
  * Extracts the matched individual from an empty list ( error ), single item list or matching a nino from a
  * radio button form.
  */
  def extractExcludedIndividual(chosenNino: String, individuals: EiLPersonList): Option[EiLPerson] = {
    individuals.active.size match {
      case 0 => None
      case 1 => Some(individuals.active.head)
      case _ => {
        chosenNino.trim.size match {
          case 0 => Some(individuals.active.head)
          case _ => individuals.active.filter(x => x.nino == chosenNino).headOption
        }

      }
    }
  }

  def remove(year:String, iabdType: String):Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        if ( exclusionsAllowed ) {

          processRemoval(individualsForm.bindFromRequest, year, iabdType, YEAR_RANGE)
        } else {
          Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED, TaxDateUtils.getTaxYearRange())))
        }
  }

  def processRemoval(form: Form[(EiLPersonList)], year:String,
                           iabdType: String, taxYearRange: TaxYearRange)
                          (implicit hc:HeaderCarrier, request: Request[AnyContent], ac: AuthContext) : Future[Result] = {
    form.fold(
      formWithErrors => Future.successful(Ok(views.html.errorPage(INVALID_FORM_ERROR, taxYearRange, ""))),
      values => Future.successful(Ok(views.html.exclusion.removalConfirmation(taxYearRange, year, iabdType,
        individualsForm.fill(values))))
    )

  }

  def removeExclusionsCommit(iabdType: String):Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        val iabdTypeValue = iabdValueURLDeMapper(iabdType)
        val taxYearRange = TaxDateUtils.getTaxYearRange()
        if ( exclusionsAllowed) {
          processRemovalCommit(individualsForm.bindFromRequest, iabdTypeValue, taxYearRange)
        } else {
          Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED,taxYearRange )))
        }
  }

  def processRemovalCommit(form: Form[(EiLPersonList)],
                     iabdType: String, taxYearRange:TaxYearRange)
                    (implicit hc:HeaderCarrier, request: Request[AnyContent], ac: AuthContext) : Future[Result] = {
    val year = taxYearRange.cy
    val removalsList = form.fold(
      formWithErrors => List.empty[EiLPerson],
      values => {
        values.active
      }
    )
    val registrationList: RegistrationList = new RegistrationList(None, List(new RegistrationItem(iabdType, false, false)))

    val individual = removalsList.head
    val iabdTypeValue = iabdValueURLMapper(iabdType)
    val futureExclude = tierConnector.genericPostCall(baseUrl, exclusionPostRemovePath(iabdType),
      ac.principal.accounts.epaye.get.empRef.toString, year, individual ).map {
      response =>
        response.status match {

          case OK => {
                auditExclusion(false, year,extractListNino(removalsList),iabdType)
                Ok(views.html.exclusion.whatNextRescind.render(TaxDateUtils.getTaxYearRange(), NEXT_TAX_YEAR,
                iabdTypeValue, "Exclusion rescinded", request, ac)).
                withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
          }
          case _ => Ok(views.html.errorPage("Could not perform update operation", YEAR_RANGE, "")).
            withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
        }
    }
    responseErrorHandler(futureExclude)
  }


  private def auditExclusion(exclusion:Boolean, year:Int, employee:String, iabdType:String )
                    (implicit hc:HeaderCarrier, ac: AuthContext) = {
    logSplunkEvent(createDataEvent(
      tier=spTier.FRONTEND,
      action= if (exclusion) spAction.ADD else spAction.REMOVE,
      target=spTarget.EIL,
      period= taxYearToSpPeriod(year),
      msg= if (exclusion) "Employee excluded" else "Employee exclusion rescinded",
      nino=Some(employee),
      iabd=Some(iabdType)))
  }

}
