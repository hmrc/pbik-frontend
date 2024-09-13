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

package controllers

import connectors.PbikConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models._
import models.v1.IabdType
import models.v1.exclusion._
import models.v1.trace.{TracePeopleByPersonalDetails, TracePeopleByPersonalDetailsRequest, TracePerson}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import play.api.{Configuration, Logging}
import services.{BikListService, EiLListService, SessionService}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Exceptions.InvalidBikTypeException
import utils._
import views.html.ErrorPage
import views.html.exclusion._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExclusionListController @Inject() (
  formMappings: FormMappings,
  val authenticate: AuthAction,
  cc: MessagesControllerComponents,
  override val messagesApi: MessagesApi,
  val noSessionCheck: NoSessionCheckAction,
  val eiLListService: EiLListService,
  val bikListService: BikListService,
  val sessionService: SessionService,
  val tierConnector: PbikConnector,
  taxDateUtils: TaxDateUtils,
  splunkLogger: SplunkLogger,
  controllersReferenceData: ControllersReferenceData,
  configuration: Configuration,
  exclusionOverviewView: ExclusionOverview,
  errorPageView: ErrorPage,
  exclusionNinoOrNoNinoFormView: ExclusionNinoOrNoNinoForm,
  ninoExclusionSearchFormView: NinoExclusionSearchForm,
  noNinoExclusionSearchFormView: NoNinoExclusionSearchForm,
  searchResultsView: SearchResults,
  whatNextExclusionView: WhatNextExclusion,
  removalConfirmationView: RemovalConfirmation,
  whatNextRescindView: WhatNextRescind
)(implicit ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport
    with Logging
    with WithUnsafeDefaultFormBinding {

  lazy val exclusionsAllowed: Boolean = configuration.get[Boolean]("pbik.enabled.eil")

  def validateRequest(isCurrentYear: String, iabdString: String)(implicit
    request: AuthenticatedRequest[_]
  ): Future[Int] =
    for {
      year                          <- taxDateUtils.mapYearStringToInt(isCurrentYear, controllersReferenceData.yearRange)
      registeredBenefits: List[Bik] <-
        bikListService.registeredBenefitsList(year, request.empRef)
    } yield
      if (registeredBenefits.exists(_.iabdType.equals(Bik.asNPSTypeValue(iabdString)))) {
        year
      } else {
        throw new InvalidBikTypeException()
      }

  def performPageLoad(isCurrentTaxYear: String, iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val futureResult = showExcludedPage(isCurrentTaxYear, iabdString, formMappings.binaryRadioButton)
      controllersReferenceData.responseErrorHandler(futureResult)

    }

  private def showExcludedPage(isCurrentTaxYear: String, iabdString: String, form: Form[MandatoryRadioButton])(implicit
    request: AuthenticatedRequest[_]
  ): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    if (exclusionsAllowed) {
      val iabdType = IabdType(Bik.asNPSTypeValue(iabdString).toInt).encodedName
      for {
        year           <- validateRequest(isCurrentTaxYear, iabdString)
        nextYearList   <- bikListService.nextYearList
        currentYearEIL <- eiLListService.currentYearEiL(iabdType, year)
      } yield {
        sessionService.storeCurrentExclusions(currentYearEIL)
        Ok(
          exclusionOverviewView(
            controllersReferenceData.yearRange,
            isCurrentTaxYear,
            iabdString,
            currentYearEIL.getPBIKExclusionList.sortWith(_.surname < _.surname),
            form
          )
        ).removingFromSession(HeaderTags.ETAG).addingToSession(nextYearList.headers.toSeq: _*)
      }

    } else {
      logger.info("[ExclusionListController][showExcludedPage] Exclusions not allowed, showing error page")
      Future.successful(
        Ok(
          errorPageView(
            ControllersReferenceDataCodes.FEATURE_RESTRICTED,
            taxDateUtils.getTaxYearRange()
          )
        )
      )
    }
  }

  def submitExcludedEmployees(isCurrentTaxYear: String, iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val resultFuture = formMappings.binaryRadioButton
        .bindFromRequest()
        .fold(
          formWithErrors => showExcludedPage(isCurrentTaxYear, iabdString, formWithErrors),
          values => {
            val selectedValue = values.selectionValue
            for {
              _ <- validateRequest(isCurrentTaxYear, iabdString)
            } yield selectedValue match {
              case ControllersReferenceDataCodes.YES =>
                Redirect(
                  routes.ExclusionListController
                    .withOrWithoutNinoOnPageLoad(isCurrentTaxYear, iabdString)
                )
              case ControllersReferenceDataCodes.NO  =>
                if (isCurrentTaxYear == utils.FormMappingsConstants.CY) {
                  Redirect(routes.HomePageController.onPageLoadCY)
                } else {
                  Redirect(routes.HomePageController.onPageLoadCY1)
                }
            }
          }
        )
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def withOrWithoutNinoOnPageLoad(isCurrentTaxYear: String, iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val futureResult = showWithOrWithoutNino(isCurrentTaxYear, iabdString, formMappings.binaryRadioButton)
      controllersReferenceData.responseErrorHandler(futureResult)
    }

  private def showWithOrWithoutNino(isCurrentTaxYear: String, iabdString: String, form: Form[MandatoryRadioButton])(
    implicit request: AuthenticatedRequest[_]
  ): Future[Result] =
    if (exclusionsAllowed) {
      for {
        _ <- validateRequest(isCurrentTaxYear, iabdString)
      } yield Ok(
        exclusionNinoOrNoNinoFormView(
          controllersReferenceData.yearRange,
          isCurrentTaxYear,
          iabdString,
          form = form
        )
      )

    } else {
      logger.info("[ExclusionListController][withOrWithoutNinoOnPageLoad] Exclusions not allowed, showing error page")
      Future {
        Forbidden(
          errorPageView(
            ControllersReferenceDataCodes.FEATURE_RESTRICTED,
            taxDateUtils.getTaxYearRange()
          )
        )
      }
    }

  def withOrWithoutNinoDecision(isCurrentTaxYear: String, iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      if (exclusionsAllowed) {
        val resultFuture = formMappings.binaryRadioButton
          .bindFromRequest()
          .fold(
            formWithErrors => showWithOrWithoutNino(isCurrentTaxYear, iabdString, formWithErrors),
            values => {
              val selectedValue = values.selectionValue
              for {
                _ <- validateRequest(isCurrentTaxYear, iabdString)
              } yield selectedValue match {
                case ControllersReferenceDataCodes.FORM_TYPE_NINO   =>
                  Redirect(
                    routes.ExclusionListController.showExclusionSearchForm(isCurrentTaxYear, iabdString, "nino")
                  )
                case ControllersReferenceDataCodes.FORM_TYPE_NONINO =>
                  Redirect(
                    routes.ExclusionListController.showExclusionSearchForm(isCurrentTaxYear, iabdString, "no-nino")
                  )
              }
            }
          )
        controllersReferenceData.responseErrorHandler(resultFuture)
      } else {
        logger.info("[ExclusionListController][withOrWithoutNinoDecision] Exclusions not allowed, showing error page")
        Future.successful(
          Forbidden(
            errorPageView(
              ControllersReferenceDataCodes.FEATURE_RESTRICTED,
              taxDateUtils.getTaxYearRange()
            )
          )
        )
      }
    }

  def showExclusionSearchForm(isCurrentTaxYear: String, iabdString: String, formType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val taxYearRange                 = controllersReferenceData.yearRange
      val resultFuture: Future[Result] = for {
        _ <- validateRequest(isCurrentTaxYear, iabdString)
      } yield formType match {
        case "nino"    =>
          Ok(
            ninoExclusionSearchFormView(
              taxYearRange,
              isCurrentTaxYear,
              iabdString,
              formMappings.exclusionSearchFormWithNino
            )
          )
        case "no-nino" =>
          Ok(
            noNinoExclusionSearchFormView(
              taxYearRange,
              isCurrentTaxYear,
              iabdString,
              formMappings.exclusionSearchFormWithoutNino
            )
          )
        case _         =>
          InternalServerError(
            errorPageView(
              ControllersReferenceDataCodes.INVALID_FORM_ERROR,
              taxDateUtils.getTaxYearRange()
            )
          )
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def searchResults(isCurrentTaxYear: String, iabdString: String, formType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      if (exclusionsAllowed) {
        val form: Form[EiLPerson]        = formType match {
          case ControllersReferenceDataCodes.FORM_TYPE_NINO   => formMappings.exclusionSearchFormWithNino
          case ControllersReferenceDataCodes.FORM_TYPE_NONINO => formMappings.exclusionSearchFormWithoutNino
        }
        val futureResult: Future[Result] = form
          .bindFromRequest()
          .fold(
            formWithErrors => searchResultsHandleFormErrors(isCurrentTaxYear, formType, iabdString, formWithErrors),
            validModel =>
              formType match {
                case ControllersReferenceDataCodes.FORM_TYPE_NINO   => ??? // TODO add logic
                case ControllersReferenceDataCodes.FORM_TYPE_NONINO =>
                  val requestBody = TracePeopleByPersonalDetailsRequest(
                    TracePeopleByPersonalDetails(
                      Some(validModel.firstForename),
                      validModel.secondForename,
                      validModel.surname,
                      validModel.dateOfBirth.get,
                      Gender.fromString(validModel.gender)
                    )
                  )

                  validateRequest(isCurrentTaxYear, iabdString).flatMap { year =>
                    tierConnector.findPersonByPersonalDetails(request.empRef, year, requestBody).map {
                      case Left(value)  => ??? //TODO add return error page view with right error code inside as param
                      case Right(value) =>
                        sessionService.storeListOfMatches(value.pbikExclusionList)
                        Redirect(routes.ExclusionListController.showResults(isCurrentTaxYear, iabdString, formType))
                    }
                  }
              }
          )
        controllersReferenceData.responseErrorHandler(futureResult)
      } else {
        logger.info("[ExclusionListController][searchResults] Exclusions not allowed, showing error page")
        Future.successful(
          Forbidden(
            errorPageView(
              ControllersReferenceDataCodes.FEATURE_RESTRICTED,
              taxDateUtils.getTaxYearRange()
            )
          )
        )
      }
    }

  def showResults(year: String, iabdString: String, formType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val resultFuture = for {
        _                                    <- validateRequest(year, iabdString)
        optionalSession: Option[PbikSession] <- sessionService.fetchPbikSession()
      } yield {
        val session = optionalSession.get
        searchResultsHandleValidResult(
          session.listOfMatches.get,
          year,
          formType,
          iabdString,
          session.currentExclusions.get.mapToTracePerson
        )
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def searchResultsHandleValidResult(
    listOfMatches: List[TracePerson],
    isCurrentTaxYear: String,
    formType: String,
    iabdString: String,
    currentExclusions: List[TracePerson]
  )(implicit request: AuthenticatedRequest[_]): Result = {
    val uniqueListOfMatches: List[TracePerson] =
      eiLListService.searchResultsRemoveAlreadyExcluded(currentExclusions, listOfMatches)
    uniqueListOfMatches.size match {
      case 0 =>
        logger.warn("[ExclusionListController][searchResultsHandleValidResult] List of un-excluded matches is empty")
        val message = Messages("ExclusionSearch.Fail.Exists.P")
        if (listOfMatches.nonEmpty) {
          logger.info("[ExclusionListController][searchResultsHandleValidResult] Matching has already been excluded")
          val errorCode = 63085
          Ok(
            errorPageView(
              errorMessage = message,
              taxYearRange = controllersReferenceData.yearRange,
              isCurrentTaxYear = isCurrentTaxYear,
              code = errorCode,
              iabdString = iabdString
            )
          )
        } else {
          logger.warn("[ExclusionListController][searchResultsHandleValidResult] Cached list of matches is empty")
          val errorCode = 63082
          NotFound(
            errorPageView(
              errorMessage = message,
              taxYearRange = controllersReferenceData.yearRange,
              isCurrentTaxYear = isCurrentTaxYear,
              code = errorCode,
              iabdString = iabdString
            )
          )
        }
      case _ =>
        logger.info(
          s"[ExclusionListController][searchResultsHandleValidResult] Exclusion search matched ${listOfMatches.size}" +
            s" employees with Optimistic locks ${uniqueListOfMatches.map(_.optimisticLock)}"
        )
        Ok(
          searchResultsView(
            controllersReferenceData.yearRange,
            isCurrentTaxYear,
            iabdString,
            uniqueListOfMatches,
            formMappings.individualSelectionForm,
            formType
          )
        )
    }
  }

  /*
   * Handles form errors on search results page
   * Will display relevant form page
   */
  def searchResultsHandleFormErrors(
    isCurrentTaxYear: String,
    formType: String,
    iabdString: String,
    formWithErrors: Form[EiLPerson]
  )(implicit request: AuthenticatedRequest[_]): Future[Result] =
    Future {
      formType match {
        case ControllersReferenceDataCodes.FORM_TYPE_NINO   =>
          Ok(
            ninoExclusionSearchFormView(
              controllersReferenceData.yearRange,
              isCurrentTaxYear,
              iabdString,
              formWithErrors
            )
          )
        case ControllersReferenceDataCodes.FORM_TYPE_NONINO =>
          Ok(
            noNinoExclusionSearchFormView(
              controllersReferenceData.yearRange,
              isCurrentTaxYear,
              iabdString,
              formWithErrors
            )
          )
      }
    }

  def updateMultipleExclusions(year: String, iabdString: String, formType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      if (exclusionsAllowed) {
        val resultFuture = sessionService.fetchPbikSession().flatMap { session =>
          formMappings.individualSelectionForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    searchResultsView(
                      controllersReferenceData.yearRange,
                      year,
                      iabdString,
                      session.get.listOfMatches.get,
                      formWithErrors,
                      formType
                    )
                  )
                ),
              values => {
                val individualsDetails: Option[TracePerson] =
                  session.get.listOfMatches.get.find(person => person.identifier == values.nino)
                validateRequest(year, iabdString).flatMap { _ =>
                  commitExclusion(
                    year,
                    iabdString,
                    controllersReferenceData.yearRange,
                    individualsDetails
                  )
                }
              }
            )
        }
        controllersReferenceData.responseErrorHandler(resultFuture)
      } else {
        logger.info("[ExclusionListController][updateMultipleExclusions] Exclusions not allowed, showing error page")
        Future.successful(
          Forbidden(
            errorPageView(
              ControllersReferenceDataCodes.FEATURE_RESTRICTED,
              taxDateUtils.getTaxYearRange()
            )
          )
        )
      }
    }

  private def getExcludedPerson(pbikSession: Option[PbikSession]): Option[TracePerson] =
    pbikSession.flatMap { session =>
      (session.currentExclusions, session.listOfMatches) match {
        case (Some(currentExclusions), Some(listOfMatches)) =>
          Some(
            eiLListService
              .searchResultsRemoveAlreadyExcluded(currentExclusions.mapToTracePerson, listOfMatches)
              .head
          )
        case _                                              => None
      }
    }

  def updateExclusions(year: String, iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      if (exclusionsAllowed) {
        val resultFuture = sessionService.fetchPbikSession().flatMap { session: Option[PbikSession] =>
          getExcludedPerson(session).fold {
            logger.error(
              "[ExclusionListController][updateExclusions] can not get excludedPerson, session data not filled"
            )
            Future.successful(
              InternalServerError(
                errorPageView(
                  ControllersReferenceDataCodes.DEFAULT_ERROR,
                  taxDateUtils.getTaxYearRange()
                )
              )
            )
          } { excludedPerson =>
            validateRequest(year, iabdString).flatMap { _ =>
              commitExclusion(
                year,
                iabdString,
                controllersReferenceData.yearRange,
                Some(excludedPerson)
              )
            }
          }
        }
        controllersReferenceData.responseErrorHandler(resultFuture)
      } else {
        logger.info("[ExclusionListController][updateExclusions] Exclusions not allowed, showing error page")
        Future.successful(
          Forbidden(
            errorPageView(
              ControllersReferenceDataCodes.FEATURE_RESTRICTED,
              taxDateUtils.getTaxYearRange()
            )
          )
        )
      }
    }

  def showExclusionConfirmation(year: String, iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val resultFuture = for {
        _       <- validateRequest(year, iabdString)
        session <- sessionService.fetchPbikSession()
      } yield Ok(
        whatNextExclusionView(
          taxDateUtils.getTaxYearRange(),
          year,
          iabdString,
          session.get.listOfMatches.get.head
        )
      )
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def commitExclusion(
    year: String,
    iabdString: String,
    taxYearRange: TaxYearRange,
    excludedIndividual: Option[TracePerson]
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val yearInt                                                   = if (year.equals(utils.FormMappingsConstants.CY)) taxYearRange.cyminus1 else taxYearRange.cy
    logger.info(
      s"[ExclusionListController][commitExclusion] Committing Exclusion for scheme ${request.empRef.toString}" +
        s", with employees Optimistic Lock: ${excludedIndividual.map(eiLPerson => eiLPerson.optimisticLock).getOrElse(0)}"
    )
    val iabd                                                      = IabdType(Bik.asNPSTypeValue(iabdString).toInt)
    val requestExclusion: UpdateExclusionPersonForABenefitRequest = UpdateExclusionPersonForABenefitRequest(
      excludedIndividual.map(_.optimisticLock).getOrElse(0),
      PbikExclusionPersonAddRequest(
        iabd,
        excludedIndividual.get.identifier,
        excludedIndividual.get.firstForename,
        excludedIndividual.get.secondForename,
        excludedIndividual.get.surname
      )
    )
    tierConnector
      .excludeEiLPersonFromBik(request.empRef, yearInt, requestExclusion)
      .map {
        case OK               =>
          auditExclusion(exclusion = true, yearInt, excludedIndividual.get.identifier, iabdString)
          Redirect(
            routes.ExclusionListController
              .showExclusionConfirmation(year, iabdString)
          )
        case unexpectedStatus =>
          logger.warn(
            s"[ExclusionListController][commitExclusion] Exclusion list update operation was unable to be executed " +
              s"successfully: received $unexpectedStatus response"
          )
          InternalServerError(
            errorPageView(
              "Could not perform update operation",
              controllersReferenceData.yearRange,
              isCurrentTaxYear = ""
            )
          )
            .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
      }
  }

  /*
   * Extracts the matched individual from an empty list ( error ), single item list or matching a nino from a
   * radio button form.
   */
  def extractExcludedIndividual(chosenNino: String, individuals: EiLPersonList): Option[EiLPerson] =
    individuals.active.size match {
      case 0 => None
      case 1 => Some(individuals.active.head)
      case _ =>
        chosenNino.trim.length match {
          case 0 => Some(individuals.active.head)
          case _ => individuals.active.find(person => person.nino == chosenNino)
        }
    }

  def remove(year: String, iabdString: String, nino: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      if (exclusionsAllowed) {
        val resultFuture = sessionService.fetchPbikSession().flatMap { session =>
          val selectedPerson: PbikExclusionPerson = session.get.currentExclusions.get.getPBIKExclusionList
            .filter(person => person.identifier == nino)
            .head
          sessionService.storeEiLPerson(selectedPerson).map { _ =>
            Redirect(routes.ExclusionListController.showRemovalConfirmation(year, iabdString))
          }
        }
        controllersReferenceData.responseErrorHandler(resultFuture)
      } else {
        logger.info("[ExclusionListController][remove] Exclusions not allowed, showing error page")
        Future.successful(
          Forbidden(
            errorPageView(
              ControllersReferenceDataCodes.FEATURE_RESTRICTED,
              taxDateUtils.getTaxYearRange()
            )
          )
        )
      }
    }

  def showRemovalConfirmation(year: String, iabdString: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val futureResult = sessionService.fetchPbikSession().map { session =>
        Ok(
          removalConfirmationView(
            controllersReferenceData.yearRange,
            iabdString,
            session.get.eiLPerson.get
          )
        )
      }
      controllersReferenceData.responseErrorHandler(futureResult)
    }

  def removeExclusionsCommit(iabdString: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      val taxYearRange               = taxDateUtils.getTaxYearRange()
      if (exclusionsAllowed) {
        val resultFuture = sessionService.fetchPbikSession().flatMap { session =>
          val individual            = session.get.eiLPerson.get
          val year                  = taxYearRange.cy
          val removalsList          = List(individual)
          val individualWithBenefit = PbikExclusionPersonWithBenefitRequest.from(
            IabdType(Bik.asNPSTypeValue(iabdString).toInt),
            individual
          )
          tierConnector
            .removeEiLPersonExclusionFromBik(iabdString, request.empRef, year, individualWithBenefit)
            .map {
              case OK               =>
                auditExclusion(
                  exclusion = false,
                  year,
                  splunkLogger.extractListNinoFromExclusions(removalsList),
                  iabdString
                )
                Redirect(routes.ExclusionListController.showRemovalWhatsNext(iabdString))
              case unexpectedStatus =>
                logger.warn(
                  s"[ExclusionListController][removeExclusionsCommit] Exclusion list update operation was unable to be executed successfully:" +
                    s" received $unexpectedStatus response"
                )
                Ok(
                  errorPageView(
                    "Could not perform update operation",
                    controllersReferenceData.yearRange,
                    ""
                  )
                )
                  .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
            }
        }
        controllersReferenceData.responseErrorHandler(resultFuture)
      } else {
        logger.info("[ExclusionListController][removeExclusionsCommit] Exclusions not allowed, showing error page")
        Future.successful(
          Forbidden(
            errorPageView(ControllersReferenceDataCodes.FEATURE_RESTRICTED, taxYearRange)
          )
        )
      }
  }

  def showRemovalWhatsNext(iabdString: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val futureResult = sessionService.fetchPbikSession().map { session =>
        val individual = session.get.eiLPerson.get
        Ok(
          whatNextRescindView(
            taxDateUtils.getTaxYearRange(),
            ControllersReferenceDataCodes.NEXT_TAX_YEAR,
            iabdString,
            individual
          )
        )
      }
      controllersReferenceData.responseErrorHandler(futureResult)
  }

  private def auditExclusion(exclusion: Boolean, year: Int, employee: String, iabdString: String)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[AnyContent]
  ) =
    splunkLogger.logSplunkEvent(
      splunkLogger.createDataEvent(
        tier = splunkLogger.FRONTEND,
        action = if (exclusion) splunkLogger.ADD else splunkLogger.REMOVE,
        target = splunkLogger.EIL,
        period = splunkLogger.taxYearToSpPeriod(year),
        msg = if (exclusion) "Employee excluded" else "Employee exclusion rescinded",
        nino = Some(employee),
        iabd = Some(Bik.asNPSTypeValue(iabdString)),
        name = Some(request.name),
        empRef = Some(request.empRef)
      )
    )

}
