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

package connectors

import config.Service
import models._
import models.v1._
import models.v1.exclusion.{ExclusionResponse, PbikExclusionPersonWithBenefitRequest, PbikExclusions, UpdateExclusionPersonForABenefitRequest, UpdateExclusionPersonForABenefitResponse}
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.Request
import play.api.{Configuration, Logging}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.Exceptions.GenericServerErrorException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PbikConnector @Inject() (client: HttpClientV2, configuration: Configuration)(implicit ec: ExecutionContext)
    extends Logging {

  private val maxEmptyBodyLength: Int                                            = 4
  private val baseUrl: String                                                    =
    s"${configuration.get[Service]("microservice.services.pbik")}/epaye"
  private def getRegisteredBiksURL(empRef: EmpRef, year: Int)                    =
    s"$baseUrl/${empRef.taxOfficeNumber}/${empRef.taxOfficeReference}/$year"
  private def getBenefitTypesURL(year: Int)                                      =
    s"$baseUrl/$year/getbenefittypes"
  private def getAllExclusionsURL(iabdString: String, empRef: EmpRef, year: Int) =
    s"$baseUrl/${empRef.taxOfficeNumber}/${empRef.taxOfficeReference}/$year/$iabdString/exclusion"
  private def getExcludedPersonsURL(empRef: EmpRef, year: Int)                   =
    s"$baseUrl/${empRef.taxOfficeNumber}/${empRef.taxOfficeReference}/$year/exclusion/update"
  private def getRemoveExclusionURL(empRef: EmpRef, year: Int)                   =
    s"$baseUrl/${empRef.taxOfficeNumber}/${empRef.taxOfficeReference}/$year/exclusion/remove"
  private def getUpdateBenefitURL(year: Int, suffix: String, empRef: EmpRef)     =
    s"$baseUrl/${empRef.taxOfficeNumber}/${empRef.taxOfficeReference}/$year/updatebenefittypes/$suffix"

  def getRegisteredBiks(
    empRef: EmpRef,
    year: Int
  )(implicit hc: HeaderCarrier): Future[BikResponse] =
    client
      .get(url"${getRegisteredBiksURL(empRef, year)}")
      .execute[HttpResponse]
      .map { implicit response =>
        //TODO refactor the logic and remove this generic body parsing do the same with Either[Error, T]
        val resp                                  = validateResponses("getRegisteredBiks").json.as[BenefitListResponse]
        val updatedHeaders: Seq[(String, String)] =
          HeaderTags.createResponseHeaders(resp.currentOptimisticLock.toString).toSeq
        val biks                                  = resp.getBenefitInKindWithCount.map(benefit => Bik(benefit))

        logger.info(
          s"[PbikConnector][getRegisteredBiks] Got registered BIKs ${biks.size} with headers: $updatedHeaders"
        )
        BikResponse(updatedHeaders.toMap, biks.toSet)
      }

  def getAllAvailableBiks(year: Int)(implicit hc: HeaderCarrier): Future[Either[NPSErrors, BenefitTypes]] =
    client
      .get(url"${getBenefitTypesURL(year)}")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK          =>
            response.json.validate[BenefitTypes] match {
              case JsSuccess(value, _) => Future.successful(Right(value))
              case JsError(errors)     =>
                logger.error(s"[PbikConnector][getAllAvailableBiks] Failed to parse BenefitTypes: $errors")
                Future.failed[Either[NPSErrors, BenefitTypes]](
                  new GenericServerErrorException("Failed to get available BIKs, status: " + response.status)
                )

            }
          case BAD_REQUEST =>
            logger.error(
              s"[PbikConnector][getAllAvailableBiks] a pbik error code was returned. Error: ${response.body}"
            )
            response.json.validate[NPSErrors] match {
              case JsSuccess(value, _) => Future.successful(Left(value))
              case JsError(errors)     =>
                logger.error(s"[PbikConnector][getAllAvailableBiks] Failed to parse NPSErrors: $errors")
                Future.failed[Either[NPSErrors, BenefitTypes]](
                  new GenericServerErrorException("Failed to get available BIKs, status: " + response.status)
                )
            }
          case _           =>
            Future.failed(new GenericServerErrorException("Failed to get available BIKs, status: " + response.status))
        }
      }

  def getAllExcludedEiLPersonForBik(iabdString: String, empRef: EmpRef, year: Int)(implicit
    hc: HeaderCarrier
  ): Future[Either[NPSErrors, PbikExclusions]] =
    client
      .get(url"${getAllExclusionsURL(iabdString, empRef, year)}")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK          =>
            response.json.validate[PbikExclusions] match {
              case JsSuccess(value, _) => Future.successful(Right(value))
              case JsError(errors)     =>
                logger.error(s"[PbikConnector][getAllExcludedEiLPersonForBik] Failed to parse PbikExclusion: $errors")
                Future.failed(
                  new GenericServerErrorException("Failed to get excluded persons, status: " + response.status)
                )
            }
          case BAD_REQUEST =>
            logger.error(
              s"[PbikConnector][getAllExcludedEiLPersonForBik] a pbik error code was returned. Error: ${response.body}"
            )
            response.json.validate[NPSErrors] match {
              case JsSuccess(value, _) => Future.successful(Left(value))
              case JsError(errors)     =>
                logger.error(s"[PbikConnector][getAllExcludedEiLPersonForBik] Failed to parse NPSErrors: $errors")
                Future.failed(
                  new GenericServerErrorException("Failed to get excluded persons, status: " + response.status)
                )
            }
          case _           =>
            Future.failed(
              new GenericServerErrorException("Failed to get available EilPerson, status: " + response.status)
            )
        }
      }

  def excludeEiLPersonFromBik(empRef: EmpRef, year: Int, body: UpdateExclusionPersonForABenefitRequest)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[ExclusionResponse] =
    client
      .post(url"${getExcludedPersonsURL(empRef, year)}")
      .setHeader(createOrCheckForRequiredHeaders.toSeq: _*)
      .withBody(Json.toJson(body))
      .execute[HttpResponse]
      .map { response =>
        ExclusionResponse(
          response.status,
          validateResponses("excludeEiLPersonFromBik")(response).json.as[UpdateExclusionPersonForABenefitResponse]
        )
      }

  def removeEiLPersonExclusionFromBik(
    iabdString: String,
    empRef: EmpRef,
    year: Int,
    individualToRemove: PbikExclusionPersonWithBenefitRequest
  )(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[Int] =
    //TODO build correct body
    client
      .delete(url"${getRemoveExclusionURL(empRef, year)}")
      .setHeader(createOrCheckForRequiredHeaders.toSeq: _*)
      .withBody(Json.toJson(individualToRemove))
      .execute[HttpResponse]
      .map { implicit response =>
        validateResponses("removeEiLPersonExclusionFromBik").status
      }

  def updateOrganisationsRegisteredBiks(year: Int, changes: List[Bik])(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Int] = {
    val etagFromSession              = request.session.get(HeaderTags.ETAG).getOrElse("0").toInt
    val updatedBiks                  = changes.map(bik => BenefitInKindRequest(bik, request.isAgent))
    val payload                      = BenefitListUpdateRequest(updatedBiks, EmployerOptimisticLockRequest(etagFromSession))
    val suffix                       = if (request.isAgent) "agent" else "org"
    val headers: Map[String, String] = createOrCheckForRequiredHeaders
    client
      .post(url"${getUpdateBenefitURL(year, suffix, request.empRef)}")
      .setHeader(headers.toSeq: _*)
      .withBody(Json.toJson(payload))
      .execute[HttpResponse]
      .map { response =>
        val validatedResponse         = validateResponses("updateOrganisationsRegisteredBiks")(response)
        val benefitListUpdateResponse = validatedResponse.json.as[BenefitListUpdateResponse]
        val lockValue                 = benefitListUpdateResponse.employerOptimisticLockResponse.updatedOptimisticLock

        logger.info(
          s"[PbikConnector][updateOrganisationsRegisteredBiks] Updated BIKs ${updatedBiks.size} with lock value: $lockValue"
        )
        lockValue
      }
  }

  private def createOrCheckForRequiredHeaders(implicit request: Request[_]): Map[String, String] = {
    val etagFromSession = request.session.get(HeaderTags.ETAG).getOrElse(HeaderTags.ETAG_DEFAULT_VALUE)
    logger.info("[PbikConnector][createOrCheckForRequiredHeaders] POST etagFromSession: " + etagFromSession)
    HeaderTags.createResponseHeaders(etagFromSession)
  }

  private def parseOldErrorBody(response: HttpResponse): HttpResponse =
    response.json.validate[PbikError] match {
      case JsSuccess(value, _) =>
        logger.error(s"[PbikConnector][parseOldErrorBody] a pbik error code was returned. Error: ${value.errorCode}")
        throw new GenericServerErrorException(value.errorCode)
      case JsError(_)          =>
        logger.error(
          s"[PbikConnector][parseOldErrorBody] an error was returned but it was not a PbikError. Error: ${response.body}"
        )
        throw new GenericServerErrorException(response.body)
    }

  private def parseNewErrorBody(response: HttpResponse): HttpResponse =
    response.json.validate[NPSErrors] match {
      case JsSuccess(value, _) =>
        // new error code has the format "400.12345" where 400 is the status code and 12345 is the error code
        val prefix    = s"${response.status}."
        val errorCode = value.failures.head.code.replace(prefix, "")
        logger.error(s"[PbikConnector][parseNewErrorBody] a pbik error code was returned. Error: $errorCode")
        throw new GenericServerErrorException(errorCode)
      case JsError(_)          =>
        logger.error(
          s"[PbikConnector][parseNewErrorBody] an error was returned but it was not NPSErrors. Error: ${response.body}"
        )
        throw new GenericServerErrorException(response.body)
    }

  private def validateResponses(fromMethod: String)(implicit response: HttpResponse): HttpResponse = {
    val isOldError = response.body.contains("errorCode")
    val isNewError = response.body.contains("reason") && response.body.contains("code")

    if (isOldError) {
      logger.error(s"[PbikConnector][$fromMethod] a pbik error code was returned. Error: ${response.body}")
      parseOldErrorBody(response)
    } else if (isNewError) {
      logger.error(s"[PbikConnector][$fromMethod] a pbik error code was returned. Error: ${response.body}")
      parseNewErrorBody(response)
    } else if (response.status >= BAD_REQUEST) {
      logger.error(s"[PbikConnector][$fromMethod] an error code was returned. Error: ${response.body}")
      throw new GenericServerErrorException(response.body)
    } else {
      response
    }
  }
}
