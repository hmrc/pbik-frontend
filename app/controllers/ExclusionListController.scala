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
import connectors.{HmrcTierConnector, TierConnector}
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models._
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import play.api.{Logger, Play}
import services.{BikListService, EiLListService}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.Exceptions.{InvalidBikTypeURIException, InvalidYearURIException}
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ExclusionListConfiguration extends RunMode with RunModeConfig {

  lazy val exclusionsAllowed: Boolean = Play.configuration.getBoolean("pbik.enabled.eil").getOrElse(false)

}

object ExclusionListController extends ExclusionListController with TierConnector {
  val pbikAppConfig: AppConfig = PbikAppConfig

  def eiLListService: EiLListService.type = EiLListService

  def bikListService: BikListService.type = BikListService

  val tierConnector = new HmrcTierConnector
  val authenticate: AuthAction = Play.current.injector.instanceOf[AuthAction]
  val noSessionCheck: NoSessionCheckAction = Play.current.injector.instanceOf[NoSessionCheckAction]
}

trait ExclusionListController extends FrontendController
  with URIInformation
  with ControllersReferenceData
  with SplunkLogger
  with ExclusionListConfiguration {
  this: TierConnector =>

  val authenticate: AuthAction
  val noSessionCheck: NoSessionCheckAction

  def eiLListService: EiLListService

  def bikListService: BikListService

  def mapYearStringToInt(URIYearString: String): Future[Int] = {
    URIYearString match {
      case utils.FormMappingsConstants.CY => Future.successful(YEAR_RANGE.cyminus1)
      case utils.FormMappingsConstants.CYP1 => Future.successful(YEAR_RANGE.cy)
      case _ => Future.failed(throw new InvalidYearURIException())
    }
  }

  def validateRequest(isCurrentYear: String, iabdType: String)(implicit request: AuthenticatedRequest[_]): Future[Int] = {
    for {
      year <- mapYearStringToInt(isCurrentYear)
      registeredBenefits: List[Bik] <- bikListService.registeredBenefitsList(year, request.empRef)(getRegisteredPath)
    } yield {
      if (registeredBenefits.exists(_.iabdType.equals(iabdValueURLDeMapper(iabdType)))) {
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
        val iabdTypeValue = iabdValueURLDeMapper(iabdType)
        val staticDataRequest = for {
          year <- validateRequest(isCurrentTaxYear, iabdType)
          currentYearList: (Map[String, String], List[Bik]) <- bikListService.currentYearList
          nextYearList: (Map[String, String], List[Bik]) <- bikListService.nextYearList
          currentYearEIL: List[EiLPerson] <- eiLListService.currentYearEiL(iabdTypeValue, year)
        } yield {
          Ok(views.html.exclusion.exclusionOverview(YEAR_RANGE, isCurrentTaxYear, iabdTypeValue, currentYearEIL.sortWith(_.surname < _.surname), request.empRef))
            .removingFromSession(HeaderTags.ETAG)
            .addingToSession(nextYearList._1.toSeq: _*)
        }
        responseErrorHandler(staticDataRequest)

      } else {
        Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED,
          TaxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }

  }

  def withOrWithoutNinoOnPageLoad(isCurrentTaxYear: String, iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val iabdTypeValue = iabdValueURLDeMapper(iabdType)
      if (exclusionsAllowed) {
        val resultFuture = for {
          _ <- validateRequest(isCurrentTaxYear, iabdType)
        } yield {
          Ok(views.html.exclusion.exclusionNinoOrNoNinoForm(YEAR_RANGE, isCurrentTaxYear, iabdTypeValue, empRef = request.empRef))
        }
        responseErrorHandler(resultFuture)
      } else {
        Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED,
          TaxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }
  }

  def withOrWithoutNinoDecision(isCurrentTaxYear: String, iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val iabdTypeValue = iabdValueURLDeMapper(iabdType)
      if (exclusionsAllowed) {
        val taxYearRange = YEAR_RANGE
        val resultFuture = binaryRadioButton.bindFromRequest().fold(
          formWithErrors => Future(Redirect(routes.ExclusionListController.withOrWithoutNinoOnPageLoad(
            isCurrentTaxYear, iabdType)).flashing("error" -> Messages("ExclusionDecision.noselection.error")))
          ,
          values => {
            val selectedValue = values.selectionValue
            for {
              _ <- validateRequest(isCurrentTaxYear, iabdType)
            } yield {
              selectedValue match {
                case FORM_TYPE_NINO => Ok(views.html.exclusion.ninoExclusionSearchForm(taxYearRange,
                  isCurrentTaxYear, iabdTypeValue, exclusionSearchFormWithNino, empRef = request.empRef))
                case FORM_TYPE_NONINO => Ok(views.html.exclusion.noNinoExclusionSearchForm(taxYearRange,
                  isCurrentTaxYear, iabdTypeValue, exclusionSearchFormWithoutNino, empRef = request.empRef))
                case "" => Redirect(routes.ExclusionListController.withOrWithoutNinoOnPageLoad(
                  isCurrentTaxYear, iabdTypeValue)).flashing("error" -> Messages("ExclusionDecision.noselection.error"))
              }
            }
          }
        )
        responseErrorHandler(resultFuture)
      } else {
        Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED, TaxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }
  }

  def searchResults(isCurrentTaxYear: String, iabdType: String, formType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      val iabdTypeValue = iabdValueURLDeMapper(iabdType)
      if (exclusionsAllowed) {
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
        responseErrorHandler(futureResult)
      } else {
        Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED, TaxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
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
          case FORM_TYPE_NINO => Ok(views.html.exclusion.ninoExclusionSearchForm(YEAR_RANGE, isCurrentTaxYear,
            iabdTypeValue, form.bindFromRequest().withError("status", message), existsAlready, empRef = request.empRef))
          case _ => Ok(views.html.exclusion.noNinoExclusionSearchForm(YEAR_RANGE, isCurrentTaxYear,
            iabdTypeValue, form.bindFromRequest().withError("status", message), existsAlready, empRef = request.empRef))
        }

      case _ =>
        Logger.info("Exclusion search matched " + listOfMatches.size + " employees with Optimistic locks " + listOfMatches.map(x => x.perOptLock))
        val filledListOfMatchesForm = individualsFormWithRadio.fill((individualSelectionOption.getOrElse(""),
          EiLPersonList(listOfMatches)))
        Ok(views.html.exclusion.searchResults(YEAR_RANGE, isCurrentTaxYear, iabdTypeValue,
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
        case FORM_TYPE_NINO => Ok(views.html.exclusion.ninoExclusionSearchForm(YEAR_RANGE, isCurrentTaxYear,
          iabdTypeValue, formWithErrors, empRef = request.empRef))
        case FORM_TYPE_NONINO => Ok(views.html.exclusion.noNinoExclusionSearchForm(YEAR_RANGE, isCurrentTaxYear,
          iabdTypeValue, formWithErrors, empRef = request.empRef))
      }
    }
  }

  def updateExclusions(year: String, iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      val iabdTypeValue = iabdValueURLDeMapper(iabdType)
      if (exclusionsAllowed) {
        val taxYearRange = YEAR_RANGE

        val futureResult = processExclusionForm(individualsForm.bindFromRequest, year, iabdTypeValue, taxYearRange)
        responseErrorHandler(futureResult)

      } else {
        Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED, TaxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }
  }

  def updateMultipleExclusions(year: String, iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      val iabdTypeValue = iabdValueURLDeMapper(iabdType)
      if (exclusionsAllowed) {
        val taxYearRange = YEAR_RANGE
        val form = individualsFormWithRadio.bindFromRequest
        val futureResult = processIndividualExclusionForm(form, year, iabdTypeValue, taxYearRange)
        responseErrorHandler(futureResult)
      } else {
        Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED, TaxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }
  }

  def processIndividualExclusionForm(form: Form[(String, EiLPersonList)], isCurrentTaxYear: String,
                                     iabdType: String, taxYearRange: TaxYearRange)
                                    (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val iabdTypeValue = iabdValueURLMapper(iabdType)
    form.fold(
      formWithErrors =>
        for {
          _ <- validateRequest(isCurrentTaxYear, iabdTypeValue)
        } yield {
          Ok(views.html.exclusion.searchResults(taxYearRange, isCurrentTaxYear, iabdType,
            formWithErrors, FORM_TYPE_NONINO, empRef = request.empRef))
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
        Ok(views.html.errorPage(INVALID_FORM_ERROR, taxYearRange, year, empRef = Some(request.empRef)))
      },
      values => {
        val excludedIndividual = extractExcludedIndividual("", values)
        if (excludedIndividual.isEmpty) {
          Future.successful(Ok(views.html.errorPage(INVALID_FORM_ERROR, taxYearRange, year, empRef = Some(request.empRef))))
        } else {
          commitExclusion(year, iabdType, taxYearRange, excludedIndividual)
        }
      }
    )
  }

  def commitExclusion(year: String, iabdType: String, taxYearRange: TaxYearRange, excludedIndividual: Option[EiLPerson])
                     (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent], context: PbikContext): Future[Result] = {
    val yearInt = if (year.equals(utils.FormMappingsConstants.CY)) taxYearRange.cyminus1 else taxYearRange.cy
    val spYear = if (TaxDateUtils.isCurrentTaxYear(yearInt)) spPeriod.CY else spPeriod.CYP1
    val registrationList: RegistrationList = RegistrationList(None, List(RegistrationItem(iabdType, active = false, enabled = false)))

    Logger.info(s"Committing Exclusion for scheme ${request.empRef.toString} , with employees Optimisitic Lock: ${excludedIndividual.get.perOptLock}")

    tierConnector.genericPostCall(baseUrl, exclusionPostUpdatePath(iabdType),
      request.empRef, yearInt, excludedIndividual.get).map {
      response =>
        response.status match {
          case OK => {
            auditExclusion(exclusion = true, yearInt, excludedIndividual.get.nino, iabdType)
            Ok(views.html.exclusion.whatNextExclusion(TaxDateUtils.getTaxYearRange(), year,
              iabdType, excludedIndividual.get.firstForename + " " + excludedIndividual.get.surname, request.empRef))
          }
          case _ => Ok(views.html.errorPage("Could not perform update operation", YEAR_RANGE, "", empRef = Some(request.empRef))(request, context, applicationMessages)).
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
        processRemoval(individualsForm.bindFromRequest, year, iabdType, YEAR_RANGE)
      } else {
        Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED, TaxDateUtils.getTaxYearRange(), empRef = Some(request.empRef))))
      }
  }

  def processRemoval(form: Form[EiLPersonList], year: String,
                     iabdType: String, taxYearRange: TaxYearRange)
                    (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    form.fold(
      formWithErrors => Future.successful(Ok(views.html.errorPage(INVALID_FORM_ERROR, taxYearRange, "", empRef = Some(request.empRef)))),
      values => Future.successful(Ok(views.html.exclusion.removalConfirmation(taxYearRange, year, iabdType,
        individualsForm.fill(values), empRef = request.empRef)))
    )

  }

  def removeExclusionsCommit(iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      val iabdTypeValue = iabdValueURLDeMapper(iabdType)
      val taxYearRange = TaxDateUtils.getTaxYearRange()
      if (exclusionsAllowed) {
        processRemovalCommit(individualsForm.bindFromRequest, iabdTypeValue, taxYearRange)
      } else {
        Future.successful(Ok(views.html.errorPage(FEATURE_RESTRICTED, taxYearRange, empRef = Some(request.empRef))))
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
    val iabdTypeValue = iabdValueURLMapper(iabdType)
    val futureExclude = tierConnector.genericPostCall(baseUrl, exclusionPostRemovePath(iabdType),
      request.empRef, year, individual).map {
      response =>
        response.status match {

          case OK => {
            auditExclusion(exclusion = false, year, extractListNino(removalsList), iabdType)
            Ok(views.html.exclusion.whatNextRescind(TaxDateUtils.getTaxYearRange(), NEXT_TAX_YEAR,
              iabdTypeValue, individual.firstForename + " " + individual.surname, request.empRef))
              .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))

          }
          case _ => Ok(views.html.errorPage("Could not perform update operation", YEAR_RANGE, "", empRef = Some(request.empRef)))
            .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
        }
    }
    responseErrorHandler(futureExclude)
  }


  private def auditExclusion(exclusion: Boolean, year: Int, employee: String, iabdType: String)
                            (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]) = {
    logSplunkEvent(createDataEvent(
      tier = spTier.FRONTEND,
      action = if (exclusion) spAction.ADD else spAction.REMOVE,
      target = spTarget.EIL,
      period = taxYearToSpPeriod(year),
      msg = if (exclusion) "Employee excluded" else "Employee exclusion rescinded",
      nino = Some(employee),
      iabd = Some(iabdType),
      name = Some(request.name),
      empRef = Some(request.empRef)))
  }

}
