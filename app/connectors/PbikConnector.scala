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
import models.v1.{BenefitInKindRequest, BenefitListResponse, BenefitListUpdateResponse, NPSErrors}
import play.api.http.Status.BAD_REQUEST
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

  private val maxEmptyBodyLength: Int                                              = 4
  private val baseUrl: String                                                      = s"${configuration.get[Service]("microservice.services.pbik")}/epaye"
  private def getRegisteredBiksURL(empRef: EmpRef, year: Int)                      = s"$baseUrl/${empRef.encodedEmpRef}/$year"
  private def getBenefitTypesURL(year: Int)                                        = s"$baseUrl/$year/getbenefittypes"
  private def getAllExclusionsURL(iabdString: String, empRef: EmpRef, year: Int)   =
    s"$baseUrl/${empRef.encodedEmpRef}/$year/${Bik.asNPSTypeValue(iabdString)}/exclusion"
  private def getExcludedPersonsURL(iabdString: String, empRef: EmpRef, year: Int) =
    s"$baseUrl/${empRef.encodedEmpRef}/$year/${Bik.asNPSTypeValue(iabdString)}/exclusion/update"
  private def getRemoveExclusionURL(iabdString: String, empRef: EmpRef, year: Int) =
    s"$baseUrl/${empRef.encodedEmpRef}/$year/${Bik.asNPSTypeValue(iabdString)}/exclusion/remove"
  private def getUpdateBenefitURL(year: Int, suffix: String, empRef: EmpRef)       =
    s"$baseUrl/${empRef.encodedEmpRef}/$year/updatebenefittypes/$suffix"

  def getRegisteredBiks(
    empRef: EmpRef,
    year: Int
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[BikResponse] = {
    val etagFromSession      = request.session.get(HeaderTags.ETAG).getOrElse("UNKNOWN_ETAG")
    val xtxidFromSession     = request.session.get(HeaderTags.X_TXID).getOrElse("UNKNOWN_XTXID")
    val requestSessionHeader = HeaderTags.createResponseHeaders(etagFromSession, xtxidFromSession)

    logger.info(s"[PbikConnector][getRegisteredBiks] GET benefits with headers: $requestSessionHeader")

    client
      .get(url"${getRegisteredBiksURL(empRef, year)}")
      .execute[HttpResponse]
      .map { implicit response =>
        val resp                                  = validateResponses("getRegisteredBiks").json.as[BenefitListResponse]
        val updatedHeaders: Seq[(String, String)] =
          HeaderTags.createResponseHeaders(resp.employerOptimisticLockResponse.currentOptimisticLock.toString).toSeq
        val biks                                  = resp.pbikRegistrationDetails.getOrElse(List.empty).map(benefit => Bik(benefit))

        logger.info(
          s"[PbikConnector][getRegisteredBiks] Got registered BIKs ${biks.size} with headers: $updatedHeaders"
        )
        BikResponse(updatedHeaders.toMap, biks)
      }
  }

  def getAllAvailableBiks(year: Int)(implicit hc: HeaderCarrier): Future[List[Bik]] =
    client
      .get(url"${getBenefitTypesURL(year)}")
      .execute[HttpResponse]
      .map(implicit response => validateResponses("getAllAvailableBiks").json.as[List[Bik]])

  def getAllExcludedEiLPersonForBik(iabdString: String, empRef: EmpRef, year: Int)(implicit
    hc: HeaderCarrier
  ): Future[List[EiLPerson]] =
    client
      .get(url"${getAllExclusionsURL(iabdString, empRef, year)}")
      .execute[HttpResponse]
      .map(implicit response => validateResponses("getAllExcludedEiLPersonForBik").json.as[List[EiLPerson]])

  def excludeEiLPersonFromBik(iabdString: String, empRef: EmpRef, year: Int, individual: EiLPerson)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[EiLResponse] =
    client
      .post(url"${getExcludedPersonsURL(iabdString, empRef, year)}")
      .setHeader(createOrCheckForRequiredHeaders.toSeq: _*)
      .withBody(Json.toJson(individual))
      .execute[HttpResponse]
      .map {
        case response if response.body.length <= maxEmptyBodyLength =>
          EiLResponse(validateResponses("excludeEiLPersonFromBik")(response).status, List.empty)
        case response                                               =>
          EiLResponse(response.status, validateResponses("excludeEiLPersonFromBik")(response).json.as[List[EiLPerson]])
      }

  def removeEiLPersonExclusionFromBik(iabdString: String, empRef: EmpRef, year: Int, individualToRemove: EiLPerson)(
    implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[Int] =
    client
      .post(url"${getRemoveExclusionURL(iabdString, empRef, year)}")
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
    val updatedBiks                  = changes.map(bik => BenefitInKindRequest(bik, request.isAgent))
    val suffix                       = if (request.isAgent) "agent" else "org"
    val headers: Map[String, String] = createOrCheckForRequiredHeaders
    client
      .post(url"${getUpdateBenefitURL(year, suffix, request.empRef)}")
      .setHeader(headers.toSeq: _*)
      .withBody(Json.toJson(updatedBiks))
      .execute[HttpResponse]
      .map { response =>
        val validatedResponse         = validateResponses("updateOrganisationsRegisteredBiks")(response)
        val benefitListUpdateResponse = validatedResponse.json.as[BenefitListUpdateResponse]
        val lockValue                 = benefitListUpdateResponse.employerOptimisticLockResponse.currentOptimisticLock

        logger.info(
          s"[PbikConnector][updateOrganisationsRegisteredBiks] Updated BIKs ${updatedBiks.size} with lock value: $lockValue"
        )
        lockValue
      }
  }

  private def createOrCheckForRequiredHeaders(implicit request: Request[_]): Map[String, String] = {
    val etagFromSession  = request.session.get(HeaderTags.ETAG).getOrElse(HeaderTags.ETAG_DEFAULT_VALUE)
    val xtxidFromSession = request.session.get(HeaderTags.X_TXID).getOrElse(HeaderTags.X_TXID_DEFAULT_VALUE)
    logger.info(
      "[PbikConnector][createOrCheckForRequiredHeaders] POST etagFromSession: " + etagFromSession + ", xtxidFromSession: " + xtxidFromSession
    )
    HeaderTags.createResponseHeaders(etagFromSession, xtxidFromSession)
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
