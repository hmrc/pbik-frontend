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

import config.PbikAppConfig
import models.auth.AuthenticatedRequest
import models.v1.IabdType.IabdType
import models.v1._
import models.v1.exclusion.{PbikExclusionPersonWithBenefitRequest, PbikExclusions, UpdateExclusionPersonForABenefitRequest}
import models.v1.trace.{TracePeopleByNinoRequest, TracePeopleByPersonalDetailsRequest, TracePersonListResponse}
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, OK, UNPROCESSABLE_ENTITY}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.Exceptions.GenericServerErrorException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PbikConnector @Inject() (client: HttpClientV2, config: PbikAppConfig)(implicit ec: ExecutionContext)
    extends Logging {

  def getRegisteredBiks(
    empRef: EmpRef,
    year: Int
  )(implicit hc: HeaderCarrier): Future[BenefitListResponse] =
    client
      .get(url"${config.getRegisteredBiksURL(empRef, year)}")
      .execute[HttpResponse]
      .flatMap { implicit response =>
        response.status match {
          case OK =>
            response.json.validate[BenefitListResponse] match {
              case JsSuccess(value, _) => Future.successful(value)
              case JsError(errors)     =>
                logger.error(s"[PbikConnector][getRegisteredBiks] Failed to parse BenefitListResponse: $errors")
                Future.failed(
                  new GenericServerErrorException(
                    s"Failed to get registered benefits for $year, status: ${response.status}"
                  )
                )
            }
          case _  =>
            Future.failed(
              new GenericServerErrorException(
                s"Failed to get registered benefits for $year, status: ${response.status}"
              )
            )
        }
      }

  def getAllAvailableBiks(year: Int)(implicit hc: HeaderCarrier): Future[Either[NPSErrors, BenefitTypes]] =
    client
      .get(url"${config.getBenefitTypesURL(year)}")
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

  def getAllExcludedEiLPersonForBik(iabdType: IabdType, empRef: EmpRef, year: Int)(implicit
    hc: HeaderCarrier
  ): Future[Either[NPSErrors, PbikExclusions]] =
    client
      .get(url"${config.getAllExclusionsURL(iabdType, empRef, year)}")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK                   =>
            response.json.validate[PbikExclusions] match {
              case JsSuccess(value, _) => Future.successful(Right(value))
              case JsError(errors)     =>
                logger.error(s"[PbikConnector][getAllExcludedEiLPersonForBik] Failed to parse PbikExclusion: $errors")
                Future.failed(
                  new GenericServerErrorException("Failed to get excluded persons, status: " + response.status)
                )
            }
          case UNPROCESSABLE_ENTITY =>
            response.json.validate[NPSErrors] match {
              case JsSuccess(value, _) => Future.successful(Left(value))
              case JsError(errors)     =>
                logger.error(s"[PbikConnector][getAllExcludedEiLPersonForBik] Failed to parse NPSErrors: $errors")
                Future.failed(
                  new GenericServerErrorException(s"Failed to get excluded persons, status: ${response.status}")
                )
            }
          case _                    =>
            logger.error(
              s"[PbikConnector][getAllExcludedEiLPersonForBik] a pbik error code was returned. Error: ${response.body}"
            )
            Future.failed(
              new GenericServerErrorException(s"Failed to get available EilPerson, status: ${response.status}")
            )
        }
      }

  def excludeEiLPersonFromBik(empRef: EmpRef, year: Int, body: UpdateExclusionPersonForABenefitRequest)(implicit
    hc: HeaderCarrier
  ): Future[Either[NPSErrors, Int]] =
    client
      .post(url"${config.getExcludedPersonsURL(empRef, year)}")
      .withBody(Json.toJson(body))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK                   =>
            Future.successful(Right(response.status))
          case UNPROCESSABLE_ENTITY =>
            response.json.validate[NPSErrors] match {
              case JsSuccess(value, _) => Future.successful(Left(value))
              case JsError(errors)     =>
                logger.error(s"[PbikConnector][excludeEiLPersonFromBik] Failed to parse NPSErrors: $errors")
                Future.failed(
                  new GenericServerErrorException("Failed to exclude person, status: " + response.status)
                )
            }
          case _                    =>
            logger.error(
              s"[PbikConnector][excludeEiLPersonFromBik] a pbik error code was returned. Error: ${response.body}"
            )
            Future.failed(
              new GenericServerErrorException("Failed to exclude person, status: " + response.status)
            )
        }
      }

  def removeEiLPersonExclusionFromBik(
    iabdType: IabdType,
    empRef: EmpRef,
    year: Int,
    individualToRemove: PbikExclusionPersonWithBenefitRequest
  )(implicit hc: HeaderCarrier): Future[Either[NPSErrors, Int]] =
    client
      .delete(url"${config.getRemoveExclusionURL(empRef, year, iabdType.convertToUrlParam)}")
      .withBody(Json.toJson(individualToRemove))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK                   =>
            Future.successful(Right(response.status))
          case UNPROCESSABLE_ENTITY =>
            response.json.validate[NPSErrors] match {
              case JsSuccess(value, _) => Future.successful(Left(value))
              case JsError(errors)     =>
                logger.error(s"[PbikConnector][removeEiLPersonExclusionFromBik] Failed to parse NPSErrors: $errors")
                Future.failed(
                  new GenericServerErrorException(
                    "Failed to remove excluded person from benefit, status: " + response.status
                  )
                )
            }
          case _                    =>
            logger.error(
              s"[PbikConnector][removeEiLPersonExclusionFromBik] a pbik error code was returned. Error: ${response.body}"
            )
            Future.failed(
              new GenericServerErrorException(
                "Failed to remove excluded person from benefit, status: " + response.status
              )
            )
        }
      }

  def updateOrganisationsRegisteredBiks(year: Int, payload: BenefitListUpdateRequest)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Int] = {
    val suffix        = if (request.isAgent) "agent" else "org"
    val payloadAsJson = Json.toJson(payload)

    logger.info(
      s"[PbikConnector][updateOrganisationsRegisteredBiks] Updated BIKs ${payload.pbikRegistrationUpdate.size} with lock value: ${payload.employerOptimisticLockRequest}"
    )

    client
      .post(url"${config.getUpdateBenefitURL(year, suffix, request.empRef)}")
      .withBody(Json.toJson(payload))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(response.status)
          case _  =>
            logger.error(
              s"[PbikConnector][updateOrganisationsRegisteredBiks] Failed to update benefit list, status: ${response.status}, request body: ${payloadAsJson
                  .toString()}, response body: ${response.body}"
            )
            Future.failed(
              new GenericServerErrorException(
                s"Failed to update benefit list, status: ${response.status}"
              )
            )
        }
      }
  }

  private def findPerson(empRef: EmpRef, year: Int, body: JsValue)(implicit
    hc: HeaderCarrier
  ): Future[Either[NPSErrors, TracePersonListResponse]] = {
    logger.info(s"[PbikConnector][findPerson] Finding person for empRef: $empRef and year: $year")
    client
      .post(url"${config.postTraceByPersonalDetailsURL(year, empRef)}")
      .withBody(body)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK                   =>
            response.json.validate[TracePersonListResponse] match {
              case JsSuccess(value, _) => Future.successful(Right(value))
              case JsError(errors)     =>
                logger.error(
                  s"[PbikConnector][findPerson] Failed to parse TracePersonListResponse: $errors"
                )
                Future.failed(
                  new GenericServerErrorException(
                    "Failed to find person by personal OR nino details, status: " + response.status
                  )
                )
            }
          case UNPROCESSABLE_ENTITY =>
            logger.error(
              s"[PbikConnector][findPerson] Pbik error code was returned. status: ${response.status} error: ${response.body}"
            )
            response.json.validate[NPSErrors] match {
              case JsSuccess(value, _) => Future.successful(Left(value))
              case JsError(errors)     =>
                logger.error(s"[PbikConnector][findPerson] Failed to parse NPSErrors: $errors")
                Future.failed(
                  new GenericServerErrorException(
                    "Failed to find person by personal details OR nino details, status: " + response.status
                  )
                )
            }
          case _                    =>
            Future.failed(
              new GenericServerErrorException(
                "Failed to find person by personal details OR nino details, status: " + response.status
              )
            )
        }
      }
  }

  def findPersonByPersonalDetails(empRef: EmpRef, year: Int, body: TracePeopleByPersonalDetailsRequest)(implicit
    hc: HeaderCarrier
  ): Future[Either[NPSErrors, TracePersonListResponse]] =
    findPerson(empRef, year, Json.toJson(body))

  def findPersonByNino(empRef: EmpRef, year: Int, body: TracePeopleByNinoRequest)(implicit
    hc: HeaderCarrier
  ): Future[Either[NPSErrors, TracePersonListResponse]] =
    findPerson(empRef, year, Json.toJson(body))

}
