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

package controllers

import connectors.HmrcTierConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models._
import play.api.Configuration
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Codec.utf_8
import play.api.mvc._
import services.{BikListService, EiLListService, SessionService}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Exceptions.{InvalidBikTypeURIException, InvalidYearURIException}
import utils._
import views.html.ErrorPage
import views.html.exclusion._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ExclusionListController @Inject()(
  formMappings: FormMappings,
  val authenticate: AuthAction,
  cc: MessagesControllerComponents,
  override val messagesApi: MessagesApi,
  val noSessionCheck: NoSessionCheckAction,
  val eiLListService: EiLListService,
  val bikListService: BikListService,
  val cachingService: SessionService,
  val tierConnector: HmrcTierConnector, //TODO: Why do we need this?,
  taxDateUtils: TaxDateUtils,
  splunkLogger: SplunkLogger,
  controllersReferenceData: ControllersReferenceData,
  configuration: Configuration,
  uriInformation: URIInformation,
  exclusionOverviewView: ExclusionOverview,
  errorPageView: ErrorPage,
  exclusionNinoOrNoNinoFormView: ExclusionNinoOrNoNinoForm,
  ninoExclusionSearchFormView: NinoExclusionSearchForm,
  noNinoExclusionSearchFormView: NoNinoExclusionSearchForm,
  searchResultsView: SearchResults,
  whatNextExclusionView: WhatNextExclusion,
  removalConfirmationView: RemovalConfirmation,
  whatNextRescindView: WhatNextRescind)
    extends FrontendController(cc) with I18nSupport with Logging {

  lazy val exclusionsAllowed: Boolean = configuration.get[Boolean]("pbik.enabled.eil")

  def mapYearStringToInt(URIYearString: String): Future[Int] =
    URIYearString match {
      case utils.FormMappingsConstants.CY   => Future.successful(controllersReferenceData.yearRange.cyminus1)
      case utils.FormMappingsConstants.CYP1 => Future.successful(controllersReferenceData.yearRange.cy)
      case _                                => Future.failed(throw new InvalidYearURIException())
    }

  def validateRequest(isCurrentYear: String, iabdType: String)(implicit request: AuthenticatedRequest[_]): Future[Int] =
    for {
      year <- mapYearStringToInt(isCurrentYear)
      registeredBenefits: List[Bik] <- bikListService.registeredBenefitsList(year, request.empRef)(
                                        uriInformation.getRegisteredPath)
    } yield {
      if (registeredBenefits.exists(_.iabdType.equals(uriInformation.iabdValueURLDeMapper(iabdType)))) {
        year
      } else {
        throw new InvalidBikTypeURIException()
      }
    }

  def performPageLoad(isCurrentTaxYear: String, iabdType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val futureResult = showExcludedPage(isCurrentTaxYear, iabdType, formMappings.binaryRadioButton)
      controllersReferenceData.responseErrorHandler(futureResult)

    }

  def showExcludedPage(isCurrentTaxYear: String, iabdType: String, form: Form[MandatoryRadioButton])(
    implicit request: AuthenticatedRequest[_]): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    if (exclusionsAllowed) {
      val iabdTypeValue = uriInformation.iabdValueURLDeMapper(iabdType)
      for {
        year                                           <- validateRequest(isCurrentTaxYear, iabdType)
        nextYearList: (Map[String, String], List[Bik]) <- bikListService.nextYearList
        currentYearEIL: List[EiLPerson]                <- eiLListService.currentYearEiL(iabdTypeValue, year)
      } yield {
        cachingService.cacheCurrentExclusions(currentYearEIL)
        Ok(
          exclusionOverviewView(
            controllersReferenceData.yearRange,
            isCurrentTaxYear,
            iabdTypeValue,
            currentYearEIL.sortWith(_.surname < _.surname),
            request.empRef,
            form
          )
        ).removingFromSession(HeaderTags.ETAG)
          .addingToSession(nextYearList._1.toSeq: _*)
      }

    } else {
      logger.info("[ExclusionListController][showExcludedPage] Exclusions not allowed, showing error page")
      Future.successful(
        Ok(
          errorPageView(
            ControllersReferenceDataCodes.FEATURE_RESTRICTED,
            taxDateUtils.getTaxYearRange(),
            empRef = Some(request.empRef))))
    }
  }

  def submitExcludedEmployees(isCurrentTaxYear: String, iabdType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val resultFuture = formMappings.binaryRadioButton
        .bindFromRequest()
        .fold(
          formWithErrors => showExcludedPage(isCurrentTaxYear, iabdType, formWithErrors),
          values => {
            val selectedValue = values.selectionValue
            for {
              _ <- validateRequest(isCurrentTaxYear, iabdType)
            } yield {
              selectedValue match {
                case ControllersReferenceDataCodes.YES =>
                  Redirect(
                    routes.ExclusionListController
                      .withOrWithoutNinoOnPageLoad(isCurrentTaxYear, iabdType)
                  )
                case "no" =>
                  Redirect(routes.HomePageController.onPageLoad)
              }
            }
          }
        )
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def withOrWithoutNinoOnPageLoad(isCurrentTaxYear: String, iabdType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val futureResult = showWithOrWithoutNino(isCurrentTaxYear, iabdType, formMappings.binaryRadioButton)
      controllersReferenceData.responseErrorHandler(futureResult)
    }

  def showWithOrWithoutNino(isCurrentTaxYear: String, iabdType: String, form: Form[MandatoryRadioButton])(
    implicit request: AuthenticatedRequest[_]): Future[Result] = {
    val iabdTypeValue = uriInformation.iabdValueURLDeMapper(iabdType)
    if (exclusionsAllowed) {
      for {
        _ <- validateRequest(isCurrentTaxYear, iabdType)
      } yield {
        Ok(
          exclusionNinoOrNoNinoFormView(
            controllersReferenceData.yearRange,
            isCurrentTaxYear,
            iabdTypeValue,
            form = form,
            empRef = request.empRef))
      }

    } else {
      logger.info("[ExclusionListController][withOrWithoutNinoOnPageLoad] Exclusions not allowed, showing error page")
      Future {
        Forbidden(
          errorPageView(
            ControllersReferenceDataCodes.FEATURE_RESTRICTED,
            taxDateUtils.getTaxYearRange(),
            empRef = Some(request.empRef)))
      }
    }
  }

  def withOrWithoutNinoDecision(isCurrentTaxYear: String, iabdType: String): Action[AnyContent] =
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
              } yield {
                selectedValue match {
                  case ControllersReferenceDataCodes.FORM_TYPE_NINO =>
                    Redirect(
                      routes.ExclusionListController.showExclusionSearchForm(isCurrentTaxYear, iabdType, "nino")
                    )
                  case ControllersReferenceDataCodes.FORM_TYPE_NONINO =>
                    Redirect(
                      routes.ExclusionListController.showExclusionSearchForm(isCurrentTaxYear, iabdType, "no-nino"))
                }
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
              taxDateUtils.getTaxYearRange(),
              empRef = Some(request.empRef))))
      }
    }

  def showExclusionSearchForm(isCurrentTaxYear: String, iabdType: String, formType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val taxYearRange = controllersReferenceData.yearRange
      val iabdTypeValue = uriInformation.iabdValueURLDeMapper(iabdType)
      val resultFuture = for {
        _ <- validateRequest(isCurrentTaxYear, iabdType)
      } yield {
        formType match {
          case "nino" =>
            Ok(
              ninoExclusionSearchFormView(
                taxYearRange,
                isCurrentTaxYear,
                iabdTypeValue,
                formMappings.exclusionSearchFormWithNino,
                empRef = request.empRef))
          case "no-nino" =>
            Ok(
              noNinoExclusionSearchFormView(
                taxYearRange,
                isCurrentTaxYear,
                iabdTypeValue,
                formMappings.exclusionSearchFormWithoutNino,
                empRef = request.empRef))
          case _ =>
            InternalServerError(
              errorPageView(
                ControllersReferenceDataCodes.INVALID_FORM_ERROR,
                taxDateUtils.getTaxYearRange(),
                empRef = Some(request.empRef)))
        }
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def searchResults(isCurrentTaxYear: String, iabdType: String, formType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      val iabdTypeValue = uriInformation.iabdValueURLDeMapper(iabdType)
      if (exclusionsAllowed) {
        val form = formType match {
          case ControllersReferenceDataCodes.FORM_TYPE_NINO   => formMappings.exclusionSearchFormWithNino
          case ControllersReferenceDataCodes.FORM_TYPE_NONINO => formMappings.exclusionSearchFormWithoutNino
        }
        val futureResult = form
          .bindFromRequest()
          .fold(
            formWithErrors => searchResultsHandleFormErrors(isCurrentTaxYear, formType, iabdTypeValue, formWithErrors),
            validModel => {
              for {
                year <- validateRequest(isCurrentTaxYear, iabdType)
                result <- tierConnector.genericPostCall(
                           uriInformation.baseUrl,
                           uriInformation.exclusionPostUpdatePath(iabdTypeValue),
                           request.empRef,
                           year,
                           validModel)
                resultAlreadyExcluded: List[EiLPerson] <- eiLListService.currentYearEiL(iabdTypeValue, year)
                cache                                  <- cachingService.cacheListOfMatches(result.json.validate[List[EiLPerson]].asOpt.get)
              } yield {
                Redirect(routes.ExclusionListController.showResults(isCurrentTaxYear, iabdType, formType))
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
              taxDateUtils.getTaxYearRange(),
              empRef = Some(request.empRef))))
      }
    }
  def showResults(year: String, iabdType: String, formType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val iabdTypeValue = uriInformation.iabdValueURLDeMapper(iabdType)
      val resultFuture = for {
        _                                    <- validateRequest(year, iabdType)
        optionalSession: Option[PbikSession] <- cachingService.fetchPbikSession()
      } yield {
        val session = optionalSession.get
        searchResultsHandleValidResult(
          session.listOfMatches.get,
          year,
          formType,
          iabdTypeValue,
          session.currentExclusions.get)
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  /*
   * Handles valid List[EiLPerson] on search results page
   * If list is 0 size will return employee not found page
   */
  def searchResultsHandleValidResult(
    listOfMatches: List[EiLPerson],
    isCurrentTaxYear: String,
    formType: String,
    iabdTypeValue: String,
    currentExclusions: List[EiLPerson])(implicit request: AuthenticatedRequest[_]): Result = {
    val uniqueListOfMatches: List[EiLPerson] =
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
              empRef = Some(request.empRef),
              iabdType = iabdTypeValue
            ))
        } else {
          logger.warn("[ExclusionListController][searchResultsHandleValidResult] Cached list of matches is empty")
          val errorCode = 63082
          NotFound(
            errorPageView(
              errorMessage = message,
              taxYearRange = controllersReferenceData.yearRange,
              isCurrentTaxYear = isCurrentTaxYear,
              code = errorCode,
              empRef = Some(request.empRef),
              iabdType = iabdTypeValue
            ))
        }
      case _ =>
        logger.info(
          s"[ExclusionListController][searchResultsHandleValidResult] Exclusion search matched ${listOfMatches.size}" +
            s" employees with Optimistic locks ${uniqueListOfMatches.map(_.perOptLock)}")
        Ok(
          searchResultsView(
            controllersReferenceData.yearRange,
            isCurrentTaxYear,
            iabdTypeValue,
            EiLPersonList(uniqueListOfMatches),
            formMappings.individualSelectionForm,
            formType,
            empRef = request.empRef
          ))
    }
  }

  /*
   * Handles form errors on search results page
   * Will display relevant form page
   */
  def searchResultsHandleFormErrors(
    isCurrentTaxYear: String,
    formType: String,
    iabdTypeValue: String,
    formWithErrors: Form[EiLPerson])(implicit request: AuthenticatedRequest[_]): Future[Result] =
    Future {
      formType match {
        case ControllersReferenceDataCodes.FORM_TYPE_NINO =>
          Ok(
            ninoExclusionSearchFormView(
              controllersReferenceData.yearRange,
              isCurrentTaxYear,
              iabdTypeValue,
              formWithErrors,
              empRef = request.empRef))
        case ControllersReferenceDataCodes.FORM_TYPE_NONINO =>
          Ok(
            noNinoExclusionSearchFormView(
              controllersReferenceData.yearRange,
              isCurrentTaxYear,
              iabdTypeValue,
              formWithErrors,
              empRef = request.empRef))
      }
    }

  def createExcludedPerson(individualsDetails: EiLPerson): Option[EiLPerson] =
    Some(
      EiLPerson(
        individualsDetails.nino,
        individualsDetails.firstForename,
        individualsDetails.secondForename,
        individualsDetails.surname,
        individualsDetails.worksPayrollNumber,
        individualsDetails.dateOfBirth,
        individualsDetails.gender,
        Some(ControllersReferenceDataCodes.EXCLUSION_ADD_STATUS),
        individualsDetails.perOptLock
      ))

  def updateMultipleExclusions(year: String, iabdType: String, formType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      if (exclusionsAllowed) {
        val resultFuture = cachingService.fetchPbikSession().flatMap { session =>
          formMappings.individualSelectionForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(BadRequest(searchResultsView(
                  controllersReferenceData.yearRange,
                  year,
                  uriInformation.iabdValueURLDeMapper(iabdType),
                  EiLPersonList(session.get.listOfMatches.get),
                  formWithErrors,
                  formType,
                  request.empRef
                ))),
              values => {
                val individualsDetails = session.get.listOfMatches.get.find(person => person.nino == values.nino).get
                val excludedPerson = createExcludedPerson(individualsDetails)
                validateRequest(year, iabdType).flatMap { _ =>
                  commitExclusion(
                    year,
                    uriInformation.iabdValueURLDeMapper(iabdType),
                    controllersReferenceData.yearRange,
                    excludedPerson)
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
              taxDateUtils.getTaxYearRange(),
              empRef = Some(request.empRef))))
      }
    }

  private def getExcludedPerson(pbikSession: Option[PbikSession]): Option[EiLPerson] =
    pbikSession.flatMap { session =>
      (session.currentExclusions, session.listOfMatches) match {
        case (Some(currentExclusions), Some(listOfMatches)) =>
          val individualDetails =
            eiLListService.searchResultsRemoveAlreadyExcluded(currentExclusions, listOfMatches).head
          createExcludedPerson(individualDetails)
        case _ => None
      }
    }

  def updateExclusions(year: String, iabdType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      if (exclusionsAllowed) {
        val resultFuture = cachingService.fetchPbikSession().flatMap { session: Option[PbikSession] =>
          getExcludedPerson(session).fold {
            logger.error(
              "[ExclusionListController][updateExclusions] can not get excludedPerson, session data not filled"
            )
            Future.successful(
              InternalServerError(
                errorPageView(
                  ControllersReferenceDataCodes.DEFAULT_ERROR,
                  taxDateUtils.getTaxYearRange(),
                  empRef = Some(request.empRef)
                ))
            )
          } { excludedPerson =>
            validateRequest(year, iabdType).flatMap { _ =>
              commitExclusion(
                year,
                uriInformation.iabdValueURLDeMapper(iabdType),
                controllersReferenceData.yearRange,
                Some(excludedPerson))
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
              taxDateUtils.getTaxYearRange(),
              empRef = Some(request.empRef)
            )
          )
        )
      }
    }

  def showExclusionConfirmation(year: String, iabdType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val resultFuture = for {
        _       <- validateRequest(year, iabdType)
        session <- cachingService.fetchPbikSession()
      } yield {
        Ok(
          whatNextExclusionView(
            taxDateUtils.getTaxYearRange(),
            year,
            uriInformation.iabdValueURLDeMapper(iabdType),
            session.get.listOfMatches.get.head.firstForename + " " + session.get.listOfMatches.get.head.surname,
            request.empRef
          ))
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def commitExclusion(
    year: String,
    iabdType: String,
    taxYearRange: TaxYearRange,
    excludedIndividual: Option[EiLPerson])(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val yearInt = if (year.equals(utils.FormMappingsConstants.CY)) taxYearRange.cyminus1 else taxYearRange.cy
    logger.info(
      s"[ExclusionListController][commitExclusion] Committing Exclusion for scheme ${request.empRef.toString}" +
        s", with employees Optimistic Lock: ${excludedIndividual.map(eiLPerson => eiLPerson.perOptLock).getOrElse(0)}"
    )
    tierConnector
      .genericPostCall(
        uriInformation.baseUrl,
        uriInformation.exclusionPostUpdatePath(iabdType),
        request.empRef,
        yearInt,
        excludedIndividual.get)
      .map { response =>
        response.status match {
          case OK =>
            auditExclusion(exclusion = true, yearInt, excludedIndividual.get.nino, iabdType)
            Redirect(
              routes.ExclusionListController
                .showExclusionConfirmation(year, uriInformation.iabdValueURLMapper(iabdType)))
          case unexpectedStatus =>
            logger.warn(
              s"[ExclusionListController][commitExclusion] Exclusion list update operation was unable to be executed " +
                s"successfully: received $unexpectedStatus response")
            InternalServerError(
              errorPageView(
                "Could not perform update operation",
                controllersReferenceData.yearRange,
                isCurrentTaxYear = "",
                empRef = Some(request.empRef)))
              .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
        }
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

  def remove(year: String, iabdType: String, nino: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      if (exclusionsAllowed) {
        val resultFuture = cachingService.fetchPbikSession().flatMap { session =>
          val selectedPerson: EiLPerson = session.get.currentExclusions.get.filter(person => person.nino == nino).head
          cachingService.cacheEiLPerson(selectedPerson).map { _ =>
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
              taxDateUtils.getTaxYearRange(),
              empRef = Some(request.empRef))))
      }
    }

  def showRemovalConfirmation(year: String, iabdType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val futureResult = cachingService.fetchPbikSession().map { session =>
        Ok(
          removalConfirmationView(
            controllersReferenceData.yearRange,
            year,
            iabdType,
            EiLPersonList(List(session.get.eiLPerson.get)),
            empRef = request.empRef
          ))
      }
      controllersReferenceData.responseErrorHandler(futureResult)
    }

  def removeExclusionsCommit(iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      val iabdTypeValue = uriInformation.iabdValueURLDeMapper(iabdType)
      val taxYearRange = taxDateUtils.getTaxYearRange()
      if (exclusionsAllowed) {
        val resultFuture = cachingService.fetchPbikSession().flatMap { session =>
          val individual = session.get.eiLPerson.get
          val year = taxYearRange.cy
          val removalsList = List(individual)
          tierConnector
            .genericPostCall(
              uriInformation.baseUrl,
              uriInformation.exclusionPostRemovePath(iabdTypeValue),
              request.empRef,
              year,
              individual)
            .map { response =>
              response.status match {
                case OK =>
                  auditExclusion(exclusion = false, year, splunkLogger.extractListNino(removalsList), iabdType)
                  Redirect(routes.ExclusionListController.showRemovalWhatsNext(iabdType))
                case unexpectedStatus =>
                  logger.warn(
                    s"[ExclusionListController][processRemovalCommit] Exclusion list update operation was unable to be executed successfully:" +
                      s" received $unexpectedStatus response")
                  Ok(
                    errorPageView(
                      "Could not perform update operation",
                      controllersReferenceData.yearRange,
                      "",
                      empRef = Some(request.empRef)))
                    .withSession(request.session + (SessionKeys.sessionId -> s"session-${UUID.randomUUID}"))
              }
            }
        }
        controllersReferenceData.responseErrorHandler(resultFuture)
      } else {
        logger.info("[ExclusionListController][removeExclusionsCommit] Exclusions not allowed, showing error page")
        Future.successful(
          Forbidden(
            errorPageView(ControllersReferenceDataCodes.FEATURE_RESTRICTED, taxYearRange, empRef = Some(request.empRef))
          ))
      }
  }

  def showRemovalWhatsNext(iabdType: String): Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val futureResult = cachingService.fetchPbikSession().map { session =>
        val individual = session.get.eiLPerson.get
        Ok(
          whatNextRescindView(
            taxDateUtils.getTaxYearRange(),
            ControllersReferenceDataCodes.NEXT_TAX_YEAR,
            iabdType,
            individual.firstForename + " " + individual.surname,
            request.empRef
          ))
      }
      controllersReferenceData.responseErrorHandler(futureResult)
  }

  private def auditExclusion(exclusion: Boolean, year: Int, employee: String, iabdType: String)(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[AnyContent]) =
    splunkLogger.logSplunkEvent(
      splunkLogger.createDataEvent(
        tier = splunkLogger.FRONTEND,
        action = if (exclusion) splunkLogger.ADD else splunkLogger.REMOVE,
        target = splunkLogger.EIL,
        period = splunkLogger.taxYearToSpPeriod(year),
        msg = if (exclusion) "Employee excluded" else "Employee exclusion rescinded",
        nino = Some(employee),
        iabd = Some(iabdType),
        name = Some(request.name),
        empRef = Some(request.empRef)
      ))

}
