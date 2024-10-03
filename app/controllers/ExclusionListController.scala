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

import config.PbikAppConfig
import connectors.PbikConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models._
import models.auth.AuthenticatedRequest
import models.form.MandatoryRadioButton
import models.v1.IabdType.IabdType
import models.v1.exclusion._
import models.v1.trace.{TracePeopleByNinoRequest, TracePeopleByPersonalDetailsRequest, TracePersonResponse}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
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
  pbikAppConfig: PbikAppConfig,
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

  val exclusionsAllowed: Boolean = pbikAppConfig.exclusionsAllowed

  def performPageLoad(isCurrentTaxYear: String, iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val futureResult = showExcludedPage(isCurrentTaxYear, iabdType, formMappings.binaryRadioButton)
      controllersReferenceData.responseErrorHandler(futureResult)
    }

  def submitExcludedEmployees(isCurrentTaxYear: String, iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val resultFuture = formMappings.binaryRadioButton
        .bindFromRequest()
        .fold(
          formWithErrors => showExcludedPage(isCurrentTaxYear, iabdType, formWithErrors),
          values => {
            val selectedValue = values.selectionValue
            for {
              _ <- validateRequest(isCurrentTaxYear, iabdType)
            } yield selectedValue match {
              case ControllersReferenceDataCodes.YES =>
                Redirect(
                  routes.ExclusionListController
                    .withOrWithoutNinoOnPageLoad(isCurrentTaxYear, iabdType)
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

  def validateRequest(isCurrentYear: String, iabdType: IabdType)(implicit
    request: AuthenticatedRequest[_]
  ): Future[Int] = {
    val year = taxDateUtils.mapYearStringToInt(isCurrentYear, controllersReferenceData.yearRange)

    bikListService
      .getRegisteredBenefitsForYear(year)
      .map(registeredBenefits =>
        if (registeredBenefits.getBenefitInKindWithCount.exists(_.iabdType == iabdType)) {
          year
        } else {
          throw new InvalidBikTypeException("Invalid Bik Type")
        }
      )
  }

  private def showExcludedPage(isCurrentTaxYear: String, iabdType: IabdType, form: Form[MandatoryRadioButton])(implicit
    request: AuthenticatedRequest[_]
  ): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    if (exclusionsAllowed) {
      for {
        year           <- validateRequest(isCurrentTaxYear, iabdType)
        currentYearEIL <- eiLListService.exclusionListForYear(iabdType, year, request.empRef)
      } yield {
        sessionService.storeCurrentExclusions(currentYearEIL)
        Ok(
          exclusionOverviewView(
            controllersReferenceData.yearRange,
            isCurrentTaxYear,
            iabdType,
            currentYearEIL.exclusions.sortWith(_.surname < _.surname),
            form
          )
        )
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

  def withOrWithoutNinoOnPageLoad(isCurrentTaxYear: String, iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val futureResult = showWithOrWithoutNino(isCurrentTaxYear, iabdType, formMappings.binaryRadioButton)
      controllersReferenceData.responseErrorHandler(futureResult)
    }

  private def showWithOrWithoutNino(isCurrentTaxYear: String, iabdType: IabdType, form: Form[MandatoryRadioButton])(
    implicit request: AuthenticatedRequest[_]
  ): Future[Result] =
    if (exclusionsAllowed) {
      for {
        _ <- validateRequest(isCurrentTaxYear, iabdType)
      } yield Ok(
        exclusionNinoOrNoNinoFormView(
          controllersReferenceData.yearRange,
          isCurrentTaxYear,
          iabdType,
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

  def withOrWithoutNinoDecision(isCurrentTaxYear: String, iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      if (exclusionsAllowed) {
        val resultFuture = formMappings.binaryRadioButton
          .bindFromRequest()
          .fold(
            formWithErrors => showWithOrWithoutNino(isCurrentTaxYear, iabdType, formWithErrors),
            values => {
              val selectedValue = values.selectionValue
              for {
                _ <- validateRequest(isCurrentTaxYear, iabdType)
              } yield selectedValue match {
                case ControllersReferenceDataCodes.FORM_TYPE_NINO   =>
                  Redirect(
                    routes.ExclusionListController.showExclusionSearchForm(isCurrentTaxYear, iabdType, "nino")
                  )
                case ControllersReferenceDataCodes.FORM_TYPE_NONINO =>
                  Redirect(
                    routes.ExclusionListController.showExclusionSearchForm(isCurrentTaxYear, iabdType, "no-nino")
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

  def showExclusionSearchForm(isCurrentTaxYear: String, iabdType: IabdType, formType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val taxYearRange                 = controllersReferenceData.yearRange
      val resultFuture: Future[Result] = for {
        _ <- validateRequest(isCurrentTaxYear, iabdType)
      } yield formType match {
        case "nino"    =>
          Ok(
            ninoExclusionSearchFormView(
              taxYearRange,
              isCurrentTaxYear,
              iabdType,
              formMappings.exclusionSearchFormWithNino
            )
          )
        case "no-nino" =>
          Ok(
            noNinoExclusionSearchFormView(
              taxYearRange,
              isCurrentTaxYear,
              iabdType,
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

  private def searchResultsByNino(
    isCurrentTaxYear: String,
    iabdType: IabdType,
    formType: String
  )(implicit request: AuthenticatedRequest[_]): Future[Result] =
    formMappings.exclusionSearchFormWithNino
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            Ok(
              ninoExclusionSearchFormView(
                controllersReferenceData.yearRange,
                isCurrentTaxYear,
                iabdType,
                formWithErrors
              )
            )
          ),
        validModel =>
          for {
            year       <- validateRequest(isCurrentTaxYear, iabdType)
            requestBody = TracePeopleByNinoRequest(
                            iabdType,
                            validModel.firstName,
                            validModel.surname,
                            validModel.nino
                          )
            result     <- tierConnector.findPersonByNino(request.empRef, year, requestBody).flatMap {
                            case Left(value)  =>
                              Future.successful(
                                InternalServerError(
                                  errorPageView(
                                    s"ServiceMessage.${value.failures.head.code}",
                                    controllersReferenceData.yearRange
                                  )
                                )
                              )
                            case Right(value) =>
                              sessionService
                                .storeListOfMatches(value)
                                .map(_ =>
                                  Redirect(routes.ExclusionListController.showResults(isCurrentTaxYear, iabdType, formType))
                                )
                          }
          } yield result
      )

  private def searchResultsByPersonalDetails(
    isCurrentTaxYear: String,
    iabdType: IabdType,
    formType: String
  )(implicit request: AuthenticatedRequest[_]): Future[Result] =
    formMappings.exclusionSearchFormWithoutNino
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            Ok(
              noNinoExclusionSearchFormView(
                controllersReferenceData.yearRange,
                isCurrentTaxYear,
                iabdType,
                formWithErrors
              )
            )
          ),
        validModel =>
          for {
            year       <- validateRequest(isCurrentTaxYear, iabdType)
            requestBody = TracePeopleByPersonalDetailsRequest(
                            iabdType,
                            Some(validModel.firstName),
                            validModel.surname,
                            validModel.dateOfBirth.dateOfBirthFormatForNPS,
                            validModel.gender
                          )
            result     <- tierConnector.findPersonByPersonalDetails(request.empRef, year, requestBody).flatMap {
                            case Left(value)  =>
                              Future.successful(
                                InternalServerError(
                                  errorPageView(
                                    s"ServiceMessage.${value.failures.head.code}",
                                    controllersReferenceData.yearRange
                                  )
                                )
                              )
                            case Right(value) =>
                              sessionService
                                .storeListOfMatches(value)
                                .map(_ =>
                                  Redirect(routes.ExclusionListController.showResults(isCurrentTaxYear, iabdType, formType))
                                )
                          }
          } yield result
      )

  def searchResults(isCurrentTaxYear: String, iabdType: IabdType, formType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      if (exclusionsAllowed) {
        val futureResult = formType match {
          case ControllersReferenceDataCodes.FORM_TYPE_NINO   => searchResultsByNino(isCurrentTaxYear, iabdType, formType)
          case ControllersReferenceDataCodes.FORM_TYPE_NONINO =>
            searchResultsByPersonalDetails(isCurrentTaxYear, iabdType, formType)
        }

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

  def showResults(year: String, iabdType: IabdType, formType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val resultFuture = for {
        _                                    <- validateRequest(year, iabdType)
        optionalSession: Option[PbikSession] <- sessionService.fetchPbikSession()
      } yield {
        val session = optionalSession.get
        searchResultsHandleValidResult(
          session.listOfMatches.get.pbikExclusionList,
          year,
          formType,
          iabdType,
          session.currentExclusions.get.mapToTracePerson
        )
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def searchResultsHandleValidResult(
    listOfMatches: List[TracePersonResponse],
    isCurrentTaxYear: String,
    formType: String,
    iabdType: IabdType,
    currentExclusions: List[TracePersonResponse]
  )(implicit request: AuthenticatedRequest[_]): Result = {
    val uniqueListOfMatches: List[TracePersonResponse] =
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
              iabdType = Some(iabdType)
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
              iabdType = Some(iabdType)
            )
          )
        }
      case _ =>
        Ok(
          searchResultsView(
            controllersReferenceData.yearRange,
            isCurrentTaxYear,
            iabdType,
            uniqueListOfMatches,
            formMappings.individualSelectionForm,
            formType
          )
        )
    }
  }

  def updateMultipleExclusions(year: String, iabdType: IabdType, formType: String): Action[AnyContent] =
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
                      iabdType,
                      session.get.listOfMatches.get.pbikExclusionList,
                      formWithErrors,
                      formType
                    )
                  )
                ),
              values => {
                val individualsDetails: Option[TracePersonResponse] =
                  session.get.listOfMatches.get.pbikExclusionList
                    .find(person => person.nationalInsuranceNumber == values.nino)
                validateRequest(year, iabdType).flatMap { _ =>
                  commitExclusion(
                    year,
                    iabdType,
                    controllersReferenceData.yearRange,
                    session.get.listOfMatches.get.updatedEmployerOptimisticLock,
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

  def commitExclusion(
    year: String,
    iabdType: IabdType,
    taxYearRange: TaxYearRange,
    employerOptimisticLock: Int,
    excludedIndividual: Option[TracePersonResponse]
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val yearInt                                                   = if (year.equals(utils.FormMappingsConstants.CY)) taxYearRange.cyminus1 else taxYearRange.cy
    val requestExclusion: UpdateExclusionPersonForABenefitRequest = UpdateExclusionPersonForABenefitRequest(
      employerOptimisticLock,
      PbikExclusionPersonAddRequest(
        iabdType,
        excludedIndividual.get.nationalInsuranceNumber,
        excludedIndividual.get.firstForename,
        excludedIndividual.get.surname,
        excludedIndividual.get.optimisticLock
      )
    )
    tierConnector
      .excludeEiLPersonFromBik(request.empRef, yearInt, requestExclusion)
      .map {
        case Right(value) if value == OK =>
          auditExclusion(exclusion = true, yearInt, excludedIndividual.get.nationalInsuranceNumber, iabdType)
          Redirect(
            routes.ExclusionListController
              .showExclusionConfirmation(year, iabdType)
          )
        case Left(value)                 =>
          val error = value.failures.head
          InternalServerError(
            errorPageView(s"ServiceMessage.${error.code}", controllersReferenceData.yearRange)
          )
        case Right(unexpectedStatus)     =>
          logger.warn(
            s"[ExclusionListController][commitExclusion] Exclusion list update operation was unable to be executed " +
              s"successfully: received $unexpectedStatus response"
          )
          InternalServerError(
            errorPageView("Could not perform update operation", controllersReferenceData.yearRange)
          )
      }
  }

  private def auditExclusion(exclusion: Boolean, year: Int, employee: String, iabdType: IabdType)(implicit
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
        iabd = Some(iabdType.toString),
        name = request.userId,
        empRef = Some(request.empRef)
      )
    )

  def updateExclusions(year: String, iabdType: IabdType): Action[AnyContent] =
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
            validateRequest(year, iabdType).flatMap { _ =>
              commitExclusion(
                year,
                iabdType,
                controllersReferenceData.yearRange,
                session.get.listOfMatches.get.updatedEmployerOptimisticLock,
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

  private def getExcludedPerson(pbikSession: Option[PbikSession]): Option[TracePersonResponse] =
    pbikSession.flatMap { session =>
      (session.currentExclusions.map(_.mapToTracePerson), session.listOfMatches.map(_.pbikExclusionList)) match {
        case (Some(currentExclusions), Some(listOfMatches)) =>
          eiLListService
            .searchResultsRemoveAlreadyExcluded(currentExclusions, listOfMatches)
            .headOption
        case _                                              => None
      }
    }

  def showExclusionConfirmation(year: String, iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val resultFuture = for {
        _       <- validateRequest(year, iabdType)
        session <- sessionService.fetchPbikSession()
      } yield Ok(
        whatNextExclusionView(
          taxDateUtils.getTaxYearRange(),
          year,
          iabdType,
          session.get.listOfMatches.get.pbikExclusionList.head
        )
      )
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def remove(year: String, iabdType: IabdType, nino: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      if (exclusionsAllowed) {
        val resultFuture = sessionService.fetchPbikSession().flatMap { session =>
          val selectedPerson: PbikExclusionPerson = session.get.currentExclusions.get.exclusions
            .filter(person => person.nationalInsuranceNumber == nino)
            .head
          sessionService
            .storeEiLPerson(
              SelectedExclusionToRemove(session.get.currentExclusions.get.currentEmployerOptimisticLock, selectedPerson)
            )
            .map { _ =>
              Redirect(routes.ExclusionListController.showRemovalConfirmation(year, iabdType))
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

  def showRemovalConfirmation(year: String, iabdType: IabdType): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val futureResult = sessionService.fetchPbikSession().map { session =>
        Ok(
          removalConfirmationView(
            controllersReferenceData.yearRange,
            iabdType,
            session.get.eiLPerson.get.personToExclude
          )
        )
      }
      controllersReferenceData.responseErrorHandler(futureResult)
    }

  def removeExclusionsCommit(iabdType: IabdType): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      val taxYearRange               = taxDateUtils.getTaxYearRange()
      if (exclusionsAllowed) {
        val resultFuture = sessionService.fetchPbikSession().flatMap { session =>
          val individual            = session.get.eiLPerson.get
          val year                  = taxYearRange.cy
          val removalsList          = List(individual.personToExclude)
          val individualWithBenefit =
            PbikExclusionPersonWithBenefitRequest(individual.employerOptimisticLock, individual.personToExclude)
          tierConnector
            .removeEiLPersonExclusionFromBik(iabdType, request.empRef, year, individualWithBenefit)
            .map {
              case Right(value) if value == OK =>
                auditExclusion(
                  exclusion = false,
                  year,
                  splunkLogger.extractListNinoFromExclusions(removalsList),
                  iabdType
                )
                Redirect(routes.ExclusionListController.showRemovalWhatsNext(iabdType))
              case Left(value)                 =>
                val error = value.failures.head
                InternalServerError(
                  errorPageView(s"ServiceMessage.${error.code}", controllersReferenceData.yearRange)
                )
              case Right(unexpectedStatus)     =>
                logger.warn(
                  s"[ExclusionListController][removeExclusionsCommit] Exclusion list update operation was unable to be executed successfully:" +
                    s" received $unexpectedStatus response"
                )
                Ok(
                  errorPageView("Could not perform update operation", controllersReferenceData.yearRange)
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

  def showRemovalWhatsNext(iabdType: IabdType): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val futureResult = sessionService.fetchPbikSession().map { session =>
        val individual = session.get.eiLPerson.get
        Ok(
          whatNextRescindView(
            taxDateUtils.getTaxYearRange(),
            FormMappingsConstants.CYP1,
            iabdType,
            individual.personToExclude
          )
        )
      }
      controllersReferenceData.responseErrorHandler(futureResult)
  }

}
