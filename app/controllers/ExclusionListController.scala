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

package controllers

import java.util.UUID

import config._
import connectors.HmrcTierConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import javax.inject.Inject
import models._
import play.api.Mode.Mode
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import play.api.{Configuration, Environment, Logger, Play}
import services.{BikListService, EiLListService}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.Exceptions.{InvalidBikTypeURIException, InvalidYearURIException}
import utils.{ControllersReferenceData, SplunkLogger, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class ExclusionListController @Inject()(
                                        val authenticate: AuthAction,
                                        val noSessionCheck: NoSessionCheckAction,
                                        val eiLListService: EiLListService,
                                        val bikListService: BikListService,
                                        val tierConnector: HmrcTierConnector, //TODO: Why do we need this?,
                                        val runModeConfiguration: Configuration,
                                        environment:Environment,
                                        taxDateUtils: TaxDateUtils,
                                        splunkLogger: SplunkLogger,
                                        controllersReferenceData: ControllersReferenceData)(
                                        implicit val pbikAppConfig: PbikAppConfig,
                                        implicit val context: PbikContext,
                                        implicit val uRIInformation: URIInformation,
                                        implicit val externalURLs: ExternalUrls,
                                        implicit val localFormPartialRetriever: LocalFormPartialRetriever
                                       ) extends FrontendController {
  val mode: Mode = environment.mode

  lazy val exclusionsAllowed: Boolean = Play.configuration.getBoolean("pbik.enabled.eil").getOrElse(false)

  def mapYearStringToInt(URIYearString: String): Future[Int] = {
    URIYearString match {
      case utils.FormMappingsConstants.CY => Future.successful(controllersReferenceData.YEAR_RANGE.cyminus1)
      case utils.FormMappingsConstants.CYP1 => Future.successful(controllersReferenceData.YEAR_RANGE.cy)
      case _ => Future.failed(throw new InvalidYearURIException())
    }
  }

  def validateRequest(isCurrentYear: String, iabdType: String)(implicit request: AuthenticatedRequest[_]): Future[Int] = {
    for {
      year <- mapYearStringToInt(isCurrentYear)
      registeredBenefits: List[Bik] <- bikListService.registeredBenefitsList(year, request.empRef)(uRIInformation.getRegisteredPath)
    } yield {
      if (registeredBenefits.exists(_.iabdType.equals(uRIInformation.iabdValueURLDeMapper(iabdType)))) {
        year
      } else {
        throw new InvalidBikTypeURIException()
      }
    }
  }

  def performPageLoad(isCurrentTaxYear: String, iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      if (exclusionsAllowed) {
        val iabdTypeValue = uRIInformation.iabdValueURLDeMapper(iabdType)
        val staticDataRequest = for {
          year <- validateRequest(isCurrentTaxYear, iabdType)
          currentYearList: (Map[String, String], List[Bik]) <- bikListService.currentYearList
          nextYearList: (Map[String, String], List[Bik]) <- bikListService.nextYearList
          currentYearEIL: List[EiLPerson] <- eiLListService.currentYearEiL(iabdTypeValue, year)
        } yield {
          Ok(views.html.exclusion.exclusionOverview(controllersReferenceData.YEAR_RANGE,
            isCurrentTaxYear,
            iabdTypeValue,
            currentYearEIL.sortWith(_.surname < _.surname),
            request.empRef)
          ).removingFromSession(HeaderTags.ETAG)
            .addingToSession(nextYearList._1.toSeq: _*)
        }
        controllersReferenceData.responseErrorHandler(staticDataRequest)

      } else {
        Future.successful(Ok(views.html.errorPage(ControllersReferenceDataCodes.FEATURE_RESTRICTED,
          taxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }

  }

  def withOrWithoutNinoOnPageLoad(isCurrentTaxYear: String, iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val iabdTypeValue = uRIInformation.iabdValueURLDeMapper(iabdType)
      if (exclusionsAllowed) {
        val resultFuture = for {
          _ <- validateRequest(isCurrentTaxYear, iabdType)
        } yield {
          Ok(views.html.exclusion.exclusionNinoOrNoNinoForm(controllersReferenceData.YEAR_RANGE, isCurrentTaxYear, iabdTypeValue, empRef = request.empRef))
        }
        controllersReferenceData.responseErrorHandler(resultFuture)
      } else {
        Future.successful(Ok(views.html.errorPage(ControllersReferenceDataCodes.FEATURE_RESTRICTED,
          taxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }
  }

  def withOrWithoutNinoDecision(isCurrentTaxYear: String, iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val iabdTypeValue = uRIInformation.iabdValueURLDeMapper(iabdType)
      if (exclusionsAllowed) {
        val taxYearRange = controllersReferenceData.YEAR_RANGE
        val resultFuture = uRIInformation.binaryRadioButton.bindFromRequest().fold(
          formWithErrors => Future(Redirect(routes.ExclusionListController.withOrWithoutNinoOnPageLoad(
            isCurrentTaxYear, iabdType)).flashing("error" -> Messages("ExclusionDecision.noselection.error")))
          ,
          values => {
            val selectedValue = values.selectionValue
            for {
              _ <- validateRequest(isCurrentTaxYear, iabdType)
            } yield {
              selectedValue match {
                case ControllersReferenceDataCodes.FORM_TYPE_NINO => Ok(views.html.exclusion.ninoExclusionSearchForm(taxYearRange,
                  isCurrentTaxYear, iabdTypeValue, uRIInformation.exclusionSearchFormWithNino, empRef = request.empRef))
                case ControllersReferenceDataCodes.FORM_TYPE_NONINO => Ok(views.html.exclusion.noNinoExclusionSearchForm(taxYearRange,
                  isCurrentTaxYear, iabdTypeValue, uRIInformation.exclusionSearchFormWithoutNino, empRef = request.empRef))
                case "" => Redirect(routes.ExclusionListController.withOrWithoutNinoOnPageLoad(
                  isCurrentTaxYear, iabdTypeValue)).flashing("error" -> Messages("ExclusionDecision.noselection.error"))
              }
            }
          }
        )
        controllersReferenceData.responseErrorHandler(resultFuture)
      } else {
        Future.successful(Ok(views.html.errorPage(ControllersReferenceDataCodes.FEATURE_RESTRICTED, taxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }
  }

  def searchResults(isCurrentTaxYear: String, iabdType: String, formType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      val iabdTypeValue = uRIInformation.iabdValueURLDeMapper(iabdType)
      if (exclusionsAllowed) {
        val form = formType match {
          case ControllersReferenceDataCodes.FORM_TYPE_NINO => uRIInformation.exclusionSearchFormWithNino
          case ControllersReferenceDataCodes.FORM_TYPE_NONINO => uRIInformation.exclusionSearchFormWithoutNino
        }
        val futureResult = form.bindFromRequest().fold(
          formWithErrors =>
            searchResultsHandleFormErrors(isCurrentTaxYear, formType, iabdTypeValue, formWithErrors),
          validModel => {
            for {
              year <- validateRequest(isCurrentTaxYear, iabdType)
              result <- tierConnector.genericPostCall(uRIInformation.baseUrl, uRIInformation.exclusionPostUpdatePath(iabdTypeValue),
                request.empRef, year, validModel)
              resultAlreadyExcluded: List[EiLPerson] <- eiLListService.currentYearEiL(iabdTypeValue, year)

            } yield {
              val listOfMatches: List[EiLPerson] = eiLListService.searchResultsRemoveAlreadyExcluded(resultAlreadyExcluded,
                result.json.validate[List[EiLPerson]].asOpt.get)
              searchResultsHandleValidResult(listOfMatches, resultAlreadyExcluded, isCurrentTaxYear, formType,
                iabdTypeValue, form, None)
            }
          }
        )
        controllersReferenceData.responseErrorHandler(futureResult)
      } else {
        Future.successful(Ok(views.html.errorPage(ControllersReferenceDataCodes.FEATURE_RESTRICTED, taxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }
  }

  /*
  * Handles valid List[EiLPerson] on search results page
  * If list is 0 size will return employee not found message
  */
  def searchResultsHandleValidResult(listOfMatches: List[EiLPerson], resultAlreadyExcluded: List[EiLPerson], isCurrentTaxYear: String, formType: String,
                                     iabdTypeValue: String, form: Form[EiLPerson], individualSelectionOption: Option[String])
                                    (implicit request: AuthenticatedRequest[_]): Result = {
    listOfMatches.size match {
      case 0 =>
        Logger.error("Matches are zero size")
        val existsAlready = resultAlreadyExcluded.contains(form.bindFromRequest().get)
        val message = if (existsAlready) Messages("ExclusionSearch.Fail.Exists.P") else Messages("ExclusionSearch.Fail.P")

        formType match {
          case ControllersReferenceDataCodes.FORM_TYPE_NINO => Ok(views.html.exclusion.ninoExclusionSearchForm(controllersReferenceData.YEAR_RANGE, isCurrentTaxYear,
            iabdTypeValue, form.bindFromRequest().withError("status", message), existsAlready, empRef = request.empRef))
          case _ => Ok(views.html.exclusion.noNinoExclusionSearchForm(controllersReferenceData.YEAR_RANGE, isCurrentTaxYear,
            iabdTypeValue, form.bindFromRequest().withError("status", message), existsAlready, empRef = request.empRef))
        }

      case _ =>
        Logger.info("Exclusion search matched " + listOfMatches.size + " employees with Optimistic locks " + listOfMatches.map(x => x.perOptLock))
        val filledListOfMatchesForm = uRIInformation.individualsFormWithRadio.fill((individualSelectionOption.getOrElse(""),
          EiLPersonList(listOfMatches)))
        Ok(views.html.exclusion.searchResults(controllersReferenceData.YEAR_RANGE, isCurrentTaxYear, iabdTypeValue,
          filledListOfMatchesForm, formType, empRef = request.empRef))

    }
  }

  /*
  * Handles form errors on search results page
  * Will display relevant form page
  */
  def searchResultsHandleFormErrors(isCurrentTaxYear: String, formType: String, iabdTypeValue: String,
                                    formWithErrors: Form[EiLPerson])
                                   (implicit request: AuthenticatedRequest[_]): Future[Result] = {
    Future {
      formType match {
        case ControllersReferenceDataCodes.FORM_TYPE_NINO => Ok(views.html.exclusion.ninoExclusionSearchForm(controllersReferenceData.YEAR_RANGE, isCurrentTaxYear,
          iabdTypeValue, formWithErrors, empRef = request.empRef))
        case ControllersReferenceDataCodes.FORM_TYPE_NONINO => Ok(views.html.exclusion.noNinoExclusionSearchForm(controllersReferenceData.YEAR_RANGE, isCurrentTaxYear,
          iabdTypeValue, formWithErrors, empRef = request.empRef))
      }
    }
  }

  def updateExclusions(year: String, iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      val iabdTypeValue = uRIInformation.iabdValueURLDeMapper(iabdType)
      if (exclusionsAllowed) {
        val taxYearRange = controllersReferenceData.YEAR_RANGE

        val futureResult = processExclusionForm(uRIInformation.individualsForm.bindFromRequest, year, iabdTypeValue, taxYearRange)
        controllersReferenceData.responseErrorHandler(futureResult)

      } else {
        Future.successful(Ok(views.html.errorPage(ControllersReferenceDataCodes.FEATURE_RESTRICTED, taxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }
  }

  def updateMultipleExclusions(year: String, iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      val iabdTypeValue = uRIInformation.iabdValueURLDeMapper(iabdType)
      if (exclusionsAllowed) {
        val taxYearRange = controllersReferenceData.YEAR_RANGE
        val form = uRIInformation.individualsFormWithRadio.bindFromRequest
        val futureResult = processIndividualExclusionForm(form, year, iabdTypeValue, taxYearRange)
        controllersReferenceData.responseErrorHandler(futureResult)
      } else {
        Future.successful(Ok(views.html.errorPage(ControllersReferenceDataCodes.FEATURE_RESTRICTED, taxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }
  }

  def processIndividualExclusionForm(form: Form[(String, EiLPersonList)], isCurrentTaxYear: String,
                                     iabdType: String, taxYearRange: TaxYearRange)
                                    (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val iabdTypeValue = uRIInformation.iabdValueURLMapper(iabdType)
    form.fold(
      formWithErrors =>
        for {
          _ <- validateRequest(isCurrentTaxYear, iabdTypeValue)
        } yield {
          Ok(views.html.exclusion.searchResults(taxYearRange, isCurrentTaxYear, iabdType,
            formWithErrors, ControllersReferenceDataCodes.FORM_TYPE_NONINO, empRef = request.empRef))
        },
      values => {

        validateRequest(isCurrentTaxYear, iabdTypeValue)
        val excludedIndividual = extractExcludedIndividual(values._1, values._2)
        commitExclusion(isCurrentTaxYear, iabdType, taxYearRange, excludedIndividual)
      }
    )
  }

  def processExclusionForm(form: Form[EiLPersonList], year: String,
                           iabdType: String, taxYearRange: TaxYearRange)
                          (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    form.fold(
      formWithErrors => Future {
        Ok(views.html.errorPage(ControllersReferenceDataCodes.INVALID_FORM_ERROR, taxYearRange, year, empRef = Some(request.empRef)))
      },
      values => {
        val excludedIndividual = extractExcludedIndividual("", values)
        if (excludedIndividual.isEmpty) {
          Future.successful(Ok(views.html.errorPage(ControllersReferenceDataCodes.INVALID_FORM_ERROR, taxYearRange, year, empRef = Some(request.empRef))))
        } else {
          commitExclusion(year, iabdType, taxYearRange, excludedIndividual)
        }
      }
    )
  }

  def commitExclusion(year: String, iabdType: String, taxYearRange: TaxYearRange, excludedIndividual: Option[EiLPerson])
                     (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent], context: PbikContext): Future[Result] = {
    val yearInt = if (year.equals(utils.FormMappingsConstants.CY)) taxYearRange.cyminus1 else taxYearRange.cy
    val spYear = if (taxDateUtils.isCurrentTaxYear(yearInt)) splunkLogger.spPeriod.CY else splunkLogger.spPeriod.CYP1
    val registrationList: RegistrationList = RegistrationList(None, List(RegistrationItem(iabdType, active = false, enabled = false)))

    Logger.info(s"Committing Exclusion for scheme ${request.empRef.toString} , with employees Optimisitic Lock: ${excludedIndividual.get.perOptLock}")

    tierConnector.genericPostCall(uRIInformation.baseUrl, uRIInformation.exclusionPostUpdatePath(iabdType),
      request.empRef, yearInt, excludedIndividual.get).map {
      response =>
        response.status match {
          case OK => {
            auditExclusion(exclusion = true, yearInt, excludedIndividual.get.nino, iabdType)
            Ok(views.html.exclusion.whatNextExclusion(taxDateUtils.getTaxYearRange(), year,
              iabdType, excludedIndividual.get.firstForename + " " + excludedIndividual.get.surname, request.empRef))
          }
          case _ => Ok(views.html.errorPage("Could not perform update operation",
            controllersReferenceData.YEAR_RANGE,
            isCurrentTaxYear = "",
            empRef = Some(request.empRef))).
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
        chosenNino.trim.length match {
          case 0 => Some(individuals.active.head)
          case _ => individuals.active.find(x => x.nino == chosenNino)
        }
      }
    }
  }

  def remove(year: String, iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      if (exclusionsAllowed) {
        processRemoval(uRIInformation.individualsForm.bindFromRequest, year, iabdType, controllersReferenceData.YEAR_RANGE)
      } else {
        Future.successful(Ok(views.html.errorPage(ControllersReferenceDataCodes.FEATURE_RESTRICTED, taxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }
  }

  def processRemoval(form: Form[EiLPersonList], year: String,
                     iabdType: String, taxYearRange: TaxYearRange)
                    (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    form.fold(
      formWithErrors => Future.successful(Ok(views.html.errorPage(ControllersReferenceDataCodes.INVALID_FORM_ERROR, taxYearRange, "", empRef = Some(request.empRef)))),
      values => Future.successful(Ok(views.html.exclusion.removalConfirmation(taxYearRange, year, iabdType,
        uRIInformation.individualsForm.fill(values), empRef = request.empRef)))
    )

  }

  def removeExclusionsCommit(iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      val iabdTypeValue = uRIInformation.iabdValueURLDeMapper(iabdType)
      val taxYearRange = taxDateUtils.getTaxYearRange()
      if (exclusionsAllowed) {
        processRemovalCommit(uRIInformation.individualsForm.bindFromRequest, iabdTypeValue, taxYearRange)
      } else {
        Future.successful(Ok(views.html.errorPage(ControllersReferenceDataCodes.FEATURE_RESTRICTED, taxYearRange, empRef = Some(request.empRef))))
      }
  }

  def processRemovalCommit(form: Form[EiLPersonList],
                           iabdType: String, taxYearRange: TaxYearRange)
                          (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent], context: PbikContext): Future[Result] = {
    val year = taxYearRange.cy
    val removalsList = form.fold(
      formWithErrors => List.empty[EiLPerson],
      values => {
        values.active
      }
    )
    val registrationList: RegistrationList = RegistrationList(None, List(RegistrationItem(iabdType, active = false, enabled = false)))

    val individual = removalsList.head
    val iabdTypeValue = uRIInformation.iabdValueURLMapper(iabdType)
    val futureExclude = tierConnector.genericPostCall(uRIInformation.baseUrl, uRIInformation.exclusionPostRemovePath(iabdType),
      request.empRef, year, individual).map {
      response =>
        response.status match {

          case OK => {
            auditExclusion(exclusion = false, year, splunkLogger.extractListNino(removalsList), iabdType)
            Ok(views.html.exclusion.whatNextRescind(taxDateUtils.getTaxYearRange(), ControllersReferenceDataCodes.NEXT_TAX_YEAR,
              iabdTypeValue, individual.firstForename + " " + individual.surname, request.empRef))
              .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))

          }
          case _ => Ok(views.html.errorPage("Could not perform update operation", controllersReferenceData.YEAR_RANGE, "", empRef = Some(request.empRef)))
            .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
        }
    }
    controllersReferenceData.responseErrorHandler(futureExclude)
  }

  private def auditExclusion(exclusion: Boolean, year: Int, employee: String, iabdType: String)
                            (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]) = {
    splunkLogger.logSplunkEvent(splunkLogger.createDataEvent(
      tier = splunkLogger.spTier.FRONTEND,
      action = if (exclusion) splunkLogger.spAction.ADD else splunkLogger.spAction.REMOVE,
      target = splunkLogger.spTarget.EIL,
      period = splunkLogger.taxYearToSpPeriod(year),
      msg = if (exclusion) "Employee excluded" else "Employee exclusion rescinded",
      nino = Some(employee),
      iabd = Some(iabdType),
      name = Some(request.name),
      empRef = Some(request.empRef)))
  }

}
