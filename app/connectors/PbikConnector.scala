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
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc.Request
import play.api.{Configuration, Logging}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.Exceptions.GenericServerErrorException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PbikConnector @Inject() (client: HttpClient, configuration: Configuration)(implicit ec: ExecutionContext)
    extends Logging {

  private val maxEmptyBodyLength: Int = 4
  private val baseUrl: String         = s"${configuration.get[Service]("microservice.services.pbik")}/epaye"

  def getRegisteredBiks(
    empRef: EmpRef,
    year: Int
  )(implicit hc: HeaderCarrier): Future[BikResponse] =
    client
      .GET(s"$baseUrl/${empRef.encodedEmpRef}/$year")
      .map { implicit response =>
        val headers = responseHeaders
        val resp    = validateResponses("getRegisteredBiks").json.as[BenefitListResponse]
        val biks    = resp.pbikRegistrationDetails.map(benefit => Bik(benefit))
        BikResponse(headers, biks)
      }

  def getAllAvailableBiks(year: Int)(implicit hc: HeaderCarrier): Future[List[Bik]] =
    client
      .GET(s"$baseUrl/$year/getbenefittypes")
      .map(implicit response => validateResponses("getAllAvailableBiks").json.as[List[Bik]])

  def getAllExcludedEiLPersonForBik(iabdString: String, empRef: EmpRef, year: Int)(implicit
    hc: HeaderCarrier
  ): Future[List[EiLPerson]] =
    client
      .GET(s"$baseUrl/${empRef.encodedEmpRef}/$year/${Bik.asNPSTypeValue(iabdString)}/exclusion")
      .map(implicit response => validateResponses("getAllExcludedEiLPersonForBik").json.as[List[EiLPerson]])

  def excludeEiLPersonFromBik(iabdString: String, empRef: EmpRef, year: Int, individual: EiLPerson)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[EiLResponse] =
    client
      .POST(
        s"$baseUrl/${empRef.encodedEmpRef}/$year/${Bik.asNPSTypeValue(iabdString)}/exclusion/update",
        individual,
        createOrCheckForRequiredHeaders.toSeq
      )
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
      .POST(
        s"$baseUrl/${empRef.encodedEmpRef}/$year/${Bik.asNPSTypeValue(iabdString)}/exclusion/remove",
        individualToRemove,
        createOrCheckForRequiredHeaders.toSeq
      )
      .map { implicit response =>
        validateResponses("removeEiLPersonExclusionFromBik").status
      }

  def updateOrganisationsRegisteredBiks(year: Int, changes: List[Bik])(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Int] = {
    val updatedBiks                  = changes.map(bik => BenefitInKindRequest(bik, request.isAgent))
    val headers: Map[String, String] = createOrCheckForRequiredHeaders
    client
      .POST(
        s"$baseUrl/${request.empRef.encodedEmpRef}/$year/updatebenefittypes",
        updatedBiks,
        headers.toSeq
      )
      .map { response =>
        val validatedResponse                     = validateResponses("updateOrganisationsRegisteredBiks")(response)
        val benefitListUpdateResponse             = validatedResponse.json.as[BenefitListUpdateResponse]
        val lockValue                             = benefitListUpdateResponse.employerOptimisticLockResponse.currentOptimisticLock
        val updatedHeaders: Seq[(String, String)] = HeaderTags.createResponseHeaders(lockValue.toString).toSeq

        hc.withExtraHeaders(updatedHeaders: _*)
        lockValue
      }
  }

  private def responseHeaders(implicit response: HttpResponse): Map[String, String] =
    Map(
      HeaderTags.ETAG   -> response.header(HeaderTags.ETAG).getOrElse(HeaderTags.ETAG_DEFAULT_VALUE),
      HeaderTags.X_TXID -> response.header(HeaderTags.X_TXID).getOrElse(HeaderTags.X_TXID_DEFAULT_VALUE)
    )

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
